package build

import java.io.{FileNotFoundException, File => JFile}

import com.typesafe.config.ConfigFactory
import models._
import play.api.{Logger, Play}

import scala.util.Try

object DirectoryHandlerHelper {
  def indexDirFromConfig = {
    new JFile(Try(ConfigFactory.load("application").getString("index.dir")).toOption.filterNot(_.isEmpty).getOrElse {
      Logger.warn("""Unable to find "index.dir" configuration.""")
      "./.index"
    })
  }
}

sealed trait DirectoryHandler {
  def repositoryForProject(project: Project): JFile
  def indexDir: JFile
}

trait DirectoryHandlerImpl extends DirectoryHandler {

  lazy val gitCheckoutsDir: JFile = {
    val checkoutDir = new JFile(Play.current.configuration.getString("git.checkouts.dir").getOrElse {
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
    val indexesDir = DirectoryHandlerHelper.indexDirFromConfig
    if(indexesDir.exists() || indexesDir.mkdirs()) {
      indexesDir
    } else {
      throw new FileNotFoundException(indexesDir.getAbsolutePath)
    }
  }

  def repositoryForProject(project: Project): JFile =
    new JFile(gitCheckoutsDir, project.url_key)

  def indexDir: JFile = luceneIndexDir
}
