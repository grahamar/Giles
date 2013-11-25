package build

import scala.util.Try
import scala.concurrent.{ExecutionContext, Future}

import dao.{BuildDAO, SimpleProject, User, ProjectDAO}
import play.api.Logger

object DocsBuilderFactory {

  private lazy val docsBuilder =
    new DirectoryHandlerImpl with PamfletDocsBuilder with GitRepositoryService with LuceneDocsIndexer with LuceneDocsSearcher

  def buildInitialProject(project: SimpleProject, currentUser: Option[User])
                        (implicit ex: ExecutionContext): Future[Unit] = Future {
    Try {
      val insertedProject = ProjectDAO.insertProject(project)
      currentUser.map { usr =>
        insertedProject.id.foreach { projId =>
          usr.id.map(userId => ProjectDAO.insertUserProject(userId -> projId))
        }
      }
      ProjectDAO.findBySlug(project.slug).map { persistedProject =>
        repositoryService.clone(project).map { _ =>
          repositoryService.getVersions(project).map { versions =>
            val projectWithVersions = ProjectDAO.insertProjectVersions(persistedProject, versions)
            documentsBuilder.initAndBuildProject(projectWithVersions)
          }
        }
      }
    }.recover {
      case e: Exception =>
        BuildDAO.insertBuildFailure(project, project.defaultVersion, e.getMessage)
    }
  }

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
