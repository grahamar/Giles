package dao

import models._

import com.novus.salat._
import com.mongodb.casbah.Imports._
import org.joda.time.DateTime
import org.apache.commons.io.FilenameUtils
import play.api.Logger

class FilesDao(files: MongoCollection) {

  def create(guid: String, project: Project, version: String, relativePath: String, filename: String, title: String, contentGuid: String): File = {
    val urlKey = UrlKey.generate(FilenameUtils.concat(relativePath, filename))
    val file = File(guid = guid,
      project_guid = project.guid,
      version = version,
      title = title,
      filename = filename,
      relative_path = relativePath,
      url_key = urlKey,
      content_guid = contentGuid,
      created_at = new DateTime())

    files.insert(grater[File].asDBObject(file))

    file
  }

  def update(file: File) = {
    val obj = MongoDBObject("content_guid" -> file.content_guid)
    files.update(q = MongoDBObject("guid" -> file.guid),
      o = MongoDBObject("$set" -> obj),
      upsert = false,
      multi = false)
  }

  def findByGuid(guid: String): Option[File] = {
    search(FileQuery(guid = Some(guid))).headOption
  }

  def findAllByProjectGuidAndVersion(projectGuid: String, version: String): Iterable[File] = {
    search(FileQuery(project_guid = Some(projectGuid), version = Some(version)))
  }

  def findByContentGuid(contentGuid: String): Iterable[File] = {
    search(FileQuery(content_guid = Some(contentGuid)))
  }

  def findForProjectGuidAndVersion(projectGuid: String, version: String, fileUrlKey: String): Option[File] = {
    search(FileQuery(
      project_guid = Some(projectGuid),
      version = Some(version),
      url_key = Some(fileUrlKey)
    )).headOption
  }

  def delete(guid: String) = {
    // TODO: Soft delete?
    files.remove(MongoDBObject("guid" -> guid))
  }

  def search(query: FileQuery): Iterable[File] = {
    val builder = MongoDBObject.newBuilder
    query.guid.foreach { v => builder += "guid" -> v }
    query.project_guid.foreach { v => builder += "project_guid" -> v }
    query.version.foreach { v => builder += "version" -> v }
    query.title.foreach { v => builder += "title" -> v }
    query.filename.foreach { v => builder += "filename" -> v }
    query.relative_path.foreach { v => builder += ("relative_path" -> v) }
    query.url_key.foreach { v => builder += "url_key" -> v }
    query.content_guid.foreach { v => builder += ("content_guid" -> v) }

    files.find(builder.result()).
      skip(query.pagination.offsetOrDefault).
      limit(query.pagination.limitOrDefault).
      toList.map(grater[File].asObject(_))
  }

}
