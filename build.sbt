import play.Project._

name := "read-the-markdown"

version := "0.0.1"

scalaVersion := "2.10.2"

libraryDependencies ++= Seq( jdbc,
    "com.typesafe.slick" %% "slick"            % "2.0.0-M3",
    "ch.qos.logback"     %  "logback-classic"  % "1.0.13",
    "com.h2database"     %  "h2"               % "1.3.170",
    "org.eclipse.jgit"   %  "org.eclipse.jgit" % "3.1.0.201310021548-r",
    "jp.t2v"             %% "play2-auth"       % "0.11.0",
    "org.mindrot"        %  "jbcrypt"          % "0.3m",
    "net.databinder"     %% "pamflet-library"  % "0.5.0",
    "commons-io"         %  "commons-io"       % "2.4"
)

playScalaSettings
