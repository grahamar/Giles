package models

case class PutUserFormData(guid: String, username: String, email: String, password: String, first_name: Option[String],
                           last_name: Option[String], homepage: Option[String])

case class PutProjectFormData(guid: String, name: String, description: String, author_guid: String,
                              repo_url: String, head_version: Option[String] = Some("HEAD"))

case class PutVersionFormData(project_guid: String, version: String)

case class PutViewFormData(guid: String, file_guid: String, user_guid: Option[String])

case class PutFileFormData(guid: String, project_guid: String, version: String, title: String, filename: String, relative_path: Option[String], html: String)