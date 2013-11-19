package controllers

import java.io.File
import java.net.JarURLConnection

import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global
import collection.JavaConverters._

import play.api.mvc._
import play.api.{Logger, Mode, Play}
import play.api.Play.current
import play.api.libs.iteratee.Enumerator
import play.api.libs.{MimeTypes, Codecs}

import auth.{OptionalAuthUser, AuthConfigImpl}
import settings.Global
import util.ResourceUtil
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import org.joda.time.DateTimeZone

object ProjectController extends Controller with OptionalAuthUser with AuthConfigImpl {

  private val timeZoneCode = "GMT"

  //Dateformatter is immutable and threadsafe
  private val df: DateTimeFormatter =
    DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss '" + timeZoneCode + "'").withLocale(java.util.Locale.ENGLISH).withZone(DateTimeZone.forID(timeZoneCode))

  //Dateformatter is immutable and threadsafe
  private val dfp: DateTimeFormatter =
    DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss").withLocale(java.util.Locale.ENGLISH).withZone(DateTimeZone.forID(timeZoneCode))

  private val parsableTimezoneCode = " " + timeZoneCode

  // -- ETags handling
  // TODO this is horrible, could get very large!!
  private val etags = new java.util.concurrent.ConcurrentHashMap[String, String]().asScala

  private def etagFor(resource: java.net.URL): Option[String] = {
    etags.get(resource.toExternalForm).filter(_ => Play.isProd).orElse {
      val maybeEtag = lastModifiedFor(resource).map(_ + " -> " + resource.toExternalForm).map("\"" + Codecs.sha1(_) + "\"")
      maybeEtag.foreach(etags.put(resource.toExternalForm, _))
      maybeEtag
    }
  }

  private val lastModifieds = new java.util.concurrent.ConcurrentHashMap[String, String]().asScala

  private def lastModifiedFor(resource: java.net.URL): Option[String] = {
    lastModifieds.get(resource.toExternalForm).filter(_ => Play.isProd).orElse {
      val maybeLastModified = resource.getProtocol match {
        case "file" => Some(df.print({ new java.util.Date(new java.io.File(resource.getPath).lastModified).getTime }))
        case "jar" => {
          resource.getPath.split('!').drop(1).headOption.flatMap { fileNameInJar =>
            Option(resource.openConnection)
              .collect { case c: JarURLConnection => c }
              .flatMap(c => Option(c.getJarFile.getJarEntry(fileNameInJar.drop(1))))
              .map(_.getTime)
              .filterNot(_ == 0)
              .map(lastModified => df.print({ new java.util.Date(lastModified) }.getTime))
          }
        }
        case _ => None
      }
      maybeLastModified.foreach(lastModifieds.put(resource.toExternalForm, _))
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
    val d = dfp.parseDateTime(date.replace(parsableTimezoneCode, "")).toDate
    Some(d)
  } catch {
    case _: Exception => None
  }

  def project(projectSlug: String, projectVersion: String, restOfPath: String) = StackAction { implicit request =>
    val path = Global.configuration.getString("build.dir").getOrElse("./.builds")
    val file = projectSlug + "/" + projectVersion + "/" + restOfPath
    val resourceName = path + "/" + file

    if (new File(resourceName).isDirectory || !new File(resourceName).getCanonicalPath.startsWith(new File(path).getCanonicalPath)) {
      Logger.warn("Resource Not Found. It's either a directory or not found in the builds directory.")
      NotFound
    } else {
      val gzippedResource = ResourceUtil.resource(resourceName + ".gz")

      val resource = {
        gzippedResource.map(_ -> true)
          .filter{_ =>
            request.headers.get(ACCEPT_ENCODING).exists(_.split(',').exists(_ == "gzip" && Play.isProd))
          }.orElse{
          ResourceUtil.resource(resourceName).map(_ -> false)
          }
      }

      resource.map {
        case (url, _) if new File(url.getFile).isDirectory => {
          NotFound
        }

        case (url, isGzipped) => {
          lazy val (length, resourceData) = {
            val stream = url.openStream()
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
              etagFor(url).filter(_ == ifNoneMatch)
            }.map(_ => NotModified).getOrElse {
              request.headers.get(IF_MODIFIED_SINCE).flatMap(parseDate).flatMap { ifModifiedSince =>
                lastModifiedFor(url).flatMap(parseDate).filterNot(lastModified => lastModified.after(ifModifiedSince))
              }.map(_ => NotModified.withHeaders(
                DATE -> df.print({ new java.util.Date }.getTime))).getOrElse {
                // Prepare a streamed response
                val response = SimpleResult(
                  header = ResponseHeader(OK, Map(
                    CONTENT_LENGTH -> length.toString,
                    CONTENT_TYPE -> MimeTypes.forFileName(file).map(m => m + addCharsetIfNeeded(m)).getOrElse(BINARY),
                    DATE -> df.print({ new java.util.Date }.getTime))),
                  body = resourceData)

                // If there is a gzipped version, even if the client isn't accepting gzip, we need to specify the
                // Vary header so proxy servers will cache both the gzip and the non gzipped version
                val gzippedResponse = (gzippedResource.isDefined, isGzipped) match {
                  case (true, true) => response.withHeaders(VARY -> ACCEPT_ENCODING, CONTENT_ENCODING -> "gzip")
                  case (true, false) => response.withHeaders(VARY -> ACCEPT_ENCODING)
                  case _ => response
                }

                // Add Etag if we are able to compute it
                val taggedResponse = etagFor(url).map(etag => gzippedResponse.withHeaders(ETAG -> etag)).getOrElse(gzippedResponse)
                val lastModifiedResponse = lastModifiedFor(url).map(lastModified => taggedResponse.withHeaders(LAST_MODIFIED -> lastModified)).getOrElse(taggedResponse)

                // Add Cache directive if configured
                lastModifiedResponse.withHeaders(CACHE_CONTROL -> {
                  Play.configuration.getString("\"assets.cache." + resourceName + "\"").getOrElse(Play.mode match {
                    case Mode.Prod => Play.configuration.getString("assets.defaultCache").getOrElse("max-age=3600")
                    case _ => "no-cache"
                  })
                })
              }
            }
          }
        }
      }.getOrElse(NotFound)
    }
  }

}
