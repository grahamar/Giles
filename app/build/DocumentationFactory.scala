package build

object DocumentationFactory {

  private lazy val docsBuilder =
    new DirectoryHandlerImpl with MarkdownDocsBuilder with GitRepositoryService with LuceneDocsIndexer with LuceneDocsSearcher

  def documentsBuilder: DocsBuilder = {
    docsBuilder
  }

  def repositoryService: RepositoryService = {
    docsBuilder
  }

  def searchService: DocsSearcher = {
    docsBuilder
  }

  def directoryService: DirectoryHandler = {
    docsBuilder
  }

}
