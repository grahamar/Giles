package controllers

import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

import views._
import dao._
import auth.{OptionalAuthUser, Authenticator, AuthConfigImpl}
import build.{BuildFailedException, ProjectSearchResult, DocsBuilderFactory}
import play.api.Logger

/**
 * Manage a database of computers
 */
object Application extends Controller with OptionalAuthUser with AuthConfigImpl {

  /**
   * This result directly redirect to the application home.
   */
  val Home = Redirect(routes.Application.index)
  val Dashboard = Redirect(routes.Application.dashboard)

  // -- Actions

  /**
   * Handle default path requests, redirect to computers list
   */
  def index = StackAction { implicit request =>
    val recentlyUpgradedProjects = ProjectDAO.recentlyUpdatedProjectsWithAuthors(10)
    Ok(html.index(recentlyUpgradedProjects, Authenticator.loginForm))
  }

  def projects = StackAction { implicit request =>
    Ok(html.projects(ProjectDAO.projectsWithAuthors, Authenticator.loginForm))
  }

  def profile(username: String) = StackAction { implicit request =>
    val user = UserDAO.userForUsername(username)
    user.map{usr =>
      val projects = ProjectDAO.projectsForUser(usr)
      Ok(html.profile(usr, projects, Authenticator.loginForm))
    }.getOrElse(NotFound)
  }

  def search(filter: String) = StackAction { implicit request =>
    val results: Seq[ProjectSearchResult] = DocsBuilderFactory.forSearching.search(filter)
    Ok(html.search(results, filter, Authenticator.loginForm))
  }

  def dashboard = StackAction { implicit request =>
    val maybeUser = loggedIn
    maybeUser.map{ usr =>
      val projects = ProjectDAO.projectsForUser(usr)
      Ok(html.dashboard(projects, Authenticator.loginForm))
    }.getOrElse(Home)
  }

  def importProject = StackAction { implicit request =>
    val maybeUser = loggedIn
    maybeUser.map{ usr =>
      Ok(html.importProject(Authenticator.loginForm, importProjectForm))
    }.getOrElse(Home)
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
      Future {
        val insertedProject = ProjectDAO.insertProject(project)
        currentUser.map { usr =>
          insertedProject.id.foreach( projId =>
            usr.id.map(userId => ProjectDAO.insertUserProject(userId -> projId))
          )
        }
        ProjectDAO.findBySlug(project.slug).map { persistedProject =>
          val docsBuilder = DocsBuilderFactory.forProject(persistedProject)
          docsBuilder.getVersions(project).map { versions =>
            val projectWithVersions = ProjectDAO.insertProjectVersions(persistedProject, versions)
            docsBuilder.initAndBuildProject(projectWithVersions)
          }.recover {
            case e: Exception => Logger.error("Exception building project", e); throw new BuildFailedException(e.getMessage)
          }
        }
      }
      Future.successful(Redirect(routes.Application.importProject).
        flashing("success" -> ("Successfully created project \""+project.name+"\"")))
    } else {
      Future.successful(Redirect(routes.Application.importProject).
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
