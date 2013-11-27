package dao

import models._

import com.novus.salat._
import com.novus.salat.global._
import com.mongodb.casbah.Imports._
import org.joda.time.DateTime

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

  def findByGuid(guid: Guid): Option[View] = {
    search(ViewQuery(guid = Some(guid))).headOption
  }

  def create(guid: Guid, fileGuid: Guid, userGuid: Option[Guid]): View = {
    val view = View(guid = guid,
      file_guid = fileGuid,
      user_guid = userGuid,
      created_at = new DateTime())

    views.insert(grater[View].asDBObject(view))
    fileRollup.increment(fileGuid)
    userGuid.foreach { g => userFileRollup.increment(g, fileGuid) }
    view
  }

  def deleteFile(fileGuid: Guid) = {
    // TODO: Soft delete?
    fileRollup.delete(fileGuid)
    userFileRollup.deleteFile(fileGuid)
    views.remove(MongoDBObject("file_guid" -> fileGuid))
  }

  def numberViewsForFile(fileGuid: Guid): Long = {
    fileRollup.numberViews(fileGuid)
  }

  def numberViewsForUserAndFile(userGuid: Guid, fileGuid: Guid): Long = {
    userFileRollup.numberViews(userGuid, fileGuid)
  }

  private def search(query: ViewQuery): Iterable[View] = {
    val builder = MongoDBObject.newBuilder
    query.guid.foreach { v => builder += "guid" -> v }
    query.file_guid.foreach { v => builder += "file_guid" -> v }
    query.user_guid.foreach { v => builder += "user_guid" -> v }

    views.find(builder.result()).toList.map(grater[View].asObject(_))
  }

}
