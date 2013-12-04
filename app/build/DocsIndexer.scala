package build

import java.io.{Reader, StringReader}

import scala.util.Try
import scala.collection.JavaConverters._
import play.api.Logger

import models._
import settings.Global
import util.ResourceUtil
import org.apache.lucene.search.{BooleanClause, BooleanQuery, TermQuery}
import org.apache.lucene.index.FieldInfo.IndexOptions
import org.apache.lucene.index.Term
import org.apache.lucene.document._
import org.apache.lucene.index.{IndexWriter, IndexWriterConfig}
import org.apache.lucene.document.Field.Store
import org.apache.lucene.util.{Version => LucVersion}
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.index.IndexWriterConfig.OpenMode
import org.apache.lucene.store.FSDirectory
import org.w3c.tidy.Tidy
import org.w3c.dom.{NodeList, Node, Text, Element}
import org.apache.lucene.analysis.util.CharArraySet

trait DocsIndexer {
  def index(project: Project, version: String): Try[Unit]
  def index(publication: PublicationWithContent): Try[Unit]
  def cleanPublicationIndex(publication: Publication): Try[Unit]
  def cleanProjectAndVersionIndex(project: Project, version: String): Try[Unit]
}

object LuceneDocsIndexer {
  private val LuceneVersion = LucVersion.LUCENE_43
  private def indexWriterConfig: IndexWriterConfig = {
    val iwc = new IndexWriterConfig(LuceneDocsIndexer.LuceneVersion, new StandardAnalyzer(LuceneDocsIndexer.LuceneVersion))
    iwc.setOpenMode(OpenMode.CREATE_OR_APPEND)
    iwc
  }
}

trait LuceneDocsIndexer extends DocsIndexer {
  self: DirectoryHandler =>

  import ResourceUtil._

  def cleanProjectAndVersionIndex(project: Project, version: String): Try[Unit] = Try {
    val indexWriter: IndexWriter = {
      val dir = FSDirectory.open(indexDir)
      new IndexWriter(dir, LuceneDocsIndexer.indexWriterConfig)
    }

    val booleanQuery = new BooleanQuery()
    booleanQuery.add(new TermQuery(new Term("project", project.url_key)), BooleanClause.Occur.MUST)
    booleanQuery.add(new TermQuery(new Term("version", version)), BooleanClause.Occur.MUST)
    doWith(indexWriter) { writer =>
      writer.deleteDocuments(booleanQuery)
      writer.commit()
    }
  }.recover {
    case e: Exception =>
      Global.builds.createFailure(project.guid, version, "Cleaning Index failed - "+ e.getMessage)
      throw e
  }

  def cleanPublicationIndex(publication: Publication): Try[Unit] = Try {
    val indexWriter: IndexWriter = {
      val dir = FSDirectory.open(indexDir)
      new IndexWriter(dir, LuceneDocsIndexer.indexWriterConfig)
    }

    val booleanQuery = new BooleanQuery()
    booleanQuery.add(new TermQuery(new Term("publication", publication.url_key)), BooleanClause.Occur.MUST)
    doWith(indexWriter) { writer =>
      writer.deleteDocuments(booleanQuery)
      writer.commit()
    }
  }

  def index(project: Project, version: String): Try[Unit] = {
    cleanProjectAndVersionIndex(project, version).map { _ =>
      val index: IndexWriter = {
        val dir = FSDirectory.open(indexDir)
        new IndexWriter(dir, LuceneDocsIndexer.indexWriterConfig)
      }
      doWith(index) { indx =>
        indexProject(project, version, indx)
        indx.commit()
      }
    }.recover {
      case e: Exception =>
        Global.builds.createFailure(project.guid, version, "Index failed - "+ e.getMessage)
        throw e
    }
  }

