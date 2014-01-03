package build

import java.io.{FileNotFoundException, File}
import java.io.{File => JFile}

import models._
import settings.Global
import play.api.Logger

sealed trait DirectoryHandler {
  def repositoryForProject(project: Project): JFile
  def indexDir: JFile
}

trait DirectoryHandlerImpl extends DirectoryHandler {

  lazy val gitCheckoutsDir: JFile = {
    val checkoutDir = new JFile(Global.configuration.getString("git.checkouts.dir").getOrElse {
      Logger.warn("""Unable to find "git.checkouts.dir" configuration.""")
      "./.git_checkouts"
    })
    if(checkoutDir.exists() || checkoutDir.mkdirs()) {
      checkoutDir
    } else {
      throw new FileNotFoundException(checkoutDir.getAbsolutePath)
    }
  }

  lazy val luceneIndexDir: JFile = {
    val indexesDir = new JFile(Global.configuration.getString("index.dir").getOrElse {
      Logger.warn("""Unable to find "index.dir" configuration.""")
      "./.index"
    })
    if(indexesDir.exists() || indexesDir.mkdirs()) {
      indexesDir
    } else {
      throw new FileNotFoundException(indexesDir.getAbsolutePath)
    }
  }

  def repositoryForProject(project: Project): JFile =
    new JFile(gitCheckoutsDir, project.url_key)

  def indexDir: File = luceneIndexDir
}
