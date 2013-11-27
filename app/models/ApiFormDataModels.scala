package models

case class PutProjectFormData(guid: String, name: String, description: String, author_guid: String,
                              repoUrl: String, headVersion: Option[String] = Some("HEAD"))

case class PutVersionFormData(project_guid: String, version: String)

case class PutViewFormData(guid: String, file_guid: String, user_guid: Option[String])

case class PutFileFormData(guid: String, project_guid: String, version: String, title: String, html: String)