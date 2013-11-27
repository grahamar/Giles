package settings

import dao.util.{RegisterAnyValConversionHelpers, Index, MongoUtil}
import play.api.Application

object Global extends play.api.GlobalSettings {

  private lazy val projectIndexes = Seq(Index(field="url_key", unique=true), Index(field="name", unique=true))
  private lazy val viewIndexes = Seq(Index(field="file_guid"), Index(field="user_guid"))
  private lazy val userIndexes = Seq(Index(field="username", unique=true))

  lazy val projects = new dao.ProjectDao(MongoUtil.collectionWithIndexes("projects", projectIndexes))
  lazy val users = new dao.UserDao(MongoUtil.collectionWithIndexes("users", userIndexes))
  lazy val builds = new dao.BuildDao(MongoUtil.collectionWithIndexes("builds"))
  lazy val files = new dao.FilesDao(MongoUtil.collectionWithIndexes("files"))
  lazy val fileRollup = new dao.FileViewRollupDao()
  lazy val userFileRollup = new dao.UserFileViewRollupDao()
  lazy val views = new dao.ViewsDao(MongoUtil.collectionWithIndexes("views", viewIndexes), fileRollup, userFileRollup)

  override def onStart(app: Application) {

    import com.mongodb.casbah.commons.conversions.scala._

    RegisterJodaTimeConversionHelpers()
    RegisterConversionHelpers()
  }

}
