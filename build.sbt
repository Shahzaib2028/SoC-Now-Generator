// See README.md for license details.

ThisBuild / scalaVersion     := "2.12.13"
ThisBuild / version          := "0.1.0"
ThisBuild / organization     := "com.github.merledu"

lazy val caravan = project in file("caravan")

lazy val jigsaw = (project in file("jigsaw")).dependsOn(caravan)

// lazy val buraq_mini = (project in file("buraq_mini")).dependsOn(caravan, jigsaw)

lazy val nucleusrv = (project in file("nucleusrv")).dependsOn(caravan, jigsaw)

lazy val root = (project in file("."))
  .settings(
    name := "SoCNow",
    libraryDependencies ++= Seq(
      "edu.berkeley.cs" %% "chisel3" % "3.4.3",
      "edu.berkeley.cs" %% "chiseltest" % "0.3.3" % "test",
      "io.spray" %% "spray-json" % "1.3.5",
      "com.lihaoyi" %% "os-lib" % "0.2.7"
      // "org.json4s" %% "json4s-jackson" % "3.2.11"
    ),
    scalacOptions ++= Seq(
      "-Xsource:2.11",
      "-language:reflectiveCalls",
      "-deprecation",
      "-feature",
      "-Xcheckinit",
      // Enables autoclonetype2 in 3.4.x (on by default in 3.5)
      "-P:chiselplugin:useBundlePlugin"
    ),
    addCompilerPlugin("edu.berkeley.cs" % "chisel3-plugin" % "3.4.3" cross CrossVersion.full),
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full)
  )
  .dependsOn(caravan, jigsaw, nucleusrv)
