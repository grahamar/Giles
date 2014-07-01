# Giles The Librarian
#
# VERSION       1.0

FROM ubuntu

MAINTAINER Graham Rhodes (graham.a.r@gmail.com)

# Add 10gen official apt source to the sources list
RUN apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 7F0CEB10
RUN echo 'deb http://downloads-distro.mongodb.org/repo/ubuntu-upstart dist 10gen' | tee /etc/apt/sources.list.d/10gen.list

# Install MongoDB
RUN apt-get update
RUN apt-get install mongodb-org

# Create the MongoDB data directory
RUN mkdir -p /data/db
RUN mkdir /data/.index
RUN mkdir /data/.git_checkouts

EXPOSE 27017
ENTRYPOINT usr/bin/mongod

# Install Java 6
RUN apt-get -y install openjdk-6-jdk && apt-get clean

# Install Git
RUN apt-get install -y git curl

# Install Scala
RUN curl -o /tmp/scala-2.10.2.tgz http://www.scala-lang.org/files/archive/scala-2.10.2.tgz
RUN tar xzf /tmp/scala-2.10.2.tgz -C /usr/share/
RUN ln -s /usr/share/scala-2.10.2 /usr/share/scala

# Symlink scala binary to /usr/bin
RUN for i in scala scalc fsc scaladoc scalap; do ln -s /usr/share/scala/bin/${i} /usr/bin/${i}; done

# Install SBT
RUN curl -o /tmp/sbt.deb http://scalasbt.artifactoryonline.com/scalasbt/sbt-native-packages/org/scala-sbt/sbt/0.13.2/sbt.deb
RUN dpkg -i /tmp/sbt.deb

# Install Giles
RUN cd /opt/ ; git clone https://github.com/grahamar/Giles.git
RUN cd /opt/Giles ; sbt compile

ENTRYPOINT cd /opt/Giles ; sbt "run 1717"
