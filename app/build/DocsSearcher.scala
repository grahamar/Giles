package build

import scala.math.Ordered

import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.util.Version
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.search._
import org.apache.lucene.index.{DirectoryReader, Term}
import org.apache.lucene.store.FSDirectory
import org.apache.lucene.search.vectorhighlight.FastVectorHighlighter

trait DocsSearcher {
  def search(filter: String): Seq[ProjectSearchResult]
  def searchProject(projectSlug: String, filter: String): Seq[ProjectSearchResult]
}

case class ProjectSearchResult(projectSlug: String, projectVersion: String, path: String, filename: String, hits: Seq[String], score: Float) extends Ordered[ProjectSearchResult] {
  def compare(that: ProjectSearchResult) = that.score.compareTo(this.score)
}

trait LuceneDocsSearcher extends DocsSearcher {
  self: DirectoryHandler =>

  import util.ResourceUtil._

  def search(filter: String): Seq[ProjectSearchResult] = {
    search(filter, "body") ++ search(filter, "title")
  }

  def searchProject(projectSlug: String, filter: String): Seq[ProjectSearchResult] = {
    search(filter, "body", projectSlug) ++ search(filter, "title", projectSlug)
  }

  private def search(filter: String, field: String, projectSlug: String): Seq[ProjectSearchResult] = {
    val parser = new QueryParser(Version.LUCENE_43, field, new StandardAnalyzer(Version.LUCENE_43))
    parser.setAllowLeadingWildcard(true)
    val booleanQuery = new BooleanQuery()
    booleanQuery.add(new TermQuery(new Term("project", projectSlug)), BooleanClause.Occur.MUST)
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
          Some(ProjectSearchResult(doc.get("project"), doc.get("version"),
            encodeFileName(doc.get("path")), doc.get("path"), hits, result.score))
        } else { None }
      }.flatten.sorted
    }
  }

  private def search(filter: String, field: String): Seq[ProjectSearchResult] = {
    val parser = new QueryParser(Version.LUCENE_43, field, new StandardAnalyzer(Version.LUCENE_43))
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
        ProjectSearchResult(doc.get("project"), doc.get("version"),
          encodeFileName(doc.get("path")), doc.get("path"), hits, result.score)
      }.sorted
    }
  }
}