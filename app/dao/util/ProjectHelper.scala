package dao.util

import models._
import settings.Global

object ProjectHelper {

  def getAuthorsForProjects(projects: Iterable[Project]): Iterable[ProjectAndAuthors] =
    projects.map(getAuthorsForProject)

  def getAuthorsForProject(project: Project): ProjectAndAuthors = {
    new ProjectAndAuthors(project, project.author_guids.map(Global.users.findByGuid).flatten)
  }

  def getAuthorsAndBuildsForProjects(projects: Iterable[Project]): Iterable[ProjectAuthorsAndBuilds] =
    projects.map(getAuthorsAndBuildsForProject)

  def getAuthorsAndBuildsForProject(project: Project): ProjectAuthorsAndBuilds = {
    new ProjectAuthorsAndBuilds(project,
      project.author_guids.map(Global.users.findByGuid).flatten,
      Global.builds.findLatestByProject(project).toSeq)
  }

}
