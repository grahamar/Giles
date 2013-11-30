package util

import java.io._
import java.util.zip.{GZIPInputStream, GZIPOutputStream}
import ResourceUtil._

object Compress {
  def apply(decompressed: String): Array[Byte] = {
    doWith(new ByteArrayOutputStream) { bos =>
      doWith(new ObjectOutputStream(new GZIPOutputStream(bos))) { ozbos =>
        ozbos.writeObject(decompressed)
      }
      bos.toByteArray
    }
  }
}

object Decompress {
  def apply(compressed: Array[Byte]): String = {
    doWith(new ByteArrayInputStream(compressed)) { bis =>
      doWith(new ObjectInputStream(new GZIPInputStream(bis))) { ozbis =>
        ozbis.readObject.asInstanceOf[String]
      }
    }
  }
}
