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
}

case class ProjectSearchResult(projectUrlKey: UrlKey, projectVersion: Version, fileUrlKey: UrlKey, fileTitle: String, hits: Seq[String], score: Float) extends Ordered[ProjectSearchResult] {
  def compare(that: ProjectSearchResult) = that.score.compareTo(this.score)
}

trait LuceneDocsSearcher extends DocsSearcher {
  self: DirectoryHandler =>

  import util.ResourceUtil._

  private val LuceneVersion = LucVersion.LUCENE_43

  def search(filter: String): Seq[ProjectSearchResult] = {
    search(filter, "body") ++ search(filter, "title")
  }

  def searchProject(projectUrlKey: String, filter: String): Seq[ProjectSearchResult] = {
    search(filter, "body", projectUrlKey) ++ search(filter, "title", projectUrlKey)
  }

  private def search(filter: String, field: String, projectUrlKey: String): Seq[ProjectSearchResult] = {
    val parser = new QueryParser(LuceneVersion, field, new StandardAnalyzer(LuceneVersion))
    parser.setAllowLeadingWildcard(true)
    val booleanQuery = new BooleanQuery()
    booleanQuery.add(new TermQuery(new Term("project", projectUrlKey)), BooleanClause.Occur.MUST)
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
          Some(ProjectSearchResult(UrlKey.generate(doc.get("project")), new Version(doc.get("version")),
            UrlKey.generate(doc.get("path")), doc.get("title"), hits, result.score))
        } else { None }
      }.flatten.sorted
    }
  }

  private def search(filter: String, field: String): Seq[ProjectSearchResult] = {
    val parser = new QueryParser(LuceneVersion, field, new StandardAnalyzer(LuceneVersion))
    parser.setAllowLeadingWildcard(true)
    val query = parser.parse(filter)
    doWith(DirectoryReader.open(FSDirectory.open(indexDir))) { indexReader =>
      val indexSearcher = new IndexSearcher(indexReader)
      val collector = TopScoreDocCollector.create(1000, true)
      indexSearcher.search(query, collector)
      val results = collector.topDocs().scoreDocs
      val highlighter = new FastVectorHighlighter()
      val fieldQuery = highlighter.getFieldQuery(query)
      results.map { result =>
        val doc = indexSearcher.doc(result.doc)
        val hits = highlighter.getBestFragments(fieldQuery, indexReader, result.doc, field, 200, 5).map { hit =>
          "..."+hit.replaceAll("<b>", "<b class='highlight'>")+"..."
        }
        ProjectSearchResult(UrlKey.generate(doc.get("project")), new Version(doc.get("version")),
          UrlKey.generate(doc.get("path")), doc.get("title"), hits, result.score)
      }.sorted
    }
  }
}