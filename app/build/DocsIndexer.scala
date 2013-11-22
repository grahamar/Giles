package build

import java.io._
import java.net.URLEncoder

import scala.math._
import scala.util.Try
import play.api.Logger

import dao.{ProjectVersion, Project}
import util.ResourceUtil
import org.apache.lucene.search.{BooleanClause, BooleanQuery, TermQuery, TopScoreDocCollector}
import org.apache.lucene.index.FieldInfo.IndexOptions
import org.apache.lucene.search.vectorhighlight.FastVectorHighlighter
import org.apache.lucene.index.Term

case class ProjectSearchResult(projectSlug: String, projectVersion: String, path: String, filename: String, hits: Seq[String], score: Float) extends Ordered[ProjectSearchResult] {
  def compare(that: ProjectSearchResult) = that.score.compareTo(this.score)
}

trait DocsIndexer {
  def index(project: Project, version: ProjectVersion): Try[Unit]
  def index(project: Project, version: ProjectVersion, projectVersionedBuildDir: File): Try[Unit]
  def search(filter: String): Seq[ProjectSearchResult]
  def removeExistingProjectAndVersionIndex(project: Project, version: ProjectVersion): Try[Unit]
}

trait LuceneDocsIndexer extends DocsIndexer {
  self: DirectoryHandler =>

  import org.apache.lucene.document._
  import org.apache.lucene.index.{IndexWriter, IndexWriterConfig, DirectoryReader}
  import org.apache.lucene.search.IndexSearcher
  import org.w3c.tidy.Tidy
  import org.w3c.dom.{NodeList, Node, Text, Element}
  import org.apache.lucene.document.Field.Store
  import org.apache.lucene.util.Version
  import org.apache.lucene.analysis.standard.StandardAnalyzer
  import org.apache.lucene.index.IndexWriterConfig.OpenMode
  import org.apache.lucene.store.FSDirectory
  import org.apache.lucene.queryparser.classic.QueryParser

  import ResourceUtil._

  private val fileSuffix = ".html"
  private val skipDirectories = Seq("css", "img", "js", "offline")

  def index(project: Project, version: ProjectVersion): Try[Unit] = {
    index(project, version, buildDirForProjectVersion(project, version))
  }

  def index(project: Project, version: ProjectVersion, projectVersionedBuildDir: File): Try[Unit] = Try {
    val index: IndexWriter = {
      val dir = FSDirectory.open(indexDir)
      val iwc = new IndexWriterConfig(Version.LUCENE_43, new StandardAnalyzer(Version.LUCENE_43))
      iwc.setOpenMode(OpenMode.CREATE_OR_APPEND)
      new IndexWriter(dir, iwc)
    }
    doWith(index) { indx =>
      indexDirectory(project, version, projectVersionedBuildDir, indx)
      indx.commit()
      Logger.info("Total indexes: "+indx.numDocs())
    }
  }

  def search(filter: String): Seq[ProjectSearchResult] = {
    search(filter, "body") ++ search(filter, "title")
  }

  private def search(filter: String, field: String): Seq[ProjectSearchResult] = {
    val analyzer = new StandardAnalyzer(Version.LUCENE_43)
    val highlighter = new FastVectorHighlighter()
    val parser = new QueryParser(Version.LUCENE_43, field, analyzer)
    parser.setAllowLeadingWildcard(true)
    val query = parser.parse(filter)
    doWith(DirectoryReader.open(FSDirectory.open(indexDir))) { indexReader =>
      Logger.info("Total indexes: "+indexReader.numDocs())
      val indexSearcher = new IndexSearcher(indexReader)
      val collector = TopScoreDocCollector.create(1000, true)
      indexSearcher.search(query, collector)
      val results = collector.topDocs().scoreDocs
      val fieldQuery = highlighter.getFieldQuery(query)
      results.map { result =>
        val doc = indexSearcher.doc(result.doc)
        val hits = highlighter.getBestFragments(fieldQuery, indexReader, result.doc, field, 200, 5).map { hit =>
          "..."+hit.replaceAll("<b>", "<b class='highlight'>")+"..."
        }
        ProjectSearchResult(doc.get("project"), doc.get("version"),
          URLEncoder.encode(doc.get("path"), "UTF-8"), doc.get("path"), hits, result.score)
      }.sorted
    }
  }

  def removeExistingProjectAndVersionIndex(project: Project, version: ProjectVersion): Try[Unit] = Try {
    val indexWriter: IndexWriter = {
      val dir = FSDirectory.open(indexDir)
      val iwc = new IndexWriterConfig(Version.LUCENE_43, new StandardAnalyzer(Version.LUCENE_43))
      iwc.setOpenMode(OpenMode.APPEND)
      new IndexWriter(dir, iwc)
    }

    val booleanQuery = new BooleanQuery()
    booleanQuery.add(new TermQuery(new Term("project", project.slug)), BooleanClause.Occur.MUST)
    booleanQuery.add(new TermQuery(new Term("version", version.versionName)), BooleanClause.Occur.MUST)
    doWith(indexWriter) { writer =>
      writer.deleteDocuments(booleanQuery)
      writer.commit()
    }
  }

  private def indexDirectory(project: Project, version: ProjectVersion, dataDir: File, index: IndexWriter): Unit = {
    Logger.debug("Indexing Directory ["+dataDir.getAbsoluteFile+"]")
    for (f <- dataDir.listFiles()) {
      if (f.isDirectory && !skipDirectory(f)) {
        indexDirectory(project, version, f, index)
      } else {
        indexFile(project, version, f, index)
      }
    }
  }

  private def skipDirectory(directory: File): Boolean = {
    skipDirectories.contains(directory.getName)
  }

  private def indexFile(project: Project, version: ProjectVersion, f: File, index: IndexWriter): Unit = {
    if (!f.isHidden && !f.isDirectory && f.canRead && f.exists() && f.getName.endsWith(fileSuffix)) {
      val filePath = f.getAbsolutePath.
        substring(indexDir.getAbsolutePath.length+project.slug.length+version.versionName.length+4)
      ResourceUtil.doWith(new InputStreamReader(new FileInputStream(f), "UTF-8")) { stream =>
        index.addDocument(getDocument(project, version, filePath, stream))
      }
    }
  }

  private def getDocument(project: Project, version: ProjectVersion, filename: String, is: InputStreamReader): Document = {
    val tidy = new Tidy()
    tidy.setQuiet(true)
    tidy.setShowWarnings(false)

    val root = tidy.parseDOM(is, null)
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
    }
    rawDoc.flatMap(getTitle).foreach { title =>
      doc.add(new StringField("title", title, Store.YES))
      doc.add(new StringField("path", filename, Store.YES))
      doc.add(new StringField("project", project.slug, Store.YES))
      doc.add(new StringField("version", version.versionName, Store.YES))
    }
    doc
  }

  private def getTitle(rawDoc: Element): Option[String] = {
    rawDoc.getElementsByTagName("title").toSeq.headOption.map{ titleElement =>
      Option(titleElement.getFirstChild).map(_.asInstanceOf[Text].getData)
    }.flatten
  }

  private def getBody(rawDoc: Element): Option[String] = {
    rawDoc.getElementsByTagName("body").toSeq.headOption.map(getText)
  }

  private def getText(node: Node): String = {
    val children: Iterator[Node] = node.getChildNodes
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

  private implicit def nodeListToIterable(nodeList: NodeList): Iterator[Node] = {
    Iterator.tabulate(nodeList.getLength)(nodeList.item)
  }

}
