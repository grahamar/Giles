import play.Project._

name := "giles"

organization := "com.gilt"

scalaVersion := "2.10.4"

libraryDependencies ++= Seq(
  "com.google.http-client"  % "google-http-client-gson"   % "1.18.0-rc",
  "com.google.oauth-client" % "google-oauth-client-java6" % "1.18.0-rc",
  "com.google.apis"    % "google-api-services-plus"       % "v1-rev128-1.18.0-rc",
  "org.apache.commons" % "commons-compress"               % "1.8.1",
  "ch.qos.logback"     %  "logback-classic"               % "1.0.13",
  "org.eclipse.jgit"   %  "org.eclipse.jgit"              % "3.3.2.201404171909-r",
  "org.eclipse.jgit"   % "org.eclipse.jgit.archive"       % "3.3.2.201404171909-r",
  "com.typesafe.activator" % "activator-templates-cache"  % "1.0-7e765bfb36a2121e7dbd33e81d92b8a13f294a6d" excludeAll(
    ExclusionRule(organization = "com.typesafe.akka", name = "akka-actor_2.11"),
    ExclusionRule(organization = "org.scala-lang"),
    ExclusionRule(organization = "org.scala-lang.modules"),
    ExclusionRule(organization = "org.scala-sbt")
  ),
  "org.scala-sbt"      %  "io"                            % "0.13.5",
  "com.jcraft"         %  "jsch.agentproxy.jsch"          % "0.0.6",
  "com.jcraft"         %  "jsch.agentproxy.sshagent"      % "0.0.6",
  "com.jcraft"         %  "jsch.agentproxy.usocket-jna"   % "0.0.6",
  "jp.t2v"             %% "play2-auth"                    % "0.11.0",
  "org.mindrot"        %  "jbcrypt"                       % "0.3m",
  "net.redhogs.actuarius" %%  "actuarius"                 % "0.2.7",
  "org.apache.lucene"  %  "lucene-core"                   % "4.3.0",
  "org.apache.lucene"  %  "lucene-analyzers-common"       % "4.3.0",
  "org.apache.lucene"  %  "lucene-queryparser"            % "4.3.0",
  "org.apache.lucene"  %  "lucene-highlighter"            % "4.3.0",
  "net.sf.jtidy"       %  "jtidy"                         % "r938",
  "com.ocpsoft"        %  "ocpsoft-pretty-time"           % "1.0.7",
  "commons-io"         %  "commons-io"                    % "2.4",
  "org.json4s"         %%  "json4s-native"                % "3.2.4",
  "com.novus"          %% "salat-core"                    % "1.9.4" exclude("org.json4s", "json4s-native_2.10"),
  "org.planet42"       %% "laika"                         % "0.4.0",
  "dnsjava"            %  "dnsjava"                       % "2.1.1",
  "javax.mail"         %  "mail"                          % "1.4.7",
  "net.sourceforge.plantuml" % "plantuml"                 % "7986",
  "com.fasterxml.jackson.datatype" % "jackson-datatype-joda" % "2.2.2",
  "com.wordnik"        %% "swagger-play2"                 % "1.3.2" exclude("org.json4s", "json4s-native_2.10"),
  "com.amazonaws"      %  "aws-java-sdk"                  % "1.8.0",
  "org.mockito"        %  "mockito-core"                  % "1.9.5"                   % "test",
  "org.scalacheck"     %% "scalacheck"                    % "1.10.1"                  % "test",
  "org.scalatest"      %% "scalatest"                     % "2.0"                     % "test"
)

playScalaSettings

net.virtualvoid.sbt.graph.Plugin.graphSettings

licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))

homepage := Some(url("https://github.com/grahamar/Giles"))

scmInfo := Some(ScmInfo(url("https://github.com/grahamar/Giles.git"), "scm:git:git@github.com:grahamar/Giles.git"))
