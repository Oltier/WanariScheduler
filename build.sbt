name := """WNRScheduler"""
organization := "com.wanari"
version := "1.0.0-SNAPSHOT"

scalaVersion := "2.12.2"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.5.1",
  "com.typesafe.akka" %% "akka-testkit" % "2.5.1" % "test",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  "com.cronutils" % "cron-utils" % "5.0.5"
)
