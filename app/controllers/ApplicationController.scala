package controllers

import play.api.mvc._
import play.api.data._

import views._
import controllers.auth.{AuthConfigImpl, OptionalAuthUser}
import build.{ProjectSearchResult, DocumentationFactory}
import settings.Global
import dao.util.UserProjectDAOHelper

/**
 * Manage a database of computers
 */
object ApplicationController extends Controller with OptionalAuthUser with AuthConfigImpl {

  /**
   * This result directly redirect to the application home.
   */
  val Home = Redirect(routes.ApplicationController.index)
  val Dashboard = Redirect(routes.ApplicationController.dashboard)

  // -- Actions

  /**
   * Handle default path requests, redirect to computers list
   */
  def index = StackAction { implicit request =>
    val projects = Global.projects.findRecentlyUpdated(10)
    Ok(html.index(UserProjectDAOHelper.getAuthorsAndBuildsForProjects(projects).toSeq, AuthenticationController.loginForm))
  }

  def index(loginForm: Form[Option[User]])(implicit flash: Flash, currentUser: Option[models.User]) = {
    val projects = Global.projects.findRecentlyUpdated(10)
    html.index(UserProjectDAOHelper.getAuthorsAndBuildsForProjects(projects).toSeq, loginForm)
  }

  def search(filter: String) = StackAction { implicit request =>
    val results: Seq[ProjectSearchResult] = DocumentationFactory.searchService.search(filter)
    Ok(html.search(results, filter, AuthenticationController.loginForm))
  }

  def searchProject(projectSlug: String, filter: String) = StackAction { implicit request =>
    val results: Seq[ProjectSearchResult] = DocumentationFactory.searchService.searchProject(projectSlug, filter)
    Ok(html.search(results, filter, AuthenticationController.loginForm))
  }

  def dashboard = StackAction { implicit request =>
    val maybeUser = loggedIn
    maybeUser.map{ usr =>
      val projects = Global.projects.findByAuthorGuid(usr.guid)
      Ok(html.dashboard(UserProjectDAOHelper.getAuthorsAndBuildsForProjects(projects).toSeq, AuthenticationController.loginForm))
    }.getOrElse(Home)
  }

}