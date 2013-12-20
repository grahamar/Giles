package models

import org.joda.time.DateTime
import util.ResourceUtil

case class User(guid: String,
                username: String,
                email: String,
                password: String,
                project_guids: Seq[String],
                first_name: Option[String] = None,
                last_name: Option[String] = None,
                homepage: Option[String] = None,
                created_at: DateTime = new DateTime,
                salt: Option[String] = None)

case class JsonUser(guid: String,
                    username: String,
                    email: String,
                    project_guids: Seq[String],
                    first_name: Option[String] = None,
                    last_name: Option[String] = None,
                    homepage: Option[String] = None,
                    created_at: DateTime = new DateTime)

case class ApiKey(guid: String, user_guid: String, application_name: String, api_key: String)

object UrlKey {
  def generate(name: String): String = {
    name.toLowerCase.trim.replaceAll("""\s+""", "-").replaceAll("""\.+""", "").
      split("/").map(ResourceUtil.encodeFileName).mkString("/")
  }
}

case class Favourite(guid: String,
                     user_guid: String,
                     project_guid: String)

case class Project(guid: String,
                   name: String,
                   description: String,
                   url_key: String,
                   repo_url: String,
                   head_version: String,
                   versions: Seq[String],
                   author_usernames: Seq[String],
                   created_by: String,
                   created_at: DateTime,
                   updated_at: DateTime)

class ProjectAndAuthors(val project: Project, val authors: Seq[User])
class ProjectAuthorsAndBuilds(val project: Project, val authors: Seq[User], val builds: Seq[Build]) {
  def latestBuild: Option[Build] = builds.find(_.version == project.head_version).map(Some.apply).getOrElse(builds.headOption)
}

case class Build(guid: String,
                 project_guid: String,
                 version: String,
                 authors: Seq[String],
                 message: String,
                 created_at: DateTime,
                 status: String = "failure")

/**
 * An actual file (in HTML) containing some documentation for
 * this project. API is to provide a title and the actual contents
 * for this file.
 */
case class File(guid: String,
                project_guid: String,
                version: String,
                title: String,
                filename: String,
                relative_path: String,
                url_key: String,
                content_guid: String,
                created_at: DateTime) extends Ordered[File] {

  // Explicit check for files that should come first (like index or readme)
  override def compare(other: File): Int = {
    if (filename == other.filename) {
      0
    } else if (filename.toLowerCase.startsWith("readme")  || filename.toLowerCase.startsWith("index")) {
      -1
    } else if (other.filename.toLowerCase.startsWith("readme") || other.filename.toLowerCase.startsWith("index")) {
      1
    } else {
      url_key.compare(other.url_key)
    }
  }

}

case class Publication(guid: String, user_guid: String, title: String, url_key: String, content_guid: String, created_at: DateTime)

case class SwaggerApiFile(guid: String, project_guid: String, version: String, listing: Boolean, path: String, content_guid: String, created_at: DateTime)

case class FileContent(guid: String, hash_key: String, content_size: Long, content: Array[Byte])

case class FileWithContent(file: File, content: String)

case class PublicationWithContent(publication: Publication, content: String)

case class View(guid: String, file_guid: String, user_guid: Option[String], created_at: DateTime)

case class FileRollup(file_guid: String, count: Long)

case class PopularFile(file: File, project: Project)

case class UserFileRollup(user_guid: String, file_guid: String, count: Long)
