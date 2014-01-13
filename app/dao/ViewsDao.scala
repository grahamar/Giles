package dao

import models._

import com.novus.salat._
import com.mongodb.casbah.Imports._
import org.joda.time.DateTime
import play.api.Logger

/**
 * A view is essentially a page view. We de-normalise views by
 * file, rolling up by:
 *   - day
 *   - month
 *   - year
 *
 * We also keep a second de-normalisation by user - rolling up the
 * files a user accesses most often
 */
class ViewsDao(views: MongoCollection, fileRollup: FileViewRollupDao, userFileRollup: UserFileViewRollupDao) {

  def findByGuid(guid: String): Option[View] = {
    search(ViewQuery(guid = Some(guid))).headOption
  }

  def create(guid: String, fileGuid: String, userGuid: Option[String]): View = {
    val view = View(guid = guid,
      file_guid = fileGuid,
      user_guid = userGuid,
      created_at = new DateTime())

    views.insert(grater[View].asDBObject(view))
    fileRollup.increment(fileGuid)
    userGuid.foreach { g => userFileRollup.increment(g, fileGuid) }
    view
  }

  def deleteFile(fileGuid: String) = {
    // TODO: Soft delete?
    fileRollup.delete(fileGuid)
    userFileRollup.deleteFile(fileGuid)
    views.remove(MongoDBObject("file_guid" -> fileGuid))
  }

  def numberViewsForFile(fileGuid: String): Long = {
    fileRollup.numberViews(fileGuid)
  }

  def numberViewsForUserAndFile(userGuid: String, fileGuid: String): Long = {
    userFileRollup.numberViews(userGuid, fileGuid)
  }

  def search(query: ViewQuery): Iterable[View] = {
    val builder = MongoDBObject.newBuilder
    query.guid.foreach { v => builder += "guid" -> v }
    query.file_guid.foreach { v => builder += "file_guid" -> v }
    query.user_guid.foreach { v => builder += "user_guid" -> v }
    query.created_at.foreach { v => builder += "created_at" -> v }
    query.start_date.foreach { start =>
      query.end_at.foreach { end =>
        builder += ("created_at" -> MongoDBObject("$gte" -> start))
      }
    }

    Logger.info("=>"+builder.result())

    views.find(builder.result()).toList.map(grater[View].asObject(_))
  }

}
