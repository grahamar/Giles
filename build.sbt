import play.Project._

name := "read-the-markdown"

version := "0.0.1"

scalaVersion := "2.10.2"

libraryDependencies ++= Seq( jdbc,
    "com.typesafe.slick" %% "slick"                         % "2.0.0-M3",
    "ch.qos.logback"     %  "logback-classic"               % "1.0.13",
    "com.h2database"     %  "h2"                            % "1.3.170",
    "org.eclipse.jgit"   %  "org.eclipse.jgit"              % "3.1.0.201310021548-r",
    "com.jcraft"         %  "jsch.agentproxy.jsch"          % "0.0.6",
    "com.jcraft"         %  "jsch.agentproxy.sshagent"      % "0.0.6",
    "com.jcraft"         %  "jsch.agentproxy.usocket-jna"   % "0.0.6",
    "jp.t2v"             %% "play2-auth"                    % "0.11.0",
    "org.mindrot"        %  "jbcrypt"                       % "0.3m",
    "net.databinder"     %% "pamflet-library"               % "0.5.0",
    "com.gilt"           %% "lib-lucene-sugar"              % "0.2.0",
    "org.apache.lucene"  %  "lucene-highlighter"            % "4.3.0",
    "net.sf.jtidy"       %  "jtidy"                         % "r938",
    "com.ocpsoft"        %  "ocpsoft-pretty-time"           % "1.0.7",
    "commons-io"         %  "commons-io"                    % "2.4",
    "com.novus"          %% "salat-core"                    % "1.9.4",
    "org.mockito"        %  "mockito-core"                  % "1.9.5"                   % "test",
    "org.scalacheck"     %% "scalacheck"                    % "1.10.1"                  % "test",
    "org.scalatest"      %% "scalatest"                     % "2.0"                     % "test"
)

playScalaSettings
