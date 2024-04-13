name := "Analytics"
organization := "com.example"
version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.13"

libraryDependencies ++= Seq(
  guice,
  "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.0" % Test,
  "org.apache.spark" %% "spark-core" % "3.3.0" exclude("org.scala-lang.modules", "scala-xml_2.13"),
  "org.apache.spark" %% "spark-sql" % "3.3.0",

  // Play Framework
  "com.typesafe.play" %% "play-slick" % "5.2.0",
  "com.typesafe.play" %% "play-slick-evolutions" % "5.2.0",
  "com.typesafe.play" %% "play-json" % "2.9.2",

  // Kafka
  "org.apache.kafka" %% "kafka" % "3.0.0",
  "org.apache.kafka" % "kafka-clients" % "3.0.0",
  "com.typesafe.akka" %% "akka-stream-kafka" % "2.1.1",

  // Akka
  "com.typesafe.akka" %% "akka-http" % "10.2.6",
  "com.typesafe.akka" %% "akka-stream" % "2.6.17",
  "com.typesafe.akka" %% "akka-actor" % "2.6.17",
  "com.typesafe.akka" %% "akka-slf4j" % "2.6.17",
  "com.typesafe.play" %% "play-akka-http-server" % "2.8.8",

  // Database
  "mysql" % "mysql-connector-java" % "8.0.26",
  "com.datastax.oss" % "java-driver-core" % "4.14.1",

  // Play-ws
  "com.typesafe.play" %% "play-ws" % "2.9.0",

  // Logging
  "ch.qos.logback" % "logback-classic" % "1.2.6",

  // Cache
  "com.typesafe.play" %% "play-cache" % "2.8.8",

  // MongoDB
  "org.mongodb.scala" %% "mongo-scala-driver" % "4.4.0",

  // Twirl
  "com.typesafe.play" %% "twirl-api" % "1.5.0-M2",

  // Plotly
  "org.plotly-scala" %% "plotly-render" % "0.8.2",

  // XML
  "javax.xml.bind" % "jaxb-api" % "2.3.1",

  // Mailer
  "com.typesafe.play" %% "play-mailer" % "8.0.1",
  "com.typesafe.play" %% "play-mailer-guice" % "8.0.1",

  // ReactiveMongo
  "org.reactivemongo" %% "play2-reactivemongo" % "0.20.13-play27",
  "org.reactivemongo" %% "reactivemongo-akkastream" % "0.20.13",

  // Circe for JSON
  "io.circe" %% "circe-core" % "0.14.1",
  "io.circe" %% "circe-generic" % "0.14.1",

  // Redis
  "redis.clients" % "jedis" % "3.7.0",

  // Spark SQL Kafka connector
  "org.apache.spark" %% "spark-sql-kafka-0-10" % "3.5.1"
)

// Logging
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.6"

// Play Framework cache
libraryDependencies += "com.typesafe.play" %% "play-cache" % "2.8.8"

// Dependency overrides to ensure Scala XML compatibility
dependencyOverrides += "org.scala-lang.modules" %% "scala-xml" % "1.2.0"

// MongoDB driver
libraryDependencies += "org.mongodb.scala" %% "mongo-scala-driver" % "4.4.0"

// Twirl template engine
libraryDependencies += "com.typesafe.play" %% "twirl-api" % "1.5.0-M2"

// Plotly for Scala
libraryDependencies += "org.plotly-scala" %% "plotly-render" % "0.8.2"

// XML API for Scala
libraryDependencies += "javax.xml.bind" % "jaxb-api" % "2.3.1"

// Play Mailer
libraryDependencies += "com.typesafe.play" %% "play-mailer" % "8.0.1"
libraryDependencies += "com.typesafe.play" %% "play-mailer-guice" % "8.0.1"

// ReactiveMongo for Play
libraryDependencies += "org.reactivemongo" %% "play2-reactivemongo" % "0.20.13-play27"
libraryDependencies += "org.reactivemongo" %% "reactivemongo-akkastream" % "0.20.13"

// Circe for JSON
libraryDependencies += "io.circe" %% "circe-core" % "0.14.1"
libraryDependencies += "io.circe" %% "circe-generic" % "0.14.1"

// Redis client
libraryDependencies += "redis.clients" % "jedis" % "3.7.0"

PlayKeys.devSettings += "play.server.http.idleTimeout" -> "600s"
