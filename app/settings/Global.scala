package settings

import java.io.{File => JFile}

import build.{DirectoryHandlerHelper, DefaultAmazonS3Client}
import com.typesafe.config.ConfigFactory
import com.wordnik.swagger.config.SwaggerConfig
import com.wordnik.swagger.core.SwaggerSpec
import dao.util.{Index, MongoUtil}
import play.api.Application

object Global extends play.api.GlobalSettings {

  val gilesS3Client = DefaultAmazonS3Client(ConfigFactory.load("giles-aws"))
  gilesS3Client.tryRestoreIndex(DirectoryHandlerHelper.indexDirFromConfig)

  val ActivatorCacheBaseFile: JFile = {
    val f = new JFile(DirectoryHandlerHelper.Config.getString("activator.cache.dir")).getAbsoluteFile
    if(!f.exists() && !f.mkdirs())
      sys.error(s"Unable to create '${f.getAbsolutePath}'")
    else
      f
  }
  val ActivatorCacheIndexDir: JFile = {
    val f = new JFile(ActivatorCacheBaseFile, "index")
    if(!f.exists() && !f.mkdirs())
      sys.error(s"Unable to create '${f.getAbsolutePath}'")
    else
      f
  }
  val activatorS3Client = DefaultAmazonS3Client(ConfigFactory.load("activator-aws"))
  activatorS3Client.tryRestoreIndex(ActivatorCacheIndexDir)

  private lazy val projectIndexes = Seq(Index(field="url_key", unique=true), Index(field="name", unique=true), Index(field="updated_at"))
  private lazy val viewIndexes = Seq(Index(field="file_guid"), Index(field="user_guid"))
  private lazy val userIndexes = Seq(Index(field="username", unique=true))
  private lazy val buildIndexes = Seq(Index(field="project_guid"), Index(field="version"))
  private lazy val fileContentIndexes = Seq(Index(field="hash_key", unique=true))
  private lazy val favouriteIndexes = Seq(Index(field="user_guid"))
  private lazy val publicationIndexes = Seq(Index(field="user_guid"), Index(field="url_key", unique=true))
  private lazy val apiKeysIndexes = Seq(Index(field="user_guid"), Index(field="application_name"))

  lazy val projects = new dao.ProjectDao(MongoUtil.collectionWithIndexes("projects", projectIndexes))
  lazy val favourites = new dao.FavouriteDao(MongoUtil.collectionWithIndexes("favourites", favouriteIndexes))
  lazy val users = new dao.UserDao(MongoUtil.collectionWithIndexes("users", userIndexes))
  lazy val builds = new dao.BuildDao(MongoUtil.collectionWithIndexes("builds", buildIndexes))
  lazy val files = new dao.FilesDao(MongoUtil.collectionWithIndexes("files"))
  lazy val fileContents = new dao.FileContentsDao(MongoUtil.collectionWithIndexes("file_contents", fileContentIndexes))
  lazy val fileRollup = new dao.FileViewRollupDao()
  lazy val userFileRollup = new dao.UserFileViewRollupDao()
  lazy val views = new dao.ViewsDao(MongoUtil.collectionWithIndexes("views", viewIndexes), fileRollup, userFileRollup)
  lazy val publications = new dao.PublicationDao(MongoUtil.collectionWithIndexes("publications", publicationIndexes))
  lazy val apiKeys = new dao.ApiKeysDao(MongoUtil.collectionWithIndexes("api_keys", apiKeysIndexes))
  lazy val swaggerApiFiles = new dao.SwaggerApiDao(MongoUtil.collectionWithIndexes("swagger_api_files"))

  override def onStart(app: Application) {

    import com.mongodb.casbah.commons.conversions.scala._

    RegisterJodaTimeConversionHelpers()
    RegisterConversionHelpers()

    com.wordnik.swagger.config.ConfigFactory.setConfig(new SwaggerConfig("0.1", SwaggerSpec.version, "http://localhost:9000", "/api"))

  }

}
