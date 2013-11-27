package dao

import models._

import com.novus.salat._
import com.mongodb.casbah.Imports._
import org.joda.time.DateTime
import java.util.UUID

class ProjectDao(projects: MongoCollection) {

  def create(createdByGuid: UUID, guid: UUID, name: String, description: String = "", repoUrl: String, headVersion: String = "HEAD"): Project = {
    val urlKey = UrlKey.generate(name)
    val project = Project(guid = guid,
      name = name,
      description = description,
      url_key = urlKey,
      repo_url = repoUrl,
      keywords = Keywords.generate(Seq(guid.toString, name, urlKey)),
      head_version = headVersion,
      versions = Seq(headVersion),
      author_guids = Seq(createdByGuid),
      created_at = new DateTime(),
      updated_at = new DateTime())
    create(project)
  }

  def create(project: Project): Project = {
    projects.insert(grater[Project].asDBObject(project))
    project
  }

  def update(project: Project) {
    val obj = MongoDBObject("description" -> project.description, "head_version" -> project.head_version)
    projects.update(q = MongoDBObject("guid" -> project.guid),
      o = MongoDBObject("$set" -> obj),
      upsert = false,
      multi = false)
  }

  def findByGuid(guid: UUID): Option[Project] = {
    search(ProjectQuery(guid = Some(guid))).headOption
  }

  def findByName(name: String): Option[Project] = {
    search(ProjectQuery(name = Some(name))).headOption
  }

  def findByUrlKey(urlKey: String): Option[Project] = {
    search(ProjectQuery(url_key = Some(urlKey))).headOption
  }

  def findByAuthorGuid(authorGuid: UUID): Option[Project] = {
    search(ProjectQuery(author_guids = Some(Seq(authorGuid)))).headOption
  }

  def findRecentlyUpdated(limit: Int): Iterable[Project] = {
    search(ProjectQuery(limit = Some(limit), order_by = Some("updated_at"), order_direction = -1))
  }

  def delete(guid: UUID) = {
    // TODO: Soft delete?
    projects.remove(MongoDBObject("guid" -> guid))
  }

  def search(query: ProjectQuery): Iterable[Project] = {
    val builder = MongoDBObject.newBuilder
    query.guid.foreach { v => builder += "guid" -> v }
    query.name.foreach { v => builder += "name" -> v }
    query.url_key.foreach { v => builder += "url_key" -> v }
    query.author_guids.foreach { v => builder += "author_guids" -> v }
    query.query.foreach { v => builder += "keywords" -> v.toLowerCase.r }

    val sortBuilder = MongoDBObject.newBuilder
    query.order_by.foreach { field => sortBuilder += field -> query.order_direction }

    projects.find(builder.result()).
      skip(query.pagination.offsetOrDefault).
      sort(sortBuilder.result()).
      limit(query.pagination.limitOrDefault).
      toList.map(grater[Project].asObject(_))
  }

}
