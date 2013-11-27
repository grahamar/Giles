package dao

import models._
import util.{Index, MongoUtil}

import com.novus.salat._
import com.novus.salat.global._
import com.mongodb.casbah.Imports._
import java.util.UUID

class FileViewRollupDao {

  private lazy val fileViewsAllTime = MongoUtil.collection("file_views_all_time", Seq(Index(field="file_guid", unique=true)))

  def increment(fileGuid: UUID) {
    fileViewsAllTime.update(q = MongoDBObject("file_guid" -> fileGuid),
      o = $inc("count" -> 1),
      upsert = true,
      multi = false)
  }

  def numberViews(fileGuid: UUID): Long = {
    val result = fileViewsAllTime.find(MongoDBObject("file_guid" -> fileGuid)).toList.map(grater[FileRollup].asObject(_))
    result.headOption match {
      case Some(rollup: FileRollup) => rollup.count
      case None => 0
    }
  }

  def mostViewed(limit: Option[Int] = None): Iterable[FileRollup] = {
    search(FileRollupQuery(limit = limit,
      order_by = Some("count"),
      order_direction = -1))
  }

  def delete(fileGuid: UUID) {
    fileViewsAllTime.remove(MongoDBObject("file_guid" -> fileGuid))
  }

  def search(query: FileRollupQuery): Iterable[FileRollup] = {
    val builder = MongoDBObject.newBuilder
    query.file_guid.foreach { v => builder += "file_guid" -> v }

    val sortBuilder = MongoDBObject.newBuilder
    query.order_by.foreach { field => sortBuilder += field -> query.order_direction }

    val page = query.pagination
    fileViewsAllTime.find(builder.result()).
      skip(page.offsetOrDefault).
      sort(sortBuilder.result()).
      limit(page.limitOrDefault).
      toList.map(grater[FileRollup].asObject(_))
  }

}
