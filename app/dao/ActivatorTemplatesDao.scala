package dao

import java.util.UUID

import com.mongodb.casbah.Imports._
import com.novus.salat._
import models.{ActivatorTemplateModelQuery, ActivatorTemplateModel}
import org.joda.time.DateTime

class ActivatorTemplatesDao(templates: MongoCollection) {

  def create(repo: String): ActivatorTemplateModel = {
    val apiFile = ActivatorTemplateModel(guid = UUID.randomUUID().toString,
      repo = repo,
      created_at = new DateTime()
    )
    templates.insert(grater[ActivatorTemplateModel].asDBObject(apiFile))
    apiFile
  }

  def findByGuid(guid: String): Option[ActivatorTemplateModel] = {
    search(ActivatorTemplateModelQuery(guid = Some(guid))).headOption
  }

  def findByRepo(repo: String): Option[ActivatorTemplateModel] = {
    search(ActivatorTemplateModelQuery(repo = Some(repo))).headOption
  }

  def delete(guid: String) = {
    // TODO: Soft delete?
    templates.remove(MongoDBObject("guid" -> guid))
  }

  def search(query: ActivatorTemplateModelQuery): Iterable[ActivatorTemplateModel] = {
    val builder = MongoDBObject.newBuilder
    query.guid.foreach { v => builder += "guid" -> v }
    query.repo.foreach { v => builder += ("repo" -> v) }

    templates.find(builder.result()).
      skip(query.pagination.offsetOrDefault).
      limit(query.pagination.limitOrDefault).
      toList.map(grater[ActivatorTemplateModel].asObject(_))
  }

}
