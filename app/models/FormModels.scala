package models

import java.util.UUID
import org.joda.time.DateTime

case class UserData(username: String, email: String, password: String, rePassword: String,
                    firstName: String, lastName: String, homepage: String) {
  def toUser: User = {
    User(guid = new Guid(UUID.randomUUID().toString),
      username = username,
      email = email,
      password = password,
      project_guids = Seq.empty,
      first_name = Option(firstName),
      last_name = Option(lastName),
      created_at = new DateTime())
  }
}

case class ProjectImportData(name: String, description: String, repoUrl: String, headVersion: String)

object DefaultProjectImportData extends ProjectImportData("", "", "", "HEAD")
