package build

import java.io.{FileNotFoundException, File}
import java.io.{File => JFile}

import models._
import settings.Global

sealed trait DirectoryHandler {
  def repositoryForProject(project: Project): JFile
  def indexDir: JFile
}

trait DirectoryHandlerImpl extends DirectoryHandler {

  lazy val gitCheckoutsDir: JFile = {
    val checkoutDir = new JFile(Global.configuration.getString("git.checkouts.dir").getOrElse("./.git_checkouts"))
    if(checkoutDir.exists() || checkoutDir.mkdirs()) {
      checkoutDir
    } else {
      throw new FileNotFoundException
    }
  }

  lazy val luceneIndexDir: JFile = {
    val indexesDir = new JFile(Global.configuration.getString("index.dir").getOrElse("./.index"))
    if(indexesDir.exists() || indexesDir.mkdirs()) {
      indexesDir
    } else {
      throw new FileNotFoundException
    }
  }

  def repositoryForProject(project: Project): JFile =
    new JFile(gitCheckoutsDir, project.url_key.value)

  def indexDir: File = luceneIndexDir
}
