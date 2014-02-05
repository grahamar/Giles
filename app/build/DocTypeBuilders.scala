package build

import java.io.{File => JFile}
import java.util.UUID

import scala.io.Codec

import models._
import settings.Global
import dao.util.FileHelper

import org.apache.commons.io.{FilenameUtils, FileUtils}
import org.apache.commons.lang3.StringEscapeUtils

sealed trait DocTypeBuilder {
  def supportedFileExtensions: Array[String]
  def buildDocument(project: Project, version: String, document: JFile, filename: String, relativePath: String): Unit
}

trait MultipleDocTypesBuilder extends AbstractDocsBuilder with DocTypeBuilder {
  self: DirectoryHandler with RepositoryService with DocsIndexer with DocTypeBuilder =>

  override def supportedFileExtensions =
    MarkdownDocsBuilder.supportedFileExtensions ++ ReTextDocsBuilder.supportedFileExtensions

  override def buildDocument(project: Project, version: String, document: JFile, filename: String, relativePath: String): Unit = {
    FilenameUtils.getExtension(filename) match {
      case "md" =>
        MarkdownDocsBuilder.buildDocument(project, version, document, filename, relativePath)
      case "markdown" =>
        MarkdownDocsBuilder.buildDocument(project, version, document, filename, relativePath)
      case "rst" =>
        ReTextDocsBuilder.buildDocument(project, version, document, filename, relativePath)
    }
  }

}

object MarkdownDocsBuilder extends DocTypeBuilder {

  import eu.henkelmann.actuarius.ActuariusTransformer

  private val transformer = new ActuariusTransformer()

  override def supportedFileExtensions: Array[String] = Array("md", "markdown")

  private val Header = """<h[1-6]>(.*)</h[1-6]>""".r

  override def buildDocument(project: Project, version: String, document: JFile, filename: String, relativePath: String): Unit = {
    val htmlContent = transformer(FileUtils.readFileToString(document, "UTF-8"))
    val fileTitle = StringEscapeUtils.unescapeXml(Header.findFirstMatchIn(htmlContent).map(_.group(1)).getOrElse(filename))

    FileHelper.getOrCreateContent(htmlContent) { contentGuid =>
      Global.files.create(UUID.randomUUID().toString, project, version, relativePath, filename, fileTitle, contentGuid)
    }
  }

}

object ReTextDocsBuilder extends DocTypeBuilder {

  import laika.api.{Render, Parse}
  import laika.parse.rst.ReStructuredText
  import laika.render.HTML
  import laika.tree.Documents.Document
  import laika.tree.Elements.Text

  override def supportedFileExtensions: Array[String] = Array("rst")

  implicit val codec = Codec.UTF8

  override def buildDocument(project: Project, version: String, document: JFile, filename: String, relativePath: String): Unit = {
    val htmlDoc: Document = Parse as ReStructuredText fromFile document
    val fileTitle = htmlDoc.title.find(_.isInstanceOf[Text]).map(_.asInstanceOf[Text].content).get
    val htmlContent = Render as HTML from htmlDoc toString()

    FileHelper.getOrCreateContent(htmlContent) { contentGuid =>
      Global.files.create(UUID.randomUUID().toString, project, version, relativePath, filename, fileTitle, contentGuid)
    }
  }

}
