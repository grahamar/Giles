package util

import java.io.File
import scala.util.Try

object ResourceUtil {

  def doWith[T <: { def close() }, R](toClose: T)(f: T => R): R = {
    try {
      f(toClose)
    } finally {
      toClose.close()
    }
  }

  def resource(name: String): Option[java.net.URL] = {
    val file = new File(Option(name).map {
      case s if s.startsWith("/") => s.drop(1)
      case s => s
    }.get)

    Try {
      Option(file.toURI.toURL)
    }.getOrElse(None)
  }
}
