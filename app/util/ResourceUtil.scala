package util

import java.io.{FileOutputStream, BufferedInputStream, File, FileInputStream}
import java.net.{URLDecoder, URLEncoder}
import java.util.zip.{ZipEntry, ZipOutputStream}

import scala.util.Try
import scala.collection.JavaConverters._

import org.apache.commons.compress.archivers.zip.ZipFile
import org.apache.commons.io.IOUtils

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

  /**
   * Creates a file in the default temporary directory, calls `action` with the file, deletes the file, and returns the result of calling `action`.
   * The name of the file will begin with `prefix`, which must be at least three characters long, and end with `postfix`, which has no minimum length.
   */
  def withTemporaryFile[T](prefix: String, postfix: String)(action: File => T): T = {
    val file = File.createTempFile(prefix, postfix)
    try { action(file) }
    finally { file.delete() }
  }

  def compressDirectory(srcFile: File, zos: ZipOutputStream): Unit = {
    srcFile.listFiles().foreach { file =>
      if(file.isDirectory) {
        compressDirectory(file, zos)
      } else {
        doWith(new BufferedInputStream(new FileInputStream(file))) { fis =>
          zos.putNextEntry(new ZipEntry(file.getName))
          IOUtils.copy(fis, zos)
          zos.closeEntry()
        }
      }
    }
  }

  def extractDirectory(srcZipFile: File, destFile: File) = {
    doWith(new ZipFile(srcZipFile)) { zipFile =>
      zipFile.getEntries.asScala.foreach { entry =>
        val destinationPath = new File(destFile, entry.getName)
        destinationPath.getParentFile.mkdirs()
        if (!entry.isDirectory) {
          doWith(new BufferedInputStream(zipFile.getInputStream(entry))) { inStream =>
            doWith(new FileOutputStream(destinationPath)) { outStream =>
              IOUtils.copy(inStream, outStream)
            }
          }
        }
      }
    }
  }

}