  def index(publication: PublicationWithContent): Try[Unit] = Try {
    val index: IndexWriter = {
      val dir = FSDirectory.open(indexDir)
      new IndexWriter(dir, LuceneDocsIndexer.indexWriterConfig)
    }
    doWith(index) { indx =>
      val tidy = new Tidy()
      tidy.setQuiet(true)
      tidy.setShowWarnings(false)

      ResourceUtil.doWith(new StringReader(publication.content)) { stream =>
        val root = tidy.parseDOM(stream, null)
        val rawDoc = Option(root.getDocumentElement)

        val doc = new org.apache.lucene.document.Document()
        rawDoc.flatMap(getBody).foreach { body =>
          val fieldType = new FieldType()
          fieldType.setIndexed(true)
          fieldType.setStored(true)
          fieldType.setStoreTermVectors(true)
          fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS)
          fieldType.setStoreTermVectorOffsets(true)
          fieldType.setStoreTermVectorPayloads(true)
          fieldType.setStoreTermVectorPositions(true)
          fieldType.setTokenized(true)
          doc.add(new Field("pub-body", body, fieldType))

          doc.add(new StringField("pub-title",rawDoc.flatMap(getTitle).getOrElse(publication.publication.title), Store.YES))
          doc.add(new StringField("publication", publication.publication.url_key, Store.YES))
        }
      }
      indx.commit()
    }
  }

  private def indexProject(project: Project, version: String, index: IndexWriter): Unit = {
    Logger.debug("Indexing Project ["+project.name+"]")

    import dao.util.FileConverters._

    Global.files.findAllByProjectGuidAndVersion(project.guid, version).
    map(_.withContent).foreach { file =>
      indexFile(project, version, file, index)
    }
  }

  private def indexFile(project: Project, version: String, file: FileWithContent, index: IndexWriter): Unit = {
    ResourceUtil.doWith(new StringReader(file.content)) { stream =>
      index.addDocument(getDocument(project, version, file, stream))
    }
  }

  private def getDocument(project: Project, version: String, file: FileWithContent, html: Reader): Document = {
    val tidy = new Tidy()
    tidy.setQuiet(true)
    tidy.setShowWarnings(false)

    val root = tidy.parseDOM(html, null)
    val rawDoc = Option(root.getDocumentElement)

    val doc = new org.apache.lucene.document.Document()
    rawDoc.flatMap(getBody).foreach { body =>
      val fieldType = new FieldType()
      fieldType.setIndexed(true)
      fieldType.setStored(true)
      fieldType.setStoreTermVectors(true)
      fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS)
      fieldType.setStoreTermVectorOffsets(true)
      fieldType.setStoreTermVectorPayloads(true)
      fieldType.setStoreTermVectorPositions(true)
      fieldType.setTokenized(true)
      doc.add(new Field("body", body, fieldType))

      doc.add(new StringField("title",rawDoc.flatMap(getTitle).getOrElse(file.file.title), Store.YES))
      doc.add(new StringField("filename", file.file.filename, Store.YES))
      doc.add(new StringField("path", file.file.url_key, Store.YES))
      doc.add(new StringField("project", project.url_key, Store.YES))
      doc.add(new StringField("version", version, Store.YES))
    }

    doc
  }

  private def getTitle(rawDoc: Element): Option[String] = {
    rawDoc.getElementsByTagName("title").iter.toSeq.headOption.map{ titleElement =>
      Option(titleElement.getFirstChild).map(_.asInstanceOf[Text].getData)
    }.flatten
  }

  private def getBody(rawDoc: Element): Option[String] = {
    rawDoc.getElementsByTagName("body").iter.toSeq.headOption.map(getText)
  }

  private def getText(node: Node): String = {
    val children: Iterator[Node] = node.getChildNodes.iter
    val sb = new StringBuffer()
    for(child <- children) {
      child.getNodeType match {
        case Node.ELEMENT_NODE =>
          sb.append(getText(child))
          sb.append(" ")

        case Node.TEXT_NODE =>
          sb.append(child.asInstanceOf[Text].getData)
      }
    }
    sb.toString
  }

  private implicit class RichNodeList(nodeList: NodeList) {
    def iter: Iterator[Node] = Iterator.tabulate(nodeList.getLength)(nodeList.item)
  }

}
