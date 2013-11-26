package controllers

import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

import views._
import dao._
import auth.{OptionalAuthUser, Authenticator, AuthConfigImpl}
import build.{ProjectSearchResult, DocsBuilderFactory}

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

  def search(filter: String) = StackAction { implicit request =>
    val results: Seq[ProjectSearchResult] = DocsBuilderFactory.searchService.search(filter)
    Ok(html.search(results, filter, Authenticator.loginForm))
  }

  def searchProject(projectSlug: String, filter: String) = StackAction { implicit request =>
    val results: Seq[ProjectSearchResult] = DocsBuilderFactory.searchService.searchProject(projectSlug, filter)
    Ok(html.search(results, filter, Authenticator.loginForm))
  }

  def dashboard = StackAction { implicit request =>
    val maybeUser = loggedIn
    maybeUser.map{ usr =>
      val projects = ProjectDAO.projectsForUser(usr)
      Ok(html.dashboard(projects, Authenticator.loginForm))
    }.getOrElse(Home)
  }

}