package build

import scala.collection.JavaConverters._
import scala.util.Try
import play.api.Logger

import models._
import settings.Global

import org.apache.commons.io.{FilenameUtils, FileUtils}
import java.util.UUID

sealed trait DocsBuilder {
  self: DirectoryHandler with RepositoryService with DocsIndexer =>

  def build(project: Project): Try[Unit]
  def clean(project: Project, version: String): Try[Unit]
}

trait MarkdownDocsBuilder extends DocsBuilder {
  self: DirectoryHandler with RepositoryService with DocsIndexer =>

  private val SupportedFileExtensions = Array("md", "markdown")

  def build(project: Project): Try[Unit] = {
    for {
      _         <- clean(project)
      _         <- clone(project)
      versions  <- getVersions(project)
    } yield {
      val newVersions = versions.diff(project.versions)
      Global.projects.update(project.copy(versions = project.versions ++ newVersions))
      buildProjectAndVersions(project, Seq(project.head_version) ++ newVersions)
    }
  }

  def clean(project: Project, version: String): Try[Unit] = Try {
    Global.files.findAllByProjectGuidAndVersion(project.guid, version).foreach { file =>
      Global.files.delete(file.guid)
    }
  }

  private def buildProjectAndVersions(project: Project, versions: Seq[String]): Seq[Try[Build]] = {
    versions.map { version =>
      checkout(project, version).flatMap { _ =>
        clean(project, version).flatMap { _ =>
          build(project, version).flatMap { _ =>
            index(project, version).map { _ =>
              Global.builds.createSuccess(project.guid, version)
            }
          }
        }
      }
    }
  }

  private def build(project: Project, version: String): Try[Unit] = Try {
    val inputDir = repositoryForProject(project)
    if(!inputDir.exists()) {
      throw new BuildFailedException("No directory exists at ["+inputDir.getAbsolutePath+"].")
    }
    Logger.info("Building... project=["+project.name+"] version=["+version+"] input=["+inputDir.getAbsolutePath+"]")
    FileUtils.iterateFiles(inputDir, SupportedFileExtensions, true).asScala.foreach { document =>
      val extension = FilenameUtils.getExtension(document.getName)
      val filename = FilenameUtils.getName(document.getName)
      val relativePath = FilenameUtils.getFullPath(document.getAbsolutePath).substring(inputDir.getAbsolutePath.length)
      Logger.info(s"Adding File. path=...$relativePath filename=$filename extension=$extension")

      Global.files.create(UUID.randomUUID(), project, version, filename, FileUtils.readFileToString(document, "UTF-8"))
    }
  }.recover {
    case e: Exception => {
      Global.builds.createFailure(project.guid, version, "Build failed - "+ e.getMessage)
      throw e
    }
  }

}

class BuildFailedException(msg: String) extends IllegalStateException(msg)