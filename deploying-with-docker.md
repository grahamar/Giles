# Deploying Giles using Docker

## Steps
- Run Mongo Docker on Port 27017 `sudo docker run --name giles-mongo -d --publish 27017:27017 mongo`
- Build Giles Docker Image `sudo docker build -t grahamar/giles github.com/grahamar/Giles.git`
- Run Giles Docker Container on Port 1717 `sudo docker run --name giles --link giles-mongo:mongo -d --publish 1717:1717 grahamar/giles`
