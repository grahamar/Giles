package build

import java.io._

import play.api.Logger

import dao.{ProjectVersion, Project}
import util.ResourceUtil

case class ProjectSearchResult(projectSlug: String, projectVersion: String, path: String, score: Float) extends Ordered[ProjectSearchResult] {
  def compare(that: ProjectSearchResult) = that.score.compareTo(this.score)
}

trait DocsIndexer {
  def index(project: Project, version: ProjectVersion): Unit
  def index(project: Project, version: ProjectVersion, projectVersionedBuildDir: File): Unit
  def search(filter: String): Seq[ProjectSearchResult]
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

  def index(project: Project, version: ProjectVersion): Unit = {
    index(project, version, buildDirForProjectVersion(project, version))
  }

  def index(project: Project, version: ProjectVersion, projectVersionedBuildDir: File): Unit = {
    val index: IndexWriter = {
      val dir = FSDirectory.open(indexDir)
      val analyzer = new StandardAnalyzer(Version.LUCENE_43)
      val iwc = new IndexWriterConfig(Version.LUCENE_43, analyzer)
      iwc.setOpenMode(OpenMode.CREATE)
      new IndexWriter(dir, iwc)
    }
    doWith(index) { indx =>
      indexDirectory(project, version, projectVersionedBuildDir, indx)
      indx.commit()
    }
  }

  def search(filter: String): Seq[ProjectSearchResult] = {
    Logger.info("Searching for ["+filter+"]")
    val analyzer = new StandardAnalyzer(Version.LUCENE_43)
    val bodyQuery = new QueryParser(Version.LUCENE_43, "body", analyzer).parse(filter)
    val titleQuery = new QueryParser(Version.LUCENE_43, "title", analyzer).parse(filter)
    doWith(DirectoryReader.open(FSDirectory.open(indexDir))) { indexReader =>
      val indexSearcher = new IndexSearcher(indexReader)
      val results = indexSearcher.search(titleQuery, 1000).scoreDocs ++ indexSearcher.search(bodyQuery, 1000).scoreDocs
      results.map { result =>
        val doc = indexSearcher.doc(result.doc)
        ProjectSearchResult(doc.get("project"), doc.get("version"), doc.get("path"), result.score)
      }.sorted
    }
  }

  private def indexDirectory(project: Project, version: ProjectVersion, dataDir: File, index: IndexWriter): Unit = {
    Logger.info("Indexing Directory ["+dataDir.getAbsoluteFile+"]")
    try {
      for (f <- dataDir.listFiles()) {
        if (f.isDirectory && !skipDirectory(f)) {
          indexDirectory(project, version, f, index)
        } else {
          indexFile(project, version, f, index)
        }
      }
    } catch {
      case ex: Exception => Logger.error("Exception indexing directory ["+dataDir.getAbsoluteFile+"]", ex)
    }
  }

  private def skipDirectory(directory: File): Boolean = {
    skipDirectories.contains(directory.getName)
  }

  private def indexFile(project: Project, version: ProjectVersion, f: File, index: IndexWriter): Unit = {
    try {
      if (!f.isHidden && !f.isDirectory && f.canRead && f.exists() && f.getName.endsWith(fileSuffix)) {
        Logger.debug("Indexing File ["+f.getAbsoluteFile+"]")
        val filePath = f.getAbsolutePath.
          substring(indexDir.getAbsolutePath.length+project.slug.length+version.versionName.length+4)
        ResourceUtil.doWith(new FileInputStream(f)) { stream =>
          index.addDocument(getDocument(project, version, filePath, stream))
        }
      }
    } catch {
      case ex: Exception => Logger.error("Exception indexing file ["+f.getAbsoluteFile+"]", ex)
    }
  }

  private def getDocument(project: Project, version: ProjectVersion, filename: String, is: InputStream): Document = {
    val tidy = new Tidy()
    tidy.setQuiet(true)
    tidy.setShowWarnings(false)

    val root = tidy.parseDOM(is, null)
    val rawDoc = Option(root.getDocumentElement)

    val doc = new org.apache.lucene.document.Document()
    rawDoc.flatMap(getBody).foreach( body => doc.add(new TextField("body", new StringReader(body))))
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
