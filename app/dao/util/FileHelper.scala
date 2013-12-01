package dao.util

import java.util.UUID

import settings.Global
import models.{FileWithContent, FileContent}
import util.{Decompress, Compress, HashingUtils}

object FileHelper {
  def getOrCreateContent[T](html: String)(f: UUID => T) = {
    val contentHash = HashingUtils.uniqueHash(html)
    Global.fileContents.findByHashAndSize(contentHash, html.size) match {
      case Some(existingContent: FileContent) =>
        f(existingContent.guid)
      case None =>
        val content = Global.fileContents.create(UUID.randomUUID(), contentHash, html.size, Compress(html))
        f(content.guid)
    }
  }
}

object FileConverters {
  implicit class RichFile(file: models.File) {
    def withContent: FileWithContent = {
      val content = Global.fileContents.findByGuid(file.content_guid).get
      FileWithContent(file, Decompress(content.content))
    }
  }
}
