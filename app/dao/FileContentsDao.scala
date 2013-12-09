package dao

import models._

import com.novus.salat._
import com.mongodb.casbah.Imports._

class FileContentsDao(contents: MongoCollection) {

  def create(guid: String, hashKey: String, contentSize: Long, content: Array[Byte]): FileContent = {
    val fileContent = FileContent(guid = guid,
      hash_key = hashKey,
      content_size = contentSize,
      content = content)

    contents.insert(grater[FileContent].asDBObject(fileContent))

    fileContent
  }

  def findByGuid(guid: String): Option[FileContent] = {
    search(FileContentsQuery(guid = Some(guid))).headOption
  }

  def findByHashAndSize(hashKey: String, contentSize: Long): Option[FileContent] = {
    search(FileContentsQuery(hash_key = Some(hashKey), content_size = Some(contentSize))).headOption
  }

  def delete(guid: String) = {
    contents.remove(MongoDBObject("guid" -> guid))
  }

  def search(query: FileContentsQuery): Iterable[FileContent] = {
    val builder = MongoDBObject.newBuilder
    query.guid.foreach { v => builder += "guid" -> v }
    query.hash_key.foreach { v => builder += "hash_key" -> v }
    query.content_size.foreach { v => builder += "content_size" -> v }

    contents.find(builder.result()).
      toList.map(grater[FileContent].asObject(_))
  }

}
