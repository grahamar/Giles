package build

import java.io.{File => JFile}
import java.util.UUID

import scala.collection.JavaConverters._
import scala.util.Try
import play.api.Logger

import models._
import settings.Global

import org.apache.commons.io.{FilenameUtils, FileUtils}

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

    import com.tristanhunt.knockoff.DefaultDiscounter._
    import com.tristanhunt.knockoff._

    FileUtils.iterateFiles(inputDir, SupportedFileExtensions, true).asScala.foreach { document =>
      val filename = FilenameUtils.getName(document.getName)
      val relativePath = normaliseRelativePath(document, inputDir)
      val blocks = knockoff(FileUtils.readFileToString(document, "UTF-8"))
      val fileTitle = blocks.find(_.isInstanceOf[Header]).map(header => toText(Seq(header))).getOrElse(filename)
      val htmlContent = toXHTML(blocks).toString()

      Global.files.create(UUID.randomUUID(), project, version, relativePath, filename, fileTitle, htmlContent)
    }
  }.recover {
    case e: Exception => {
      Global.builds.createFailure(project.guid, version, "Build failed - "+ e.getMessage)
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