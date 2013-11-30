package util

import java.security.MessageDigest

object GravatarHelper {
  private lazy val secureGravatarBaseUrl = "https://secure.gravatar.com/avatar/"

  import DefaultGravatars._

  def urlForEmail(email: String, size: Int = 184, defaultGravatar: DefaultGravatar = Retro): String = {
    val urlBuilder = new StringBuilder(secureGravatarBaseUrl)
    urlBuilder ++= HashingUtils.md5(email)
    urlBuilder ++= "?s="
    urlBuilder ++= size.toString
    urlBuilder ++= "&d="
    urlBuilder ++= defaultGravatar.toString
    urlBuilder ++= "&r=pg"
    urlBuilder.toString()
  }
}

sealed abstract class DefaultGravatar(code: String) {
  override def toString: String = code
}
object DefaultGravatars {
  case object FileNotFound extends DefaultGravatar("404")
  case object MysteryMan extends DefaultGravatar("mm")
  case object Identicon extends DefaultGravatar("identicon")
  case object MonsterId extends DefaultGravatar("monsterid")
  case object Wavatar extends DefaultGravatar("wavatar")
  case object Retro extends DefaultGravatar("retro")
  case object Blank extends DefaultGravatar("blank")
}
