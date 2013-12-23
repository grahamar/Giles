package dao

import java.util.UUID

import models._

import com.novus.salat._
import com.mongodb.casbah.Imports._
import org.joda.time.DateTime

class BuildDao(builds: MongoCollection) {

  def create(guid: String, projectGuid: String, version: String, authors: Seq[String], message: String, status: String): Build = {
    val build = Build(guid = guid,
      project_guid = projectGuid,
      version = version,
      authors = authors,
      message = message,
      status = status,
      created_at = new DateTime())
    builds.insert(grater[Build].asDBObject(build))
    build
  }

  def createSuccess(projectGuid: String, version: String, authors: Seq[String]): Build = {
    create(UUID.randomUUID().toString, projectGuid, version, authors, "", "success")
  }

  def createFailure(projectGuid: String, version: String, message: String): Build = {
    create(UUID.randomUUID().toString, projectGuid, version, Seq.empty, message, "failure")
  }

  def findByGuid(guid: String): Option[Build] = {
    search(BuildQuery(guid = Some(guid))).headOption
  }

  def findLatestByProject(project: Project): Iterable[Build] = {
    val builds = project.versions.flatMap(v => search(BuildQuery(project_guid = Some(project.guid), version = Some(v))))
    val latestBuildByVersion: Map[String, DateTime] = builds.groupBy(_.version).toMap.mapValues{ buildsForVersion =>
      buildsForVersion.map(_.created_at).sorted(Ordering.fromLessThan((ths: DateTime, tht: DateTime) => tht.isBefore(ths))).head
    }

    import _root_.util.Util._

    builds.filter { b =>
      val latestBuildForVersion = latestBuildByVersion.get(b.version)
      b.created_at.equals(latestBuildForVersion.get)
    }.sorted
  }

  def findByProjectGuidAndVersion(projectGuid: String, version: String): Option[Build] = {
    search(BuildQuery(project_guid = Some(projectGuid), version = Some(version))).headOption
  }

  def update(build: Build) = {
    val obj = MongoDBObject("authors" -> build.authors)
    builds.update(q = MongoDBObject("guid" -> build.guid),
      o = MongoDBObject("$set" -> obj), upsert = false, multi = false)
  }

  def delete(guid: String) = {
    // TODO: Soft delete?
    builds.remove(MongoDBObject("guid" -> guid))
  }

  def search(query: BuildQuery): Iterable[Build] = {
    val builder = MongoDBObject.newBuilder
    query.guid.foreach { v => builder += "guid" -> v }
    query.project_guid.foreach { v => builder += "project_guid" -> v }
    query.version.foreach { v => builder += "version" -> v }

    val sortBuilder = MongoDBObject.newBuilder
    sortBuilder += query.order_by.map(_ -> query.order_direction).getOrElse("created_at" -> 1)

    builds.find(builder.result()).
      skip(query.pagination.offsetOrDefault).
      sort(sortBuilder.result()).
      limit(query.pagination.limitOrDefault).
      toList.map(grater[Build].asObject(_))
  }

}
