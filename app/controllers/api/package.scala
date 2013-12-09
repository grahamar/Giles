package controllers

import models._
import java.util.UUID
import scala.util.Try

package object api {

  implicit val formats = org.json4s.DefaultFormats

  implicit class RichUser(user: User) {
    def toJsonUser: JsonUser = {
      JsonUser(user.guid, user.username, user.email, user.project_guids, user.first_name, user.last_name, user.homepage,
        user.created_at)
    }
  }

  def isUUID(value: String): Boolean = Try(UUID.fromString(value)).isSuccess
}
