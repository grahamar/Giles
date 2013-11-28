
CURL Examples
======

Create A Project
    curl -X PUT -d guid="d363ab8a-a329-4b9e-89b4-37a8478118c1" -d name="Test Project" -d description="Test Project" -d author_guid="688f6c92-d2aa-43f4-890b-25c60523b53e" -d repo_url="https://github.com/grahamar/example-rtm-project.git" -d head_version="HEAD" localhost:9000/api/projects
