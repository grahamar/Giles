package build

object DocumentationFactory {

  private lazy val docsBuilder =
    new MultipleDocTypesBuilder with DirectoryHandlerImpl with GitRepositoryService with LuceneDocsIndexer with LuceneDocsSearcher

  def documentsBuilder: DocsBuilder = {
    docsBuilder
  }

  def repositoryService: RepositoryService = {
    docsBuilder
  }

  def indexService: DocsIndexer = {
    docsBuilder
  }

  def searchService: DocsSearcher = {
    docsBuilder
  }

  def directoryService: DirectoryHandler = {
    docsBuilder
  }

}
