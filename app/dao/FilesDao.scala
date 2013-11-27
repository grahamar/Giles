package dao

import models._

import com.novus.salat._
import com.novus.salat.global._
import com.mongodb.casbah.Imports._
import org.joda.time.DateTime

class FilesDao(files: MongoCollection) {

  def create(guid: Guid, project: Project, version: Version, title: String, html: String): File = {
    val urlKey = UrlKey.generate(title)
    val file = File(guid = guid,
      project_guid = project.guid,
      version = version,
      title = title,
      url_key = urlKey,
      keywords = Keywords.generate(Seq(guid.value, title, html, urlKey.value)),
      html = html,
      created_at = new DateTime())

    files.insert(grater[File].asDBObject(file))

    file
  }

  def update(file: File) {
    val obj = MongoDBObject("html" -> file.html)
    files.update(q = MongoDBObject("guid" -> file.guid),
      o = MongoDBObject("$set" -> obj),
      upsert = false,
      multi = false)
  }

  def findByGuid(guid: Guid): Option[File] = {
    search(FileQuery(guid = Some(guid))).headOption
  }

  def findAllByProjectGuidAndVersion(projectGuid: Guid, version: Version): Iterable[File] = {
    search(FileQuery(project_guid = Some(projectGuid), version = Some(version)))
  }

  def delete(guid: Guid) = {
    // TODO: Soft delete?
    files.remove(MongoDBObject("guid" -> guid.value))
  }

  def search(query: FileQuery): Iterable[File] = {
    val builder = MongoDBObject.newBuilder
    query.guid.foreach { v => builder += "guid" -> v }
    query.project_guid.foreach { v => builder += "application_guid" -> v.value }
    query.version.foreach { v => builder += "version" -> v.name }
    query.title.foreach { v => builder += "title" -> v }
    query.url_key.foreach { v => builder += "url_key" -> v }
    query.query.foreach { v => builder += "keywords" -> v.toLowerCase.r }

    files.find(builder.result()).
      skip(query.pagination.offsetOrDefault).
      limit(query.pagination.limitOrDefault).
      toList.map(grater[File].asObject(_)).sorted
  }

}
