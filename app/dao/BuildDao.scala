package dao

import models._

import com.novus.salat._
import com.novus.salat.global._
import com.mongodb.casbah.Imports._
import org.joda.time.DateTime
import java.util.UUID

class BuildDao(builds: MongoCollection) {

  def create(guid: UUID, projectGuid: UUID, version: String, message: String, status: BuildStatus): Build = {
    val build = Build(guid = guid,
      project_guid = projectGuid,
      version = version,
      message = message,
      status = status,
      created_at = new DateTime())
    builds.insert(grater[Build].asDBObject(build))
    build
  }

  def createSuccess(projectGuid: UUID, version: String): Build = {
    create(UUID.randomUUID(), projectGuid, version, "", BuildSuccess)
  }

  def createFailure(projectGuid: UUID, version: String, message: String): Build = {
    create(UUID.randomUUID(), projectGuid, version, message, BuildFailure)
  }

  def findByGuid(guid: UUID): Option[Build] = {
    search(BuildQuery(guid = Some(guid))).headOption
  }

  def findLatestByProject(project: Project): Iterable[Build] = {
    search(BuildQuery(project_guid = Some(project.guid))).take(project.versions.size)
  }

  def findByProjectGuidAndVersion(projectGuid: UUID, version: String): Option[Build] = {
    search(BuildQuery(project_guid = Some(projectGuid), version = Some(version))).headOption
  }

  def delete(guid: UUID) = {
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
