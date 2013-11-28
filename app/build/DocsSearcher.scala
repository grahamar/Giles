package build

import scala.math.Ordered

import models._

import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.util.{Version => LucVersion}
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.search._
import org.apache.lucene.index.{DirectoryReader, Term}
import org.apache.lucene.store.FSDirectory
import org.apache.lucene.search.vectorhighlight.FastVectorHighlighter

trait DocsSearcher {
  def search(filter: String): Seq[ProjectSearchResult]
  def searchProject(projectUrlKey: String, filter: String): Seq[ProjectSearchResult]
  def searchProjectVersion(projectUrlKey: String, projectVersion: String, filter: String): Seq[ProjectSearchResult]
}

case class ProjectSearchResult(projectUrlKey: String, projectVersion: String, fileUrlKey: String, fileTitle: String, hits: Seq[String], score: Float) extends Ordered[ProjectSearchResult] {
  def compare(that: ProjectSearchResult) = that.score.compareTo(this.score)
}

trait LuceneDocsSearcher extends DocsSearcher {
  self: DirectoryHandler =>

  import util.ResourceUtil._

  private val LuceneVersion = LucVersion.LUCENE_43

  def search(filter: String): Seq[ProjectSearchResult] = {
    search(filter, "body") ++
      search(filter, "title")
  }

  def searchProject(projectUrlKey: String, filter: String): Seq[ProjectSearchResult] = {
    search(filter, "body", Some(projectUrlKey)) ++
      search(filter, "title", Some(projectUrlKey))
  }

  def searchProjectVersion(projectUrlKey: String, projectVersion: String, filter: String): Seq[ProjectSearchResult] = {
    search(filter, "body", Some(projectUrlKey), Some(projectVersion)) ++
      search(filter, "title", Some(projectUrlKey), Some(projectVersion))
  }

  private def search(filter: String, field: String, projectUrlKey: Option[String] = None, projectVersion: Option[String] = None): Seq[ProjectSearchResult] = {
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
      results.map { result =>
        val doc = indexSearcher.doc(result.doc)
        val hits = highlighter.getBestFragments(fieldQuery, indexReader, result.doc, field, 200, 5).map { hit =>
          "..."+hit.replaceAll("<b>", "<b class='highlight'>")+"..."
        }
        if(hits.length > 0) {
          Some(ProjectSearchResult(UrlKey.generate(doc.get("project")), doc.get("version"),
            UrlKey.generate(doc.get("path")), doc.get("title"), hits, result.score))
        } else { None }
      }.flatten.sorted
    }
  }

}