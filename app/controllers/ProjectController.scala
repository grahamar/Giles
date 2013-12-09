package controllers

import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global

import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._

import views._
import models._
import controllers.auth.{AuthConfigImpl, OptionalAuthUser}
import build.DocumentationFactory
import settings.Global
import dao.util.ProjectHelper
import java.util.UUID
import play.api.mvc.SimpleResult
import models.ProjectQuery
import models.ProjectImportData

object ProjectController extends Controller with OptionalAuthUser with AuthConfigImpl {

  def project(urlKey: String) = StackAction { implicit request =>
    val maybeUser = loggedIn
    Global.projects.findByUrlKey(UrlKey.generate(urlKey)).map { project =>
      maybeUser.map { currentUser =>
        if(project.created_by.equals(currentUser.username) || project.author_usernames.contains(currentUser.username)) {
          Redirect(routes.ProjectController.editProject(urlKey))
        } else {
          Ok(html.project(ProjectHelper.getAuthorsAndBuildsForProject(project), AuthenticationController.loginForm))
        }
      }.getOrElse {
        Ok(html.project(ProjectHelper.getAuthorsAndBuildsForProject(project), AuthenticationController.loginForm))
      }
    }.getOrElse(NotFound(html.notfound(AuthenticationController.loginForm)))
  }

  def editProject(urlKey: String) = StackAction { implicit request =>
    Global.projects.findByUrlKey(UrlKey.generate(urlKey)).map { project =>
      Ok(html.project(ProjectHelper.getAuthorsAndBuildsForProject(project), AuthenticationController.loginForm))
    }.getOrElse(NotFound(html.notfound(AuthenticationController.loginForm)))
  }

  def favouriteProject(urlKey: String) = StackAction { implicit request =>
    Global.projects.findByUrlKey(UrlKey.generate(urlKey)).flatMap { project =>
      loggedIn.map { currentUser =>
        Global.favourites.create(currentUser, project)
        Ok
      }
    }.getOrElse(NotAcceptable)
  }

  def unfavouriteProject(urlKey: String) = StackAction { implicit request =>
    Global.projects.findByUrlKey(UrlKey.generate(urlKey)).flatMap { project =>
      loggedIn.flatMap { currentUser =>
        Global.favourites.findByUserAndProject(currentUser, project).map { fav =>
          Global.favourites.delete(fav.guid)
          Ok
        }
      }
    }.getOrElse(NotAcceptable)
  }

  def pullNewVersions(urlKey: String) = StackAction { implicit request =>
    Global.projects.findByUrlKey(UrlKey.generate(urlKey)).map { project =>
      DocumentationFactory.documentsBuilder.build(project)
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
  }.fill(DefaultProjectImportData)

}
