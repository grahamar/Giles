package dao

import models._

import com.novus.salat._
import com.mongodb.casbah.Imports._
import org.joda.time.DateTime
import play.api.Logger

class ProjectDao(projects: MongoCollection) {

  def create(createdByUsername: String, guid: String, name: String, description: String = "", repoUrl: String, authorUsernames: Seq[String], headVersion: String = "HEAD"): Project = {
    val urlKey = UrlKey.generateProjectUrlKey(name)
    val project = Project(guid = guid,
      name = name,
      description = description,
      url_key = urlKey,
      repo_url = repoUrl,
      head_version = headVersion,
      versions = Seq.empty,
      author_usernames = authorUsernames,
      created_by = createdByUsername,
      created_at = new DateTime(),
      updated_at = new DateTime())
    create(project)
  }

  def create(project: Project): Project = {
    projects.insert(grater[Project].asDBObject(project))
    project
  }

  def markUpdated(project: Project) = {
    projects.update(q = MongoDBObject("guid" -> project.guid),
      o = MongoDBObject("$set" -> MongoDBObject("updated_at" -> new DateTime())),
      upsert = false, multi = false)
  }

  def update(project: Project) = {
    val obj = MongoDBObject("description" -> project.description, "repo_url" -> project.repo_url, "head_version" -> project.head_version,
      "versions" -> project.versions, "author_usernames" -> project.author_usernames, "updated_at" -> new DateTime())
    projects.update(q = MongoDBObject("guid" -> project.guid),
      o = MongoDBObject("$set" -> obj),
      upsert = false,
      multi = false)
  }

  def findByGuid(guid: String): Option[Project] = {
    search(ProjectQuery(guid = Some(guid))).headOption
  }

  def findByName(name: String): Option[Project] = {
    search(ProjectQuery(name = Some(name))).headOption
  }

  def findByUrlKey(urlKey: String): Option[Project] = {
    search(ProjectQuery(url_key = Some(urlKey))).headOption
  }

  def findByAuthorUsername(authorUsername: String): Iterable[Project] = {
    search(ProjectQuery(author_usernames = Some(Seq(authorUsername))))
  }

  def findByCreatedBy(createdByUsername: String): Iterable[Project] = {
    search(ProjectQuery(created_by = Some(createdByUsername)))
  }

  def findRecentlyUpdated(limit: Int): Iterable[Project] = {
    search(ProjectQuery(limit = Some(limit), order_by = Some("updated_at"), order_direction = -1))
  }

  def delete(guid: String) = {
    // TODO: Soft delete?
    projects.remove(MongoDBObject("guid" -> guid))
  }

  def searchByName(name: String): Option[Project] = {
    val query = ProjectQuery(name = Some(name))
    val builder = MongoDBObject.newBuilder
    query.name.foreach { v => builder += "name" -> MongoDBObject("$regex" -> ("^"+v+"$"), "$options" -> "i") }

    val sortBuilder = MongoDBObject.newBuilder
    query.order_by.foreach { field => sortBuilder += field -> query.order_direction }

    Logger.info("Search Mongo For Project => "+builder.result())

    projects.find(builder.result()).
      skip(query.pagination.offsetOrDefault).
      sort(sortBuilder.result()).
      limit(query.pagination.limitOrDefault).
      toList.map(grater[Project].asObject(_)).headOption
  }

  def search(query: ProjectQuery): Iterable[Project] = {
    val builder = MongoDBObject.newBuilder
    query.guid.foreach { v => builder += "guid" -> v }
    query.name.foreach { v => builder += "name" -> v }
    query.url_key.foreach { v => builder += "url_key" -> v }
    query.author_usernames.foreach { v => builder += "author_usernames" -> MongoDBObject("$in" -> v) }
    query.created_by.foreach { v => builder += ("created_by" -> v) }

    val sortBuilder = MongoDBObject.newBuilder
    query.order_by.foreach { field => sortBuilder += field -> query.order_direction }

    projects.find(builder.result()).
      skip(query.pagination.offsetOrDefault).
      sort(sortBuilder.result()).
      limit(query.pagination.limitOrDefault).
      toList.map(grater[Project].asObject(_))
  }

}
