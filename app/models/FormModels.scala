package models

import java.util.UUID
import org.joda.time.DateTime

case class UserData(username: String, email: String, password: String, rePassword: String,
                    firstName: String, lastName: String, homepage: String) {
  def toUser: User = {
    User(guid = UUID.randomUUID().toString,
      username = username,
      email = email,
      password = password,
      project_guids = Seq.empty,
      first_name = Option(firstName),
      last_name = Option(lastName),
      created_at = new DateTime())
  }
}

case class ApiKeyData(application_name: String)

case class PublicationData(title: String, content: String)

case class ProjectImportData(name: String, description: String, repoUrl: String, headVersion: String)

case class SwaggerImportData(project: String, version: String, json: String)

object DefaultProjectImportData extends ProjectImportData("", "", "", "HEAD")
