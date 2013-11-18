package util

import play.api.libs.Crypto

object SessionUtil {
  def verifyHmac(valu: String): Option[String] = {
    val (hmac, value) = valu.splitAt(40)
    if (Crypto.sign(value) == hmac) Some(value) else None
  }
}
