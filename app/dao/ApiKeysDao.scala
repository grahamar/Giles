package dao

import models.{ApiKeyQuery, User, ApiKey}

import com.novus.salat._
import com.mongodb.casbah.Imports._

class ApiKeysDao(apiKeys: MongoCollection) {

  def create(guid: String, user_guid: String, application_name: String, api_key: String): ApiKey = {
    val apiKey = ApiKey(guid = guid,
      user_guid = user_guid,
      application_name = application_name,
      api_key = api_key)
    apiKeys.insert(grater[ApiKey].asDBObject(apiKey))
    apiKey
  }

  def findByGuid(guid: String): Option[ApiKey] = {
    search(ApiKeyQuery(guid = Some(guid))).headOption
  }

  def findUser(user: User): Iterable[ApiKey] = {
    search(ApiKeyQuery(user_guid = Some(user.guid)))
  }

  def findByApiKey(api_key: String): Option[ApiKey] = {
    search(ApiKeyQuery(api_key = Some(api_key))).headOption
  }

  def delete(guid: String) = {
    // TODO: Soft delete?
    apiKeys.remove(MongoDBObject("guid" -> guid))
  }

  def search(query: ApiKeyQuery): Iterable[ApiKey] = {
    val builder = MongoDBObject.newBuilder
    query.guid.foreach { v => builder += "guid" -> v }
    query.user_guid.foreach { v => builder += "user_guid" -> v }
    query.api_key.foreach { v => builder += "api_key" -> v }
    query.application_name.foreach { v => builder += "application_name" -> v }

    apiKeys.find(builder.result()).
      skip(query.pagination.offsetOrDefault).
      limit(query.pagination.limitOrDefault).
      toList.map(grater[ApiKey].asObject(_))
  }

}
