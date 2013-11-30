# Using the REST API

```
http://localhost:9000/api/*
```

## Users Endpoint

```
http://localhost:9000/api/users
```

List all users.

| Details  |  Value | Type Required/Optional  |
| ---- | ----- | ---- |
| Method | `GET` |  |
| Endpoint | `/api/users` |  |
| URI parameters | guid | Option[UUID] |
| | username | Option[String] |
| | email | Option[String] |
| | limit | Option[String] |
| | offset | Option[String] |


Create a user

| Details  |  Value | Type Required/Optional  |
| ---- | ----- | ---- |
| Method | `PUT` |  |
| Endpoint | `/api/users` |  |
| URI parameters | guid | Required[UUID] |
| | username | Required[String] |
| | email | Required[String] |
| | password | Required[String] |
| | first_name | Option[String] |
| | last_name | Option[String] |
| | homepage | Option[String] |

## Projects Endpoint

```
http://localhost:9000/api/projects
```

List all projects.

| Details  |  Value | Type Required/Optional  |
| ---- | ----- | ---- |
| Method | `GET` |  |
| Endpoint | `/api/projects` |  |
| URI parameters | guid | Option[UUID] |
| | name | Option[String] |
| | author_guids | Option[Seq[UUID]] |
| | query | Option[String] |
| | urlKey | Option[String] |
| | limit | Option[String] |
| | offset | Option[String] |

## Versions Endpoint

```
http://localhost:9000/api/versions
```

List all versions.

| Details  |  Value | Type Required/Optional  |
| ---- | ----- | ---- |
| Method | `GET` |  |
| Endpoint | `/api/versions` |  |
| URI parameters | projectGuid | Required[UUID] |
| | version | Option[String] |

## Files Endpoint

```
http://localhost:9000/api/files
```

List all files.

| Details  |  Value | Type Required/Optional  |
| ---- | ----- | ---- |
| Method | `GET` |  |
| Endpoint | `/api/files` |  |
| URI parameters | guid | Option[String] |
| | name | Option[String] |
| | project_guid | Option[String] |
| | query | Option[String] |
| | title | Option[String] |
| | urlKey | Option[String] |
| | limit | Option[String] |
| | offset | Option[String] |

## Builds Endpoint

```
http://localhost:9000/api/builds
```

List all builds.

| Details  |  Value | Type Required/Optional  |
| ---- | ----- | ---- |
| Method | `GET` |  |
| Endpoint | `/api/builds` |  |
| URI parameters | guid | Option[UUID] |
| | projectGuid | Option[UUID] |
| | version | Option[String] |
| | limit | Option[String] |
| | offset | Option[String] |

## Views Endpoint

```
http://localhost:9000/api/views
```

List all views.

| Details  |  Value | Type Required/Optional  |
| ---- | ----- | ---- |
| Method | `PUT` |  |
| Endpoint | `/api/views` |  |
| URI parameters | guid | Required[UUID] |
| | file_guid | Required[UUID] |
| | user_guid | Required[UUID] |


### Example CURL scripts

Create A Project
```
curl -X PUT -d guid="d363ab8a-a329-4b9e-89b4-37a8478118c1" -d name="Test Project" -d description="Test Project" -d author_guid="688f6c92-d2aa-43f4-890b-25c60523b53e" -d repo_url="https://github.com/grahamar/example-rtm-project.git" -d head_version="HEAD" localhost:9000/api/projects
```
