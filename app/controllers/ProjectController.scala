package controllers

import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global

import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.SimpleResult

import views._
import dao._
import auth.{Authenticator, OptionalAuthUser, AuthConfigImpl}
import build.DocsBuilderFactory

object ProjectController extends Controller with OptionalAuthUser with AuthConfigImpl {

  def project(projectSlug: String) = StackAction { implicit request =>
    val maybeUser = loggedIn
    ProjectDAO.findBySlugWithVersions(projectSlug).map { project =>
      maybeUser.foreach { currentUser =>
        if(project.project.authors.contains(currentUser)) {
          Redirect(routes.ProjectController.editProject(projectSlug))
        }
      }
      Ok(html.project(project, Authenticator.loginForm))
    }.getOrElse(NotFound)
  }

  def editProject(projectSlug: String) = StackAction { implicit request =>
    ProjectDAO.findBySlugWithVersions(projectSlug).map { project =>
      Ok(html.project(project, Authenticator.loginForm))
    }.getOrElse(NotFound)
  }

  def pullNewVersions(projectSlug: String) = StackAction { implicit request =>
    ProjectDAO.findBySlugWithVersions(projectSlug).map { project =>
      DocsBuilderFactory.documentsBuilder.update(project.project, project.versions)
      Ok(html.project(project, Authenticator.loginForm))
    }.getOrElse(BadRequest)
  }

  def projects = StackAction { implicit request =>
    Ok(html.projects(ProjectDAO.projectsWithAuthors, Authenticator.loginForm))
  }

  def importProject = StackAction { implicit request =>
    val maybeUser = loggedIn
    maybeUser.map{ usr =>
      Ok(html.importProject(Authenticator.loginForm, importProjectForm))
    }.getOrElse(Application.Home)
  }

  def createProject = AsyncStack { implicit request =>
    importProjectForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(html.importProject(Authenticator.loginForm, formWithErrors))),
      project => {
        projectCreated(project.toSimpleProject)
      }
    )
  }

  def projectCreated(project: SimpleProject)(implicit request: RequestHeader, currentUser: Option[User]): Future[SimpleResult] = {
    val checkForExisting = ProjectDAO.findBySlug(project.slug)
    if(checkForExisting.isEmpty) {
      DocsBuilderFactory.buildInitialProject(project, currentUser)
      Future.successful(Redirect(routes.ProjectController.importProject).
        flashing("success" -> ("Creating project \""+project.name+"\"...")))
    } else {
      Future.successful(Redirect(routes.ProjectController.importProject).
        flashing("failure" -> ("Project \""+project.name+"\" already exists.")))
    }
  }

  val importProjectForm = Form {
    mapping(
      "name" -> nonEmptyText,
      "repo" -> nonEmptyText,
      "default_branch" -> nonEmptyText,
      "default_version" -> nonEmptyText
    )(ProjectImportData.apply)(ProjectImportData.unapply)
  }.fill(DefaultProjectImportData)

}

case class ProjectImportData(name: String, repoUrl: String, defaultBranch: String, defaultVersion: String) {
  def toSimpleProject: SimpleProject = {
    SimpleProject(name, ProjectHelper.urlForName(name), repoUrl, ProjectBranch(defaultBranch), ProjectVersion(defaultVersion))
  }
}

object DefaultProjectImportData extends ProjectImportData("", "", "master", "latest")

