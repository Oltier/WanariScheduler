name := """WNRScheduler"""
organization := "com.wanari"
version := "1.0.0-SNAPSHOT"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4.17",
  "com.typesafe.akka" %% "akka-testkit" % "2.4.17" % "test",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  "com.cronutils" % "cron-utils" % "5.0.5"
)
