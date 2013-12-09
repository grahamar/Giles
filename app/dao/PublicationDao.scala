package dao

import models._

import com.novus.salat._
import com.mongodb.casbah.Imports._
import org.joda.time.DateTime

class PublicationDao(publications: MongoCollection) {

  def create(guid: String, user: User, title: String, contentGuid: String): Publication = {
    val urlKey = UrlKey.generate(title)
    val publication = Publication(guid = guid,
      user_guid = user.guid,
      title = title,
      url_key = urlKey,
      content_guid = contentGuid,
      created_at = new DateTime())
    publications.insert(grater[Publication].asDBObject(publication))
    publication
  }

  def update(file: Publication) = {
    val obj = MongoDBObject("content_guid" -> file.content_guid)
    publications.update(q = MongoDBObject("guid" -> file.guid),
      o = MongoDBObject("$set" -> obj),
      upsert = false,
      multi = false)
  }

  def findByGuid(guid: String): Option[Publication] = {
    search(PublicationQuery(guid = Some(guid))).headOption
  }

  def findAllByUser(user: User): Iterable[Publication] = {
    search(PublicationQuery(user_guid = Some(user.guid)))
  }

  def findByUrlKey(urlKey: String): Option[Publication] = {
    search(PublicationQuery(url_key = Some(urlKey))).headOption
  }

  def delete(guid: String) = {
    // TODO: Soft delete?
    publications.remove(MongoDBObject("guid" -> guid))
  }

  def search(query: PublicationQuery): Iterable[Publication] = {
    val builder = MongoDBObject.newBuilder
    query.guid.foreach { v => builder += "guid" -> v }
    query.user_guid.foreach { v => builder += "user_guid" -> v }
    query.title.foreach { v => builder += "title" -> v }
    query.url_key.foreach { v => builder += "url_key" -> v }

    publications.find(builder.result()).
      skip(query.pagination.offsetOrDefault).
      limit(query.pagination.limitOrDefault).
      toList.map(grater[Publication].asObject(_))
  }

}
