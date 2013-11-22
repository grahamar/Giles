package build

import java.io.File

import play.api.Logger

import dao.{BuildDAO, ProjectAndVersions, ProjectVersion, Project}
import settings.Global

import pamflet.{FileStorage, Produce}
import com.typesafe.config.ConfigFactory
import scala.util.Try

sealed trait DocsBuilder {
  self: DirectoryHandler with RepositoryService with DocsIndexer =>

  def clean(project: Project, version: ProjectVersion): Try[Unit]
  def build(project: Project): Try[Option[File]]
  def build(project: Project, version: ProjectVersion): Try[Option[File]]
  def initAndBuildProject(projectWithVersions: ProjectAndVersions): Try[Unit]
}

trait PamfletDocsBuilder extends DocsBuilder {
  self: DirectoryHandler with RepositoryService with DocsIndexer =>

  def clean(project: Project, version: ProjectVersion): Try[Unit] = Try {
    Logger.info("Cleaning version ["+version.versionName+"]")
    buildDirForProjectVersion(project, version).delete()
  }

  def build(project: Project): Try[Option[File]] = {
    build(project, project.defaultVersion)
  }

  def build(project: Project, version: ProjectVersion): Try[Option[File]] = {
    build(project, version, repositoryForProject(project))
  }

  def initAndBuildProject(projectWithVersions: ProjectAndVersions): Try[Unit] = {
    clone(projectWithVersions.project).recover {
      case e: Exception =>
        handleBuildFailed(projectWithVersions.project, projectWithVersions.versions, "Clone of ["+projectWithVersions.project.url+"] failed.", e)
    }.map { _ =>
      for {
        version         <- projectWithVersions.versions
        buildDirOption  <- build(projectWithVersions.project, version)
        buildDir        <- buildDirOption
      } index(projectWithVersions.project, version, buildDir).map { _ =>
        BuildDAO.insertBuildSuccess(projectWithVersions.project, version)
      }.recover {
        case e: Exception =>
          handleBuildFailed(projectWithVersions.project, version, "Indexing build directory ["+buildDir.getAbsolutePath+"] failed.", e)
      }
    }
  }

  private def build(project: Project, version: ProjectVersion, checkoutDir: File): Try[Option[File]] = {
    val buildDir: File = buildDirForProjectVersion(project, version)
    checkout(project, version).recover {
      case e: Exception => handleBuildFailed(project, version, "Checkout failed.", e)
    }.map { _ =>
      val inputDir: File = parseYamlConfig(checkoutDir)
      if(!inputDir.exists()) {
        throw new BuildFailedException("No document directory exists at ["+inputDir.getAbsolutePath+"].")
      } else {
        Logger.info("Building... project=["+project.slug+"] version=["+version.versionName+"] input=["+inputDir.getAbsolutePath+"] output=["+buildDir.getAbsoluteFile+"]")
        Produce(FileStorage(inputDir).globalized, buildDir)
      }
      Some(buildDir)
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
  }

  private def handleBuildFailed(project: Project, versions: Seq[ProjectVersion], msg: String, e: Exception): Unit = {
    versions.foreach { version =>
      handleBuildFailed(project, version, msg, e)
    }
  }

}

class BuildFailedException(msg: String) extends IllegalStateException(msg)