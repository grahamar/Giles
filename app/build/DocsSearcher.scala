package build

import scala.math.Ordered

import settings.Global
import models._

import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.util.{Version => LucVersion}
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.search._
import org.apache.lucene.index.{DirectoryReader, Term}
import org.apache.lucene.store.FSDirectory
import org.apache.lucene.search.vectorhighlight.FastVectorHighlighter
import org.apache.lucene.document.Document
import util.Util

trait DocsSearcher {
  def searchAllPublications(filter: String): Seq[PublicationSearchResult]
  def searchAllProjects(filter: String): Seq[ProjectSearchResult]
  def searchProject(projectUrlKey: String, filter: String): Seq[ProjectSearchResult]
  def searchProjectVersion(projectUrlKey: String, projectVersion: String, filter: String): Seq[ProjectSearchResult]
}

case class PublicationSearchResult(publicationUrlKey: String, publicationTitle: String, hits: Seq[String], score: Float) extends Ordered[PublicationSearchResult] {
  def compare(that: PublicationSearchResult) = that.score.compareTo(this.score)
}

case class ProjectSearchResult(projectUrlKey: String, projectVersion: String, fileUrlKey: Option[String], fileTitle: String, filename: Option[String], hits: Seq[String], score: Float) extends Ordered[ProjectSearchResult] {
  def compare(that: ProjectSearchResult) = that.score.compareTo(this.score)
}

trait LuceneDocsSearcher extends DocsSearcher {
  self: DirectoryHandler =>

  import util.ResourceUtil._

  private val LuceneVersion = LucVersion.LUCENE_43

  def searchAllProjects(filter: String): Seq[ProjectSearchResult] = {
    (searchProjectName(filter) ++ search(filter, "title")(toProjectResult) ++ search(filter, "body")(toProjectResult)).sorted
  }

  def searchAllPublications(filter: String): Seq[PublicationSearchResult] = {
    (search(filter, "pub-body")(toPublicationResult) ++ search(filter, "pub-title")(toPublicationResult)).sorted
  }

  def searchProject(projectUrlKey: String, filter: String): Seq[ProjectSearchResult] = {
    (search(filter, "title", Some(projectUrlKey))(toProjectResult) ++
      search(filter, "body", Some(projectUrlKey))(toProjectResult)).sorted
  }

  def searchProjectVersion(projectUrlKey: String, projectVersion: String, filter: String): Seq[ProjectSearchResult] = {
    (search(filter, "title", Some(projectUrlKey), Some(projectVersion))(toProjectResult) ++
      search(filter, "body", Some(projectUrlKey), Some(projectVersion))(toProjectResult)).sorted
  }

  private def searchProjectName(projectName: String): Seq[ProjectSearchResult] = {
    Seq(Global.projects.searchByName(projectName).map { proj =>
      ProjectSearchResult(proj.url_key, proj.versions.sorted(Util.VersionOrdering).head, None, proj.name, None, Seq.empty, 1f)
    }).flatten
  }

  private def search[T](filter: String, field: String, projectUrlKey: Option[String] = None,
                        projectVersion: Option[String] = None)(func: ((Document, Array[String], Float)) => T)(implicit ordering: Ordering[T]): Seq[T] = {
    val parser = new QueryParser(LuceneVersion, field, new StandardAnalyzer(LuceneVersion))
    parser.setAllowLeadingWildcard(true)
    val booleanQuery = new BooleanQuery()
    projectUrlKey.map(project => booleanQuery.add(new TermQuery(new Term("project", project)), BooleanClause.Occur.MUST))
    projectVersion.map(version => booleanQuery.add(new TermQuery(new Term("version", version)), BooleanClause.Occur.MUST))
    booleanQuery.add(parser.parse(filter), BooleanClause.Occur.SHOULD)
    doWith(DirectoryReader.open(FSDirectory.open(indexDir))) { indexReader =>
      val indexSearcher = new IndexSearcher(indexReader)
      val collector = TopScoreDocCollector.create(1000, true)
      indexSearcher.search(booleanQuery, collector)
      val results = collector.topDocs().scoreDocs
      val highlighter = new FastVectorHighlighter()
      val fieldQuery = highlighter.getFieldQuery(booleanQuery)
      results.flatMap { result =>
        val doc = indexSearcher.doc(result.doc)
        val hits = highlighter.getBestFragments(fieldQuery, indexReader, result.doc, field, 200, 5).map { hit =>
          "..."+hit.replaceAll("<b>", "<b class='highlight'>")+"..."
        }
        if(hits.length > 0) {
          Some(func((doc, hits, result.score)))
        } else { None }
      }
    }
  }

  private def toProjectResult(result: (Document, Array[String], Float)): ProjectSearchResult = {
    ProjectSearchResult(UrlKey.generateProjectUrlKey(result._1.get("project")), result._1.get("version"),
      Option(UrlKey.generateFileUrlKey(result._1.get("path"))), result._1.get("title"),
      Option(result._1.get("filename")), result._2, result._3)
  }

  private def toPublicationResult(result: (Document, Array[String], Float)): PublicationSearchResult = {
    PublicationSearchResult(UrlKey.generateProjectUrlKey(result._1.get("publication")), result._1.get("pub-title"),
      result._2, result._3)
  }

}