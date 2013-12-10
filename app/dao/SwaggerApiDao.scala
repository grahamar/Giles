package dao

import java.util.UUID

import models.{Project, SwaggerApiFileQuery, SwaggerApiFile}

import com.novus.salat._
import com.mongodb.casbah.Imports._
import org.joda.time.DateTime

class SwaggerApiDao(swaggerFiles: MongoCollection) {

  def create(project_guid: String, version: String, path: Option[String], content_guid: String): SwaggerApiFile = {
    val apiFile = SwaggerApiFile(guid = UUID.randomUUID().toString,
      project_guid = project_guid,
      version = version,
      path = path.getOrElse(""),
      listing = path.isEmpty,
      content_guid = content_guid,
      created_at = new DateTime()
    )
    swaggerFiles.insert(grater[SwaggerApiFile].asDBObject(apiFile))
    apiFile
  }

  def findByGuid(guid: String): Option[SwaggerApiFile] = {
    search(SwaggerApiFileQuery(guid = Some(guid))).headOption
  }

  def findByProjectAndVersion(project_guid: String, version: String, path: Option[String]): Option[SwaggerApiFile] = {
    search(SwaggerApiFileQuery(project_guid = Some(project_guid), version = Some(version),
      listing = Some(path.isEmpty), path = Some(path.getOrElse("")))).headOption
  }

  def update(file: SwaggerApiFile) = {
    val obj = MongoDBObject("content_guid" -> file.content_guid)
    swaggerFiles.update(q = MongoDBObject("guid" -> file.guid),
      o = MongoDBObject("$set" -> obj),
      upsert = false,
      multi = false)
  }

  def delete(guid: String) = {
    // TODO: Soft delete?
    swaggerFiles.remove(MongoDBObject("guid" -> guid))
  }

  def search(query: SwaggerApiFileQuery): Iterable[SwaggerApiFile] = {
    val builder = MongoDBObject.newBuilder
    query.guid.foreach { v => builder += "guid" -> v }
    query.project_guid.foreach { v => builder += ("project_guid" -> v) }
    query.version.foreach { v => builder += ("version" -> v) }
    query.listing.foreach { v => builder += ("listing" -> v) }
    query.path.foreach { v => builder += ("path" -> v) }

    swaggerFiles.find(builder.result()).
      skip(query.pagination.offsetOrDefault).
      limit(query.pagination.limitOrDefault).
      toList.map(grater[SwaggerApiFile].asObject(_))
  }

}
