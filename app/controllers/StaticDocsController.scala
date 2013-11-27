package controllers

import java.io.{FileInputStream, File}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.JavaConverters._
import play.api.mvc.{ResponseHeader, SimpleResult, Controller}
import play.api.cache.Cache
import play.api.libs.{MimeTypes, Codecs}
import play.api.{Logger, Play}
import play.api.Play.current
import play.api.libs.iteratee.Enumerator

import controllers.auth.{AuthConfigImpl, OptionalAuthUser}
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import org.joda.time.DateTimeZone
import build.DocumentationFactory
import util.ResourceUtil

object StaticDocsController extends Controller with OptionalAuthUser with AuthConfigImpl {

  def publish(projectSlug: String, projectVersion: String, restOfPath: String) = TODO

  def projectIndex(projectSlug: String, projectVersion: String) = TODO

  def projectDocs(projectSlug: String, projectVersion: String, restOfPath: String) = TODO

}
