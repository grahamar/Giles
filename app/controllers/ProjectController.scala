package controllers

import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global

import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.SimpleResult

import views._
import models._
import controllers.auth.{AuthConfigImpl, OptionalAuthUser}
import build.DocumentationFactory
import settings.Global
import dao.util.ProjectHelper
import java.util.UUID

object ProjectController extends Controller with OptionalAuthUser with AuthConfigImpl {

  def project(urlKey: String) = StackAction { implicit request =>
    val maybeUser = loggedIn
    Global.projects.findByUrlKey(UrlKey.generate(urlKey)).map { project =>
      maybeUser.map { currentUser =>
        if(project.author_guids.contains(currentUser.guid)) {
          Redirect(routes.ProjectController.editProject(urlKey))
        } else {
          Ok(html.project(ProjectHelper.getAuthorsAndBuildsForProject(project), AuthenticationController.loginForm))
        }
      }.getOrElse {
        Ok(html.project(ProjectHelper.getAuthorsAndBuildsForProject(project), AuthenticationController.loginForm))
      }
    }.getOrElse(NotFound)
  }

  def editProject(urlKey: String) = StackAction { implicit request =>
    Global.projects.findByUrlKey(UrlKey.generate(urlKey)).map { project =>
      Ok(html.project(ProjectHelper.getAuthorsAndBuildsForProject(project), AuthenticationController.loginForm))
    }.getOrElse(NotFound)
  }

  def pullNewVersions(urlKey: String) = StackAction { implicit request =>
    Global.projects.findByUrlKey(UrlKey.generate(urlKey)).map { project =>
      DocumentationFactory.documentsBuilder.build(project)
      Ok(html.project(ProjectHelper.getAuthorsAndBuildsForProject(project), AuthenticationController.loginForm))
    }.getOrElse(BadRequest)
  }

  def projects = StackAction { implicit request =>
    val projects = Global.projects.search(ProjectQuery())
    Ok(html.projects(ProjectHelper.getAuthorsAndBuildsForProjects(projects).toSeq, AuthenticationController.loginForm))
  }

  def importProject = StackAction { implicit request =>
    val maybeUser = loggedIn
    maybeUser.map{ usr =>
      Ok(html.importProject(AuthenticationController.loginForm, importProjectForm))
    }.getOrElse(ApplicationController.Home)
  }

  def createProject = AsyncStack { implicit request =>
    importProjectForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(html.importProject(AuthenticationController.loginForm, formWithErrors))),
      project => projectCreated(project)
    )
  }

  def projectCreated(project: ProjectImportData)(implicit request: RequestHeader, currentUser: Option[User]): Future[SimpleResult] = {
    val response = Global.projects.findByName(project.name).map { existing =>
      Redirect(routes.ProjectController.importProject).flashing("failure" -> ("Project \""+project.name+"\" already exists."))
    }.getOrElse {
      currentUser.map { usr =>
        val newProject = Global.projects.create(
          createdByGuid = usr.guid,
          guid = UUID.randomUUID(),
          name = project.name,
          description = project.description,
          repoUrl = project.repoUrl,
          headVersion = project.headVersion)
        DocumentationFactory.documentsBuilder.build(newProject)
        Global.users.update(usr.copy(project_guids = usr.project_guids ++ Seq(newProject.guid)))

        Redirect(routes.ProjectController.importProject).flashing("success" -> ("Creating project \""+project.name+"\"..."))
      }.getOrElse {
        Redirect(routes.ProjectController.importProject).flashing("failure" -> "Please login.")
      }
    }
    Future.successful(response)
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
