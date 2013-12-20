package build

import java.io.{File => JFile}

import scala.collection.JavaConverters._
import scala.util.Try
import play.api.Logger

import models._
import settings.Global

import org.apache.commons.io.{FilenameUtils, FileUtils}
import util.{EmailUtil, Util}

sealed trait DocsBuilder {
  self: DirectoryHandler with RepositoryService with DocsIndexer =>

  def build(project: Project): Try[Unit]
  def cleanBuild(project: Project, version: String): Try[Unit]
}

trait AbstractDocsBuilder extends DocsBuilder {
  self: DirectoryHandler with RepositoryService with DocsIndexer with DocTypeBuilder =>

  def build(project: Project): Try[Unit] = {
    for {
      _         <- cleanRepo(project)
      _         <- clone(project)
      versions  <- getVersions(project)
    } yield {
      val newVersions = versions.diff(project.versions)
      val updatedProject = project.copy(versions = project.versions ++ newVersions)
      Global.projects.update(updatedProject)
      buildProjectAndVersions(updatedProject, Seq(project.head_version) ++ newVersions)
    }
  }

  def cleanBuild(project: Project, version: String): Try[Unit] = Try {
    Global.files.findAllByProjectGuidAndVersion(project.guid, version).foreach { file =>
      Global.files.delete(file.guid)
    }
  }

  private def buildProjectAndVersions(project: Project, versions: Seq[String]): Seq[Try[Build]] = {
    versions.map { version =>
      for {
        _       <- checkout(project, version)
        authors <- getAuthors(project)
        _       <- cleanBuild(project, version)
        _       <- build(project, version)
        _       <- index(project, version)
      } yield {
        val build = Global.builds.createSuccess(project.guid, version, authors)
        updateProjectAuthors(project)
        if(!version.equals(project.head_version)) {
          Global.favourites.findAllByProject(project).flatMap(fav => Global.users.findByGuid(fav.user_guid)).
            foreach(EmailUtil.sendNewVersionEmail(_, project, version))
        }
        build
      }
    }
  }

  private def updateProjectAuthors(project: Project): Unit = {
    val topAuthors = Util.topAuthorUsernames(4, Global.builds.search(BuildQuery(project_guid = Some(project.guid))).flatMap(_.authors).toSeq)
    Global.projects.update(project.copy(author_usernames = topAuthors))
  }

  private def build(project: Project, version: String): Try[Unit] = Try {
    val inputDir = repositoryForProject(project)
    if(!inputDir.exists()) {
      throw new BuildFailedException("No directory exists at ["+inputDir.getAbsolutePath+"].")
    }
    Logger.info("Building... project=["+project.name+"] version=["+version+"] input=["+inputDir.getAbsolutePath+"]")

    FileUtils.iterateFiles(inputDir, supportedFileExtensions, true).asScala.foreach { document =>
      val filename = FilenameUtils.getName(document.getName)
      val relativePath = normaliseRelativePath(document, inputDir)

      buildDocument(project, version, document, filename, relativePath)
    }
  }.recover {
    case e: Exception => {
      Global.builds.createFailure(project.guid, version, "Build failed - "+ e.getMessage)
      Logger.error("Exception", e)
      throw e
    }
  }

  private def normaliseRelativePath(document: JFile, inputDir: JFile): String = {
    val relativePath = FilenameUtils.getFullPath(document.getAbsolutePath).substring(inputDir.getAbsolutePath.length)
    if(relativePath.startsWith("/") || relativePath.startsWith("\\")) {
      relativePath.substring(1)
    } else {
      relativePath
    }
  }

}

class BuildFailedException(msg: String) extends IllegalStateException(msg)