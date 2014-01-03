package util

import java.io.File
import scala.util.Try
import java.net.{URLEncoder, URLDecoder}
import play.api.Logger

object ResourceUtil {

  import scala.language.reflectiveCalls

  def doWith[T <: { def close() }, R](toClose: T)(f: T => R): R = {
    try {
      f(toClose)
    } finally {
      toClose.close()
    }
  }

  def decodeFileName(filename: String): String =
    URLDecoder.decode(filename, "UTF-8")

  def encodeFileName(filename: String): String =
    URLEncoder.encode(filename, "UTF-8")

  def resource(name: String): Option[File] = {
    val file = new File(Option(decodeFileName(name)).map {
      case s if s.startsWith("/") => s.drop(1)
      case s => s
    }.get)
    Try {
      Option(file)
    }.getOrElse(None)
  }
}
