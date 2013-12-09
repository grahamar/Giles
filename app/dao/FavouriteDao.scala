package dao

import java.util.UUID

import models._

import com.novus.salat._
import com.mongodb.casbah.Imports._

class FavouriteDao(favourites: MongoCollection) {

  def create(guid: String, user: User, project: Project): Favourite = {
    val favourite = Favourite(guid = guid, user_guid = user.guid, project_guid = project.guid)
    favourites.insert(grater[Favourite].asDBObject(favourite))
    favourite
  }

  def create(user: User, project: Project): Favourite = {
    create(UUID.randomUUID().toString, user, project)
  }

  def findByGuid(guid: String): Option[Favourite] = {
    search(FavouriteQuery(guid = Some(guid))).headOption
  }

  def findAllByProject(project: Project): Iterable[Favourite] = {
    search(FavouriteQuery(project_guid = Some(project.guid)))
  }

  def findAllByUser(user: User): Iterable[Favourite] = {
    search(FavouriteQuery(user_guid = Some(user.guid)))
  }

  def findByUserAndProject(user: User, project: Project): Option[Favourite] = {
    search(FavouriteQuery(user_guid = Some(user.guid), project_guid = Some(project.guid))).headOption
  }

  def delete(guid: String) = {
    favourites.remove(MongoDBObject("guid" -> guid))
  }

  def search(query: FavouriteQuery): Iterable[Favourite] = {
    val builder = MongoDBObject.newBuilder
    query.guid.foreach { v => builder += "guid" -> v }
    query.user_guid.foreach { v => builder += ("user_guid" -> v) }
    query.project_guid.foreach { v => builder += ("project_guid" -> v) }

    val sortBuilder = MongoDBObject.newBuilder
    query.order_by.foreach { field => sortBuilder += field -> query.order_direction }

    favourites.find(builder.result()).
      skip(query.pagination.offsetOrDefault).
      sort(sortBuilder.result()).
      limit(query.pagination.limitOrDefault).
      toList.map(grater[Favourite].asObject(_))
  }

}
