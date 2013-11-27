package settings

import dao.util.{Index, MongoUtil}

object Global extends play.api.GlobalSettings {

  private lazy val projectIndexes = Seq(Index(field="url_key", unique=true), Index(field="name", unique=true))
  private lazy val viewIndexes = Seq(Index(field="file_guid"), Index(field="user_guid"))
  private lazy val userIndexes = Seq(Index(field="username", unique=true))

  lazy val projects = new dao.ProjectDao(MongoUtil.collectionWithGuid("projects", projectIndexes))
  lazy val users = new dao.UserDao(MongoUtil.collectionWithGuid("users", userIndexes))
  lazy val builds = new dao.BuildDao(MongoUtil.collectionWithGuid("builds"))
  lazy val files = new dao.FilesDao(MongoUtil.collectionWithGuid("files"))
  lazy val fileRollup = new dao.FileViewRollupDao()
  lazy val userFileRollup = new dao.UserFileViewRollupDao()
  lazy val views = new dao.ViewsDao(MongoUtil.collectionWithGuid("views", viewIndexes), fileRollup, userFileRollup)

}
