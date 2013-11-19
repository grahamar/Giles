package build

import dao.Project

object DocsBuilderFactory {

  lazy val docsBuilder =
    new DirectoryHandlerImpl with PamfletDocsBuilder with GitRepositoryService with LuceneDocsIndexer

  def forProject(project: Project): DocsBuilder with RepositoryService = {
    docsBuilder
  }

  def documentsBuilder: DocsBuilder = {
    docsBuilder
  }

  def repositoryService: RepositoryService = {
    docsBuilder
  }

  def forSearching: DocsIndexer = {
    docsBuilder
  }

}
