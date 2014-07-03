# Giles The Librarian
#
# VERSION       1.0

FROM ubuntu:latest

MAINTAINER Graham Rhodes <graham.a.r@gmail.com>

# Add 10gen official apt source to the sources list
RUN apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 7F0CEB10
RUN echo 'deb http://downloads-distro.mongodb.org/repo/ubuntu-upstart dist 10gen' | tee /etc/apt/sources.list.d/10gen.list

# Install MongoDB
RUN apt-get update
RUN apt-get install -y -q mongodb-org

# Create the MongoDB data directory
RUN mkdir -p /data/db
RUN mkdir /data/.index
RUN mkdir /data/.git_checkouts

# Install Java 6
RUN apt-get -y install openjdk-6-jdk && apt-get clean

# Install Git
RUN apt-get install -y git curl

# Install SBT
RUN curl -o /tmp/sbt.deb http://scalasbt.artifactoryonline.com/scalasbt/sbt-native-packages/org/scala-sbt/sbt/0.13.0/sbt.deb
RUN dpkg -i /tmp/sbt.deb

# Install Giles
RUN mkdir /data/giles_repo
RUN cd /data/giles_repo ; git clone https://github.com/grahamar/giles.git

WORKDIR /data/giles_repo/giles

RUN sbt stage

RUN cp -R target/universal/stage /opt/giles/

WORKDIR /opt/giles

EXPOSE 27017 1717

ENTRYPOINT /usr/bin/mongod --fork --syslog & bin/giles -Dhttp.port=1717 -Dconfig.file=conf/production.application.conf
