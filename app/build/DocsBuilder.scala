package build

import java.io.File

import scala.util.Try
import play.api.Logger

import dao._
import settings.Global

import pamflet.{FileStorage, Produce}
import com.typesafe.config.ConfigFactory
import org.apache.commons.io.FileUtils
import util.ResourceUtil

sealed trait DocsBuilder {
  self: DirectoryHandler with RepositoryService with DocsIndexer =>

  def initAndBuildProject(projectWithVersions: ProjectAndVersions): Try[Unit]
  def update(project: Project, existingVersions: Seq[ProjectVersion]): Try[Unit]
}

trait PamfletDocsBuilder extends DocsBuilder {
  self: DirectoryHandler with RepositoryService with DocsIndexer =>

  def update(project: Project, existingVersions: Seq[ProjectVersion]): Try[Unit] = {
    clean(project).map { _ =>
      clone(project).map { _ =>
        getVersions(project).map { versions =>
          val newVersions = Seq(ProjectHelper.defaultProjectVersion) ++ versions.diff(existingVersions)
          for {
            version         <- newVersions
            indexHtmlOption <- build(project, version)
            indexHtml       <- indexHtmlOption
          } index(project, version).map { _ =>
            BuildDAO.insertBuildSuccess(project, version, indexHtml)
          }.recover {
            case e: Exception =>
              val buildDir: File = buildDirForProjectVersion(project, version)
              handleBuildFailed(project, version, "Indexing build directory ["+buildDir.getAbsolutePath+"] failed.", e)
          }
        }
      }
    }
  }

  def initAndBuildProject(projectWithVersions: ProjectAndVersions): Try[Unit] = {
    clone(projectWithVersions.project).recover {
      case e: Exception =>
        handleBuildFailed(projectWithVersions.project, projectWithVersions.versions, "Clone of ["+projectWithVersions.project.url+"] failed.", e)
    }.map { _ =>
      for {
        version          <- projectWithVersions.versions
        indexHtmlOption  <- build(projectWithVersions.project, version)
        indexHtml        <- indexHtmlOption
      } index(projectWithVersions.project, version).map { _ =>
        BuildDAO.insertBuildSuccess(projectWithVersions.project, version, indexHtml)
      }.recover {
        case e: Exception =>
          val buildDir: File = buildDirForProjectVersion(projectWithVersions.project, version)
          handleBuildFailed(projectWithVersions.project, version, "Indexing build directory ["+buildDir.getAbsolutePath+"] failed.", e)
      }
    }
  }

  private def build(project: Project, version: ProjectVersion): Try[Option[String]] = {
    build(project, version, repositoryForProject(project))
  }

  private def build(project: Project, version: ProjectVersion, checkoutDir: File): Try[Option[String]] = {
    val buildDir: File = buildDirForProjectVersion(project, version)
    checkout(project, version).recover {
      case e: Exception => handleBuildFailed(project, version, "Checkout failed.", e)
    }.map { _ =>
      val inputDir: File = parseYamlConfig(checkoutDir)
      if(!inputDir.exists()) {
        throw new BuildFailedException("No document directory exists at ["+inputDir.getAbsolutePath+"].")
      } else {
        Try{FileUtils.cleanDirectory(buildDir)} // Quietly empty the directory
        Logger.info("Building... project=["+project.slug+"] version=["+version.versionName+"] input=["+inputDir.getAbsolutePath+"] output=["+buildDir.getAbsoluteFile+"]")
        val pamfletStorage = FileStorage(inputDir).globalized
        val indexHtml = pamfletStorage.defaultContents.rootSection.template.get("out").getOrElse {
          ResourceUtil.encodeFileName(pamfletStorage.defaultContents.title)+".html"
        }
        Produce(pamfletStorage, buildDir)
        Some(indexHtml)
      }
    }.recover {
      case e: Exception =>
        handleBuildFailed(project, version, "Building of documents to ["+buildDir.getAbsoluteFile+"] failed.", e)
        None
    }
  }

  private def parseYamlConfig(checkoutDir: File): File = {
    val rtmYaml = new File(checkoutDir, "rtm.yaml")
    if(rtmYaml.exists()) {
      new File(checkoutDir, ConfigFactory.parseFile(rtmYaml).getString("docs.directory"))
    } else {
      Logger.warn("Config rtm.yaml not found in project root ["+checkoutDir.getAbsoluteFile+"]")
      new File(checkoutDir, Global.configuration.getString("default.docs.dir").getOrElse("docs"))
    }
  }

  private def handleBuildFailed(project: Project, version: ProjectVersion, msg: String, e: Exception): Unit = {
    BuildDAO.insertBuildFailure(project, version, msg + " - " + e.getMessage)
    throw e
  }

  private def handleBuildFailed(project: Project, versions: Seq[ProjectVersion], msg: String, e: Exception): Unit = {
    versions.foreach { version =>
      try {handleBuildFailed(project, version, msg, e)}catch{case e:Exception => }
    }
    throw e
  }

}

class BuildFailedException(msg: String) extends IllegalStateException(msg)