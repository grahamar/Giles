import play.Project._

name := "read-the-markdown"

version := "0.0.1"

scalaVersion := "2.10.2"

libraryDependencies ++= Seq( jdbc,
    "com.typesafe.slick" %% "slick"           % "2.0.0-M3",
    "ch.qos.logback"     %  "logback-classic" % "1.0.13",
    "com.h2database"     %  "h2"              % "1.3.170"
)

playScalaSettings
