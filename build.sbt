import play.Project._

name := "giles"

organization := "com.gilt"

scalaVersion := "2.10.2"

libraryDependencies ++= Seq(
    "ch.qos.logback"     %  "logback-classic"               % "1.0.13",
    "org.eclipse.jgit"   %  "org.eclipse.jgit"              % "3.1.0.201310021548-r",
    "com.jcraft"         %  "jsch.agentproxy.jsch"          % "0.0.6",
    "com.jcraft"         %  "jsch.agentproxy.sshagent"      % "0.0.6",
    "com.jcraft"         %  "jsch.agentproxy.usocket-jna"   % "0.0.6",
    "jp.t2v"             %% "play2-auth"                    % "0.11.0",
    "org.mindrot"        %  "jbcrypt"                       % "0.3m",
    "eu.henkelmann"      %  "actuarius_2.10.0"              % "0.2.6",
    "org.apache.lucene"  %  "lucene-core"                   % "4.3.0",
    "org.apache.lucene"  %  "lucene-analyzers-common"       % "4.3.0",
    "org.apache.lucene"  %  "lucene-queryparser"            % "4.3.0",
    "org.apache.lucene"  %  "lucene-highlighter"            % "4.3.0",
    "net.sf.jtidy"       %  "jtidy"                         % "r938",
    "com.ocpsoft"        %  "ocpsoft-pretty-time"           % "1.0.7",
    "commons-io"         %  "commons-io"                    % "2.4",
    "org.json4s"         %  "json4s-native_2.10"            % "3.2.4",
    "com.novus"          %% "salat-core"                    % "1.9.4" exclude("org.json4s", "json4s-native_2.10"),
    "org.planet42"       %% "laika"                         % "0.4.0",
    "dnsjava"            %  "dnsjava"                       % "2.1.1",
    "javax.mail"         %  "mail"                          % "1.4.7",
    "net.sourceforge.plantuml" % "plantuml"                 % "7986",
    "com.fasterxml.jackson.datatype" % "jackson-datatype-joda" % "2.2.2",
    "com.wordnik"        %% "swagger-play2"                 % "1.3.2" exclude("org.json4s", "json4s-native_2.10"),
    "org.mockito"        %  "mockito-core"                  % "1.9.5"                   % "test",
    "org.scalacheck"     %% "scalacheck"                    % "1.10.1"                  % "test",
    "org.scalatest"      %% "scalatest"                     % "2.0"                     % "test"
)

playScalaSettings

net.virtualvoid.sbt.graph.Plugin.graphSettings
