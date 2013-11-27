package controllers

import play.api.libs.json._
import models._

package object api {
  implicit val guidFormat = Json.format[Guid]
  implicit val urlKeyFormat = Json.format[UrlKey]
  implicit val writeBuildStatus = new Writes[BuildStatus] {
    override def writes(status: BuildStatus): JsValue =
      JsObject(Seq("class" -> JsString(status.toString), "data" -> JsString(status.toString)))
  }
  implicit val readBuildStatus = new Reads[BuildStatus] {
    override def reads(json: JsValue): JsResult[BuildStatus] = json match {
      case JsObject(Seq(("class", JsString(name)), ("data", data))) =>
        name match {
          case "BuildSuccess"  => JsSuccess[BuildStatus](BuildSuccess)
          case "BuildFailure"  => JsSuccess[BuildStatus](BuildFailure)
          case _      => JsError(s"Unknown class '$name'")
        }
      case _ => JsError(s"Unexpected JSON value $json")
    }
  }

  implicit val versionFormat = Json.format[Version]
  implicit val viewFormat = Json.format[View]
  implicit val fileFormat = Json.format[File]
  implicit val projectFormat = Json.format[Project]
  implicit val buildFormat = Json.format[Build]

  implicit def stringToGuidString(str: String): GuidString = new GuidString(str)
  implicit def stringToVersionString(str: String): VersionString = new VersionString(str)
  implicit def stringToUrlKeyString(str: String): UrlKeyString = new UrlKeyString(str)

  class GuidString(str: String) {
    def toGuid: Guid = Guid(str)
  }

  class VersionString(str: String) {
    def toVersion: Version = Version(str)
  }

  class UrlKeyString(str: String) {
    def toUrlKey: UrlKey = UrlKey.generate(str)
  }
}
