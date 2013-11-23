package dao

import java.sql.Timestamp

case class User(username: String, email: String, password: String, firstName: Option[String] = None,
                lastName: Option[String] = None, homepage: Option[String] = None,
                joined: Timestamp = new Timestamp(new java.util.Date().getTime),
                salt: Option[String] = None, id: Option[Long] = None)

case class ProjectBranch(branchName: String) extends AnyVal
case class ProjectVersion(versionName: String) extends AnyVal

trait Project {
  val name: String
  val slug: String
  val url: String
  val defaultBranch: ProjectBranch
  val defaultVersion: ProjectVersion
  val created: Timestamp
  val updated: Timestamp
  val id: Option[Long]
}

case class SimpleProject(name: String, slug: String, url: String = "",
                         defaultBranch: ProjectBranch = ProjectHelper.defaultProjectBranch,
                         defaultVersion: ProjectVersion = ProjectHelper.defaultProjectVersion,
                         created: Timestamp = new Timestamp(System.currentTimeMillis),
                         updated: Timestamp = new Timestamp(System.currentTimeMillis),
                         id: Option[Long] = None) extends Project

case class VersionWithProject(projectId: Long, version: String)

case class ProjectAndVersions(project: ProjectWithAuthors, versions: Seq[ProjectVersion])

case class ProjectWithAuthors(name: String, slug: String, url: String, defaultBranch: ProjectBranch,
                              defaultVersion: ProjectVersion, created: Timestamp, updated: Timestamp, id: Option[Long],
                              authors: Seq[User]) extends Project with Ordered[ProjectWithAuthors] {
  def compare(that: ProjectWithAuthors): Int = that.updated.compareTo(this.updated)
}

trait Enum[A] {
  trait Value { self: A =>
    _values :+= this
  }
  private var _values = Seq.empty[A]
  def values = _values
  def apply(name: String): Option[A] = {
    values.find(_.toString.equals(name))
  }
  def unapply(value: A): String = {
    value.toString
  }
}

sealed trait BuildStatus extends BuildStatus.Value
object BuildStatus extends Enum[BuildStatus]
case object BuildSuccess extends BuildStatus
case object BuildFailure extends BuildStatus

case class Build(projectId: Long, version: ProjectVersion, message: String, created: Timestamp,
                 status: BuildStatus = BuildSuccess, startPage: Option[String] = None, id: Option[Long] = None)

case class ProjectAndBuilds(project: ProjectWithAuthors, builds: Seq[Build]) {
  def versions: Seq[ProjectVersion] = builds.map(_.version)
  def latestBuild: Option[Build] = builds.find(_.version == project.defaultVersion)
}

object BuildHelper {
  def apply(result: ((Long, ProjectVersion, String, BuildStatus, Option[String], Long), Option[Timestamp])): Build = {
    val ((projectId, version, msg, status, startPage, id), created) = result
    Build(projectId, version, msg, created.get, status, startPage, Some(id))
  }
}

object ProjectHelper {
  def apply(name: String): SimpleProject = {
    new SimpleProject(name, urlForName(name))
  }
  def urlForName(name: String): String =
    name.toLowerCase.replaceAll(" ", "-").replaceAll("\\.", "").replaceAll("'", "").replaceAll("\"", "")

  lazy val defaultProjectBranch = ProjectBranch("master")
  lazy val defaultProjectVersion = ProjectVersion("latest")
}
