package dao

import models._

import com.novus.salat._
import com.novus.salat.global._
import com.mongodb.casbah.Imports._
import org.joda.time.DateTime
import java.util.UUID

class BuildDao(builds: MongoCollection) {

  def create(guid: Guid, projectGuid: Guid, version: Version, message: String, status: BuildStatus): Build = {
    val build = Build(guid = guid,
      project_guid = projectGuid,
      version = version,
      message = message,
      status = status,
      created_at = new DateTime())
    builds.insert(grater[Build].asDBObject(build))
    build
  }

  def createSuccess(projectGuid: Guid, version: Version): Build = {
    create(new Guid(UUID.randomUUID().toString), projectGuid, version, "", BuildSuccess)
  }

  def createFailure(projectGuid: Guid, version: Version, message: String): Build = {
    create(new Guid(UUID.randomUUID().toString), projectGuid, version, message, BuildFailure)
  }

  def findByGuid(guid: Guid): Option[Build] = {
    search(BuildQuery(guid = Some(guid))).headOption
  }

  def findLatestByProject(project: Project): Iterable[Build] = {
    search(BuildQuery(project_guid = Some(project.guid))).take(project.versions.size)
  }

  def findByProjectGuidAndVersion(projectGuid: Guid, version: Version): Option[Build] = {
    search(BuildQuery(project_guid = Some(projectGuid), version = Some(version))).headOption
  }

  def delete(guid: Guid) = {
    // TODO: Soft delete?
    builds.remove(MongoDBObject("guid" -> guid.value))
  }

  def search(query: BuildQuery): Iterable[Build] = {
    val builder = MongoDBObject.newBuilder
    query.guid.foreach { v => builder += "guid" -> v.value }
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
