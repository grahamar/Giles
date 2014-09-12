# Giles The Librarian
#
# VERSION       1.0

FROM ubuntu:latest

MAINTAINER Graham Rhodes <graham.a.r@gmail.com>

RUN apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 7F0CEB10
RUN echo 'deb http://downloads-distro.mongodb.org/repo/ubuntu-upstart dist 10gen' | tee /etc/apt/sources.list.d/10gen.list

RUN apt-get update

# Create the MongoDB data directory
RUN mkdir -p /data/db
RUN mkdir /data/.git_checkouts

# Install Java 6
RUN apt-get -y install openjdk-6-jdk && apt-get clean

# Install Git & Graphviz
RUN apt-get install -y git graphviz

WORKDIR /opt

ADD https://nexus.gilt.com/nexus/content/repositories/internal-releases/com/gilt/giles_2.10/1.0.0/giles_2.10-1.0.0.tgz /opt/
RUN tar xfv /opt/giles_2.10-1.0.0.tgz

EXPOSE 1717

ENTRYPOINT /opt/giles-1.0.0/bin/giles -Dhttp.port=1717 -Dconfig.file=/opt/giles-1.0.0/conf/production.application.conf
