package build

import java.io.{FileNotFoundException, File}

import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global
import play.api.Logger

import dao.{ProjectVersion, Project}
import settings.Global

import pamflet.{FileStorage, Produce}
import com.typesafe.config.{Config, ConfigFactory}

sealed trait DocsBuilder {
  self: DirectoryHandler with RepositoryService with DocsIndexer =>

  def clean(project: Project): Unit
  def build(project: Project): Unit
  def build(project: Project, version: ProjectVersion): Unit
  def force(project: Project): Unit
  def initAndBuildProject(project: Project): Unit
}

trait PamfletDocsBuilder extends DocsBuilder {
  self: DirectoryHandler with RepositoryService with DocsIndexer =>

  def clean(project: Project): Unit = {
    Logger.info("Clean")
  }

  def build(project: Project): Unit = {
    build(project, project.defaultVersion)
  }

  def build(project: Project, version: ProjectVersion): Unit = {
    build(project, version, repositoryForProjectVersion(project, version))
  }

  def force(project: Project): Unit = {
    Logger.info("Force Build")
  }

  def initAndBuildProject(project: Project): Unit = {
    for {
      checkoutDir <- checkoutOrUpdate(project, project.defaultBranch, project.defaultVersion)
      buildDir <- build(project, project.defaultVersion, checkoutDir)
    } yield index(project, project.defaultVersion, buildDir)
  }

  private def build(project: Project, version: ProjectVersion, checkoutDir: File): Future[File] = Future {
    val buildDir: File = buildDirForProjectVersion(project, version)
    val inputDir: File = parseYamlConfig(checkoutDir)
    Logger.info("Building... project=["+project.slug+"] version=["+version.versionName+"] input=["+inputDir.getAbsoluteFile+"] output=["+buildDir.getAbsoluteFile+"]")
    Produce(FileStorage(inputDir).globalized, buildDir)
    buildDir
  }

  private def parseYamlConfig(checkoutDir: File): File = {
    val rtmYaml = new File(checkoutDir, "rtm.yaml")
    if(rtmYaml.exists()) {
      val config = ConfigFactory.parseFile(rtmYaml)
      new File(checkoutDir, config.getString("docs.directory"))
    } else {
      Logger.info("Config rtm.yaml not found in project root ["+checkoutDir.getAbsoluteFile+"]")
      new File(checkoutDir, Global.configuration.getString("default.docs.dir").getOrElse("docs"))
    }
  }

}