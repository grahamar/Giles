package models

import org.joda.time.DateTime

case class Guid(value: String) extends AnyVal

case class User(guid: Guid,
                username: String,
                email: String,
                password: String,
                project_guids: Seq[Guid],
                first_name: Option[String] = None,
                last_name: Option[String] = None,
                homepage: Option[String] = None,
                created_at: DateTime = new DateTime,
                salt: Option[String] = None)

case class UrlKey(value: String) extends AnyVal
object UrlKey {
  def generate(name: String): UrlKey = {
    new UrlKey(name.toLowerCase.replaceAll(" ", "-").
      replaceAll("\\.", "").replaceAll("'", "").replaceAll("\"", ""))
  }
}
object Keywords {
  def generate(values: Seq[String]): Seq[String] = {
    values.map(_.toLowerCase)
  }
}

case class Project(guid: Guid,
                   name: String,
                   description: String,
                   url_key: UrlKey,
                   repo_url: String,
                   keywords: Seq[String],
                   head_version: Version,
                   versions: Seq[Version],
                   author_guids: Seq[Guid],
                   created_at: DateTime,
                   updated_at: DateTime)

class ProjectAndAuthors(val project: Project, val authors: Seq[User])
class ProjectAuthorsAndBuilds(val project: Project, val authors: Seq[User], val builds: Seq[Build]) {
  def latestBuild: Option[Build] = builds.find(_.version == project.head_version)
}

case class Version(name: String) extends AnyVal

sealed trait BuildStatus extends BuildStatus.Value
object BuildStatus extends Enum[BuildStatus]
case object BuildSuccess extends BuildStatus
case object BuildFailure extends BuildStatus

case class Build(guid: Guid,
                 project_guid: Guid,
                 version: Version,
                 message: String,
                 created_at: DateTime,
                 status: BuildStatus = BuildFailure)

/**
 * An actual file (in HTML) containing some documentation for
 * this project. API is to provide a title and the actual contents
 * for this file.
 */
case class File(guid: Guid,
                project_guid: Guid,
                version: Version,
                title: String,
                url_key: UrlKey,
                keywords: Seq[String],
                html: String,
                created_at: DateTime) extends Ordered[File] {

  // Explicit check for files that should come first (like index or readme)
  override def compare(other: File): Int = {
    if (url_key == other.url_key) {
      0
    } else if ("readme".equals(url_key.value)  || "index".equals(url_key.value)) {
      -1
    } else if ("readme".equals(other.url_key.value) || "index".equals(other.url_key.value)) {
      1
    } else {
      url_key.value.compare(other.url_key.value)
    }
  }

}

case class View(guid: Guid, file_guid: Guid, user_guid: Option[Guid], created_at: DateTime)

case class FileRollup(file_guid: Guid, count: Long)

case class UserFileRollup(user_guid: Guid, file_guid: Guid, count: Long)
