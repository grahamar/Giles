package controllers

import java.util.UUID

import build.DocumentationFactory
import controllers.auth.{AuthConfigImpl, OptionalAuthUser}
import dao.util.ProjectHelper
import models.{ProjectImportData, ProjectQuery, _}
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json
import play.api.mvc.{SimpleResult, _}
import settings.Global
import views._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

object ProjectController extends Controller with OptionalAuthUser with AuthConfigImpl {

  def project(urlKey: String) = StackAction { implicit request =>
    Global.projects.findByUrlKey(UrlKey.generateProjectUrlKey(urlKey)).map { project =>
      Ok(html.project(ProjectHelper.getAuthorsAndBuildsForProject(project), editProjectorm, AuthenticationController.loginForm))
    }.getOrElse(NotFound(html.notfound(AuthenticationController.loginForm)))
  }

  def editProject(urlKey: String) = StackAction { implicit request =>
    val maybeUser = loggedIn
    maybeUser.map { loggedInUser =>
      Global.projects.findByUrlKey(UrlKey.generateProjectUrlKey(urlKey)).map { project =>
        editProjectorm.bindFromRequest.fold(
          formWithErrors =>
            BadRequest(html.project(ProjectHelper.getAuthorsAndBuildsForProject(project), formWithErrors, AuthenticationController.loginForm)),
          data => Global.projects.update(project.copy(repo_url = data.repo_url, head_version = data.head_version))
        )
        Redirect(routes.ProjectController.project(urlKey))
      }.getOrElse(NotFound(html.notfound(AuthenticationController.loginForm)))
    }.getOrElse(Unauthorized)
  }

  def favouriteProject(urlKey: String) = StackAction { implicit request =>
    Global.projects.findByUrlKey(UrlKey.generateProjectUrlKey(urlKey)).flatMap { project =>
      loggedIn.map { currentUser =>
        Global.favourites.create(currentUser, project)
        Ok
      }
    }.getOrElse(NotAcceptable)
  }

  def unfavouriteProject(urlKey: String) = StackAction { implicit request =>
    Global.projects.findByUrlKey(UrlKey.generateProjectUrlKey(urlKey)).flatMap { project =>
      loggedIn.flatMap { currentUser =>
        Global.favourites.findByUserAndProject(currentUser, project).map { fav =>
          Global.favourites.delete(fav.guid)
          Ok
        }
      }
    }.getOrElse(NotAcceptable)
  }

  def projectVersions(urlKey: String) = StackAction { implicit request =>
    Global.projects.findByUrlKey(UrlKey.generateProjectUrlKey(urlKey)).map { project =>
      Ok(Json.toJson(project.versions)).as("application/json")
    }.getOrElse(BadRequest)
  }

  def pullNewVersions(urlKey: String) = StackAction { implicit request =>
    Global.projects.findByUrlKey(UrlKey.generateProjectUrlKey(urlKey)).map { project =>
      DocumentationFactory.documentsBuilder.build(project)
      Global.projects.markUpdated(project)
      Redirect(routes.ProjectController.project(urlKey))
    }.getOrElse(BadRequest)
  }

  def projects = StackAction { implicit request =>
    val projects = Global.projects.search(ProjectQuery())
    val userFavourites = loggedIn.map(Global.favourites.findAllByUser(_).map(_.project_guid).toSeq).getOrElse(Seq.empty)
    Ok(html.projects(ProjectHelper.getAuthorsAndBuildsForProjects(projects).toSeq, userFavourites, AuthenticationController.loginForm))
  }

  def importProject = StackAction { implicit request =>
    val maybeUser = loggedIn
    maybeUser.map{ usr =>
      Ok(html.importProject(AuthenticationController.loginForm, importProjectForm))
    }.getOrElse(ApplicationController.Home)
  }

  def createProject = AsyncStack { implicit request =>
    importProjectForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(html.importProject(AuthenticationController.loginForm, formWithErrors)))
      },
      project => projectCreated(project)
    )
  }

  def projectCreated(project: ProjectImportData)(implicit request: RequestHeader, currentUser: Option[User]): Future[SimpleResult] = {
    Global.projects.findByName(project.name).map { existing =>
      Future.successful(Redirect(routes.ProjectController.importProject).flashing("failure" -> ("Project \""+project.name+"\" already exists.")))
    }.getOrElse {
      currentUser.map { usr =>
        Future {
          val newProject = Global.projects.create(
            createdByUsername = usr.username,
            guid = UUID.randomUUID().toString,
            name = project.name,
            description = project.description,
            repoUrl = project.repoUrl,
            authorUsernames = Seq.empty,
            headVersion = project.headVersion)
          DocumentationFactory.documentsBuilder.build(newProject)
          Global.users.update(usr.copy(project_guids = usr.project_guids ++ Seq(newProject.guid)))
        }
        Future.successful(Redirect(routes.ProjectController.importProject).flashing("success" -> ("Creating project \""+project.name+"\"...")))
      }.getOrElse {
        Future.successful(Redirect(routes.ProjectController.importProject).flashing("failure" -> "Please login."))
      }
    }
  }

  val importProjectForm = Form {
    mapping(
      "name" -> nonEmptyText,
      "description" -> text,
      "repo" -> nonEmptyText,
      "head_version" -> nonEmptyText
    )(ProjectImportData.apply)(ProjectImportData.unapply)
  }

  val editProjectorm = Form {
    mapping(
      "repo_url" -> nonEmptyText,
      "head_version" -> nonEmptyText
    )(ProjectEditData.apply)(ProjectEditData.unapply)
  }

}
