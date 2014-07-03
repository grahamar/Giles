# Giles The Librarian
#
# VERSION       1.0

FROM ubuntu:latest

MAINTAINER Graham Rhodes <graham.a.r@gmail.com>

# Create the MongoDB data directory
RUN mkdir -p /data/db
RUN mkdir /data/.index
RUN mkdir /data/.git_checkouts

# Install Java 6
RUN apt-get -y install openjdk-6-jdk && apt-get clean

# Install Git
RUN apt-get install -y git

ADD https://nexus.gilt.com/nexus/content/repositories/internal-releases/com/gilt/giles_2.10/0.0.13/giles_2.10-0.0.13.tgz /opt/
RUN tar xzf /opt/giles_2.10-0.0.13.tgz -C /opt

EXPOSE 1717

ENTRYPOINT /opt/giles-0.0.13/bin/giles -Dhttp.port=1717 -Dconfig.file=/opt/giles-0.0.13/conf/production.application.conf
