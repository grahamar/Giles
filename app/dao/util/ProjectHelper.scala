package dao.util

import models._
import settings.Global

object ProjectHelper {

  def getAuthorsForProjects(projects: Iterable[Project]): Iterable[ProjectAndAuthors] =
    projects.map(getAuthorsForProject)

  def getAuthorsForProject(project: Project): ProjectAndAuthors = {
    new ProjectAndAuthors(project, project.author_usernames.map(Global.users.findByUsername).flatten)
  }

  def getAuthorsAndBuildsForProjects(projects: Iterable[Project]): Iterable[ProjectAuthorsAndBuilds] = {
    projects.map(getAuthorsAndBuildsForProject)
  }

  def getAuthorsAndBuildsForProject(project: Project): ProjectAuthorsAndBuilds = {
    val authors = project.author_usernames.map(Global.users.findByUsername).flatten
    val latestBuilds = Global.builds.findLatestByProject(project).toSeq

    new ProjectAuthorsAndBuilds(project, authors, latestBuilds)
  }

  def getFavouriteProjectsForUser(user: User): Iterable[ProjectAuthorsAndBuilds] = {
    getAuthorsAndBuildsForProjects(Global.favourites.findAllByUser(user).flatMap(fav => Global.projects.findByGuid(fav.project_guid)))
  }

}
