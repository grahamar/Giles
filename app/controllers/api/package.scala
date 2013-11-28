package controllers

import play.api.libs.json._
import models._
import java.util.UUID

package object api {
  implicit val writeUUID = new Writes[UUID] {
    override def writes(status: UUID): JsValue =
      JsObject(Seq("class" -> JsString(status.toString), "data" -> JsString(status.toString)))
  }
  implicit val readUUID = new Reads[UUID] {
    override def reads(json: JsValue): JsResult[UUID] = json match {
      case JsObject(Seq(("class", JsString(name)), ("data", JsString(data)))) =>
        name match {
          case "BuildSuccess"  => JsSuccess[UUID](UUID.fromString(data))
          case _      => JsError(s"Unknown class '$name'")
        }
      case _ => JsError(s"Unexpected JSON value $json")
    }
  }
  implicit val userFormat = Json.format[User]
  implicit val viewFormat = Json.format[View]
  implicit val fileFormat = Json.format[File]
  implicit val projectFormat = Json.format[Project]
  implicit val buildFormat = Json.format[Build]

}
