package dao.util

import java.util.UUID

import models._
import settings.Global
import util.{Decompress, Compress, HashingUtils}

object FileHelper {
  def getOrCreateContent[T](html: String)(f: String => T) = {
    val contentHash = HashingUtils.uniqueHash(html)
    Global.fileContents.findByHashAndSize(contentHash, html.size) match {
      case Some(existingContent: FileContent) =>
        f(existingContent.guid.toString)
      case None =>
        val content = Global.fileContents.create(UUID.randomUUID().toString, contentHash, html.size, Compress(html))
        f(content.guid.toString)
    }
  }
  def cleanupContent(contentGuid: String): Unit = {
    val references = Global.files.findByContentGuid(contentGuid).size
    if(references == 0) {
      Global.fileContents.delete(contentGuid)
    }
  }
}

object FileConverters {
  implicit class RichFile(file: File) {
    def withContent: FileWithContent = {
      val content = Global.fileContents.findByGuid(file.content_guid).get
      FileWithContent(file, Decompress(content.content))
    }
  }
}

object PublicationConverters {
  implicit class RichPublication(publication: Publication) {
    def withContent: PublicationWithContent = {
      val content = Global.fileContents.findByGuid(publication.content_guid).get
      PublicationWithContent(publication, Decompress(content.content))
    }
  }
}
