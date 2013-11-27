package build

import java.io.{File => JFile}

import scala.util.Try
import play.api.Logger

import models._
import settings.Global

import com.typesafe.config.ConfigFactory

sealed trait DocsBuilder {
  self: DirectoryHandler with RepositoryService with DocsIndexer =>

  def build(project: Project): Try[Unit]
}

trait MarkdownDocsBuilder extends DocsBuilder {
  self: DirectoryHandler with RepositoryService with DocsIndexer =>

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

  private def buildProjectAndVersions(project: Project, versions: Seq[Version]): Seq[Try[Build]] = {
    versions.map { version =>
      checkout(project, version).flatMap { _ =>
        build(project, version).flatMap { _ =>
          index(project, version).map { _ =>
            Global.builds.createSuccess(project.guid, version)
          }
        }
      }
    }
  }

  private def build(project: Project, version: Version): Try[Unit] = Try {
    val inputDir: JFile = parseYamlConfig(repositoryForProject(project))
    if(!inputDir.exists()) {
      throw new BuildFailedException("No document directory exists at ["+inputDir.getAbsolutePath+"].")
    }
    Logger.info("Building... project=["+project.name+"] version=["+version.name+"] input=["+inputDir.getAbsolutePath+"]")
    // TODO crawl through repo extracting markdown files and inserting them into mongo
  }.recover {
    case e: Exception => Global.builds.createFailure(project.guid, version, "Build failed - "+ e.getMessage)
  }

  private def parseYamlConfig(checkoutDir: JFile): JFile = {
    val rtmYaml = new JFile(checkoutDir, "rtm.yaml")
    if(rtmYaml.exists()) {
      new JFile(checkoutDir, ConfigFactory.parseFile(rtmYaml).getString("docs.directory"))
    } else {
      Logger.warn("Config rtm.yaml not found in project root ["+checkoutDir.getAbsoluteFile+"]")
      new JFile(checkoutDir, Global.configuration.getString("default.docs.dir").getOrElse("docs"))
    }
  }

}

class BuildFailedException(msg: String) extends IllegalStateException(msg)