package util

import java.security.MessageDigest

object HashingUtils {
  def uniqueHash(content: String): String = {
    sha1(content) + md5(content)
  }
  def md5(content: String): String = {
    MessageDigest.getInstance("MD5").digest(content.getBytes("UTF-8")).map(0xFF.&).map("%02x".format(_)).mkString
  }
  def sha1(content: String): String = {
    MessageDigest.getInstance("SHA1").digest(content.getBytes("UTF-8")).map(0xFF.&).map("%02x".format(_)).mkString
  }
}
