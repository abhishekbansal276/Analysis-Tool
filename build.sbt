name := """Analysis Tool"""
organization := "com.example"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.13"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.0" % Test
libraryDependencies += "org.apache.spark" %% "spark-core" % "3.5.0"
libraryDependencies += "org.apache.spark" %% "spark-sql" % "3.2.0"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-slick"            % "5.2.0",
  "com.typesafe.play" %% "play-slick-evolutions" % "5.2.0",
  "mysql" % "mysql-connector-java" % "8.0.26",
  "org.apache.kafka" %% "kafka" % "3.0.0",
  "com.typesafe.akka" %% "akka-http" % "10.2.6",
  "com.typesafe.akka" %% "akka-stream" % "2.6.17",
  "ch.qos.logback" % "logback-classic" % "1.2.6",
  "org.playframework" %% "play-json" % "3.0.2",
  "com.typesafe.akka" %% "akka-stream-kafka" % "2.1.1",
  "com.typesafe.play" %% "play-json" % "2.9.2",
  "com.datastax.oss" % "java-driver-core" % "4.14.1"
)



// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.example.controllers._"

// Adds additional packages into conf/rouest
// play.sbt.routes.RoutesKeys.routesImport += "com.example.binders._"
