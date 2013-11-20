package util

import java.io.File
import scala.util.Try
import java.net.URLDecoder
import play.api.Logger

object ResourceUtil {

  def doWith[T <: { def close() }, R](toClose: T)(f: T => R): R = {
    try {
      f(toClose)
    } finally {
      toClose.close()
    }
  }

  def resource(name: String): Option[File] = {
    val file = new File(Option(URLDecoder.decode(name, "UTF-8").replace(" ", "+")).map {
      case s if s.startsWith("/") => s.drop(1)
      case s => s
    }.get)
    Try {
      Option(file)
    }.getOrElse(None)
  }
}
