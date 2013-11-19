package build

import java.io.{FileNotFoundException, File}

import dao.{ProjectVersion, Project}
import settings.Global

sealed trait DirectoryHandler {
  def repositoryForProjectVersion(project: Project, version: ProjectVersion): File
  def buildDirForProjectVersion(project: Project, version: ProjectVersion): File
  def indexDir: File
}

trait DirectoryHandlerImpl extends DirectoryHandler {

  lazy val gitCheckoutsDir: File = {
    val checkoutDir = new File(Global.configuration.getString("git.checkouts.dir").getOrElse("./.git_checkouts"))
    if(checkoutDir.exists() || checkoutDir.mkdirs()) {
      checkoutDir
    } else {
      throw new FileNotFoundException
    }
  }

  lazy val buildsDir: File = {
    val buildDir = new File(Global.configuration.getString("build.dir").getOrElse("./.builds"))
    if(buildDir.exists() || buildDir.mkdirs()) {
      buildDir
    } else {
      throw new FileNotFoundException
    }
  }

  lazy val luceneIndexDir: File = {
    val indexesDir = new File(Global.configuration.getString("index.dir").getOrElse("./.index"))
    if(indexesDir.exists() || indexesDir.mkdirs()) {
      indexesDir
    } else {
      throw new FileNotFoundException
    }
  }

  def repositoryForProjectVersion(project: Project, version: ProjectVersion): File = {
    new File(gitCheckoutsDir, project.slug+File.separator+version.versionName)
  }

  def buildDirForProjectVersion(project: Project, version: ProjectVersion): File = {
    new File(buildsDir, project.slug+File.separator+version.versionName)
  }

  def indexDir: File = luceneIndexDir
}
