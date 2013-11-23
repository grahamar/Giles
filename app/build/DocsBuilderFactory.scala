package build

object DocsBuilderFactory {

  private lazy val docsBuilder =
    new DirectoryHandlerImpl with PamfletDocsBuilder with GitRepositoryService with LuceneDocsIndexer

  def documentsBuilder: DocsBuilder with RepositoryService with DocsIndexer = {
    docsBuilder
  }

  def repositoryService: RepositoryService = {
    docsBuilder
  }

  def searchService: DocsIndexer = {
    docsBuilder
  }

  def directoryService: DirectoryHandler = {
    docsBuilder
  }

}
