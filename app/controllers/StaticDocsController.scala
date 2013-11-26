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

import auth.{AuthConfigImpl, OptionalAuthUser}
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import org.joda.time.DateTimeZone
import build.DocsBuilderFactory
import util.ResourceUtil

object StaticDocsController extends Controller with OptionalAuthUser with AuthConfigImpl {

  private val timeZoneCode = "GMT"

  //Dateformatter is immutable and threadsafe
  private val df: DateTimeFormatter =
    DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss '" + timeZoneCode + "'").withLocale(java.util.Locale.ENGLISH).withZone(DateTimeZone.forID(timeZoneCode))

  //Dateformatter is immutable and threadsafe
  private val dfp: DateTimeFormatter =
    DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss").withLocale(java.util.Locale.ENGLISH).withZone(DateTimeZone.forID(timeZoneCode))

  private val parsableTimezoneCode = " " + timeZoneCode

  // -- ETags handling
  private def etagFor(resource: File): Option[String] = {
    Cache.get(resource.getAbsolutePath).orElse {
      val maybeEtag = lastModifiedFor(resource).map(_ + " -> " + resource.getAbsolutePath).map("\"" + Codecs.sha1(_) + "\"")
      maybeEtag.foreach(Cache.set(resource.getAbsolutePath, _))
      maybeEtag
    }.map(_.toString)
  }

  private val lastModifieds = new java.util.concurrent.ConcurrentHashMap[String, String]().asScala

  private def lastModifiedFor(resource: File): Option[String] = {
    lastModifieds.get(resource.getAbsolutePath).orElse {
      val maybeLastModified = Some(df.print({ new java.util.Date(resource.lastModified).getTime }))
      maybeLastModified.foreach(lastModifieds.put(resource.getAbsolutePath, _))
      maybeLastModified
    }
  }

  private lazy val defaultCharSet = Play.configuration.getString("default.charset").getOrElse("utf-8")

  private def addCharsetIfNeeded(mimeType: String): String =
    if (MimeTypes.isText(mimeType))
      "; charset=" + defaultCharSet
    else ""

  def parseDate(date: String): Option[java.util.Date] = try {
    //jodatime does not parse timezones, so we handle that manually
    Some(dfp.parseDateTime(date.replace(parsableTimezoneCode, "")).toDate)
  } catch {
    case _: Exception => None
  }

  def publish(projectSlug: String, projectVersion: String, restOfPath: String) = TODO

  def projectDocs(projectSlug: String, projectVersion: String, restOfPath: String) = StackAction { implicit request =>
    val path = DocsBuilderFactory.directoryService.buildDirForProjectVersion(projectSlug, projectVersion)
    val file = restOfPath
    val resourceName = path + "/" + file

    if (new File(resourceName).isDirectory || !new File(resourceName).getCanonicalPath.startsWith(path.getCanonicalPath)) {
      Logger.warn("Resource Not Found. It's either a directory or not found in the builds directory.")
      NotFound
    } else {
      ResourceUtil.resource(resourceName).map {
        case fileResource: File if !fileResource.exists() || fileResource.isDirectory => {
          Logger.warn("File not found or is a directory! ["+fileResource.getAbsolutePath+"]")
          NotFound
        }

        case fileResource: File => {
          lazy val (length, resourceData) = {
            val stream = new FileInputStream(fileResource)
            try {
              (stream.available, Enumerator.fromStream(stream))
            } catch {
              case _: Exception => (0, Enumerator[Array[Byte]]())
            }
          }

          if (length == 0) {
            Logger.warn("File found but it's empty...")
            NotFound
          } else {
            request.headers.get(IF_NONE_MATCH).flatMap { ifNoneMatch =>
              etagFor(fileResource).filter(_ == ifNoneMatch)
            }.map(_ => NotModified).getOrElse {
              request.headers.get(IF_MODIFIED_SINCE).flatMap(parseDate).flatMap { ifModifiedSince =>
                lastModifiedFor(fileResource).flatMap(parseDate).filterNot(lastModified => lastModified.after(ifModifiedSince))
              }.map(_ => NotModified.withHeaders(
                DATE -> df.print({ new java.util.Date }.getTime))).getOrElse {
                // Prepare a streamed response
                val response = SimpleResult(
                  header = ResponseHeader(OK, Map(
                    CONTENT_LENGTH -> length.toString,
                    CONTENT_TYPE -> MimeTypes.forFileName(file).map(m => m + addCharsetIfNeeded(m)).getOrElse(BINARY),
                    DATE -> df.print({ new java.util.Date }.getTime))),
                  body = resourceData)

                // Add Etag if we are able to compute it
                val taggedResponse = etagFor(fileResource).map(etag => response.withHeaders(ETAG -> etag)).getOrElse(response)
                val lastModifiedResponse = lastModifiedFor(fileResource).map(lastModified => taggedResponse.withHeaders(LAST_MODIFIED -> lastModified)).getOrElse(taggedResponse)

                // Add Cache directive if configured
                lastModifiedResponse.withHeaders(CACHE_CONTROL -> {
                  Play.configuration.getString("\"assets.cache." + resourceName + "\"").
                    getOrElse(Play.configuration.getString("assets.defaultCache").getOrElse("max-age=3600"))
                })
              }
            }
          }
        }
      }.getOrElse{
        Logger.info("Documentation for ["+projectSlug+"] - Not Found")
        NotFound
      }
    }
  }

}
