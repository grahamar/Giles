# Using the REST API

```
http://localhost:9000/api/*
```

## Users Endpoint

List all users.

```
http://localhost:9000/api/users
```
| Details  |  Value | Type Required/Optional  |
| ---- | ----- | ---- |
| Method | `GET` |  |
| Endpoint | `/api/users` |  |
| URI parameters | guid | Option[String] |
| | username | Option[String] |
| | email | Option[String] |
| | limit | Option[String] |
| | offset | Option[String] |

## Projects Endpoint

List all projects.

```
http://localhost:9000/api/projects
```
| Details  |  Value | Type Required/Optional  |
| ---- | ----- | ---- |
| Method | `GET` |  |
| Endpoint | `/api/projects` |  |
| URI parameters | guid | Option[String] |
| | name | Option[String] |
| | author_guids | Option[String] |
| | query | Option[String] |
| | urlKey | Option[String] |
| | limit | Option[String] |
| | offset | Option[String] |

## Versions Endpoint

List all versions.

```
http://localhost:9000/api/versions
```
| Details  |  Value | Type Required/Optional  |
| ---- | ----- | ---- |
| Method | `GET` |  |
| Endpoint | `/api/versions` |  |
| URI parameters | projectGuid | Required[String] |
| | version | Option[String] |

## Files Endpoint

List all files.

```
http://localhost:9000/api/files
```
| Details  |  Value | Type Required/Optional  |
| ---- | ----- | ---- |
| Method | `GET` |  |
| Endpoint | `/api/files` |  |
| URI parameters | guid | Option[String] |
| | username | Option[String] |
| | email | Option[String] |
| | limit | Option[String] |
| | offset | Option[String] |

## Builds Endpoint

List all builds.

```
http://localhost:9000/api/builds
```
| Details  |  Value | Type Required/Optional  |
| ---- | ----- | ---- |
| Method | `GET` |  |
| Endpoint | `/api/builds` |  |
| URI parameters | guid | Option[String] |
| | username | Option[String] |
| | email | Option[String] |
| | limit | Option[String] |
| | offset | Option[String] |

## Views Endpoint

List all views.

```
http://localhost:9000/api/views
```
| Details  |  Value | Type Required/Optional  |
| ---- | ----- | ---- |
| Method | `GET` |  |
| Endpoint | `/api/views` |  |
| URI parameters | guid | Option[String] |
| | username | Option[String] |
| | email | Option[String] |
| | limit | Option[String] |
| | offset | Option[String] |


### Example CURL scripts

Create A Project
```
curl -X PUT -d guid="d363ab8a-a329-4b9e-89b4-37a8478118c1" -d name="Test Project" -d description="Test Project" -d author_guid="688f6c92-d2aa-43f4-890b-25c60523b53e" -d repo_url="https://github.com/grahamar/example-rtm-project.git" -d head_version="HEAD" localhost:9000/api/projects
```
