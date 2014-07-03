package build

import java.io.{FileNotFoundException, File => JFile}

import play.api.Logger

import com.typesafe.config.ConfigFactory
import models._

import scala.util.Try

object DirectoryHandlerHelper {

  private lazy val ConfigFile = new JFile(sys.props.get("config.file").getOrElse("conf/application.conf"))
  lazy val Config = ConfigFactory.parseFile(ConfigFile)

  def indexDirFromConfig = {
    new JFile(Try {
        Config.getString("index.dir")
      }.toOption.filterNot(_.isEmpty).getOrElse {
        Logger.warn("""Unable to find "index.dir" configuration.""")
        "./.index"
      }
    )
  }
}

sealed trait DirectoryHandler {
  def repositoryForProject(project: Project): JFile
  def indexDir: JFile
}

trait DirectoryHandlerImpl extends DirectoryHandler {
  import build.DirectoryHandlerHelper._

  lazy val gitCheckoutsDir: JFile = {
    val checkoutDir = new JFile(Try(Config.getString("git.checkouts.dir")).getOrElse {
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
