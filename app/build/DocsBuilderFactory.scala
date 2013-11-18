package build

import dao.Project

object DocsBuilderFactory {

  def forProject(project: Project): DocsBuilder with RepositoryService = {
    new PamfletDocsBuilder with GitRepositoryService
  }

}
