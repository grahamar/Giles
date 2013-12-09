package util

import java.security.MessageDigest
import play.api.libs.Crypto

object HashingUtils {

  def uniqueHash(content: String): String = sha1(content) + md5(content)

  def md5(content: String): String =
    MessageDigest.getInstance("MD5").digest(content.getBytes("UTF-8")).map(0xFF.&).map("%02x".format(_)).mkString

  def sha1(content: String): String =
    MessageDigest.getInstance("SHA1").digest(content.getBytes("UTF-8")).map(0xFF.&).map("%02x".format(_)).mkString

  def calculateUserApiKey(applicationName: String ): String = Crypto.sign("Giles|" + applicationName)

  def checkUserApiKey(apiKey: String, applicationName: String): Boolean = calculateUserApiKey(applicationName) == apiKey

  def verifyHmac(valu: String): Option[String] = {
    val (hmac, value) = valu.splitAt(40)
    if (Crypto.sign(value) == hmac) Some(value) else None
  }
}
