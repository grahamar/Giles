package controllers

import controllers.api.value.ApiResponse
import models._
import java.util.UUID
import play.api.libs.json.Json

import scala.util.Try

package object api {

  implicit class RichUser(user: User) {
    def toJsonUser: JsonUser = {
      JsonUser(user.guid, user.username, user.email, user.project_guids, user.first_name, user.last_name, user.homepage,
        user.created_at)
    }
  }

  def isUUID(value: String): Boolean = Try(UUID.fromString(value)).isSuccess

  implicit val writesApiResponse = Json.writes[ApiResponse]

  implicit val writesBuild = Json.writes[Build]

  implicit val writesFile = Json.writes[File]

  implicit val writesProject = Json.writes[Project]

  implicit val writesJsonUser = Json.writes[JsonUser]

  implicit val writesViewJson = Json.writes[ViewJson]

}
