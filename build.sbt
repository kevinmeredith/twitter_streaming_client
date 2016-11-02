scalaVersion := "2.11.8" 

val http4sVersion = "0.14.10"

val circeVersion = "0.5.4"

libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-blaze-client" % "0.14.11a",
  "org.http4s" %% "http4s-circe"        % "0.14.11a"
)

// credit: https://tpolecat.github.io/2014/04/11/scalac-flags.html
scalacOptions ++= Seq(
  //"-deprecation",
  "-encoding", "UTF-8",       // yes, this is 2 args
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-unchecked",
  //"-Xfatal-warnings",
  "-Xlint",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",        // N.B. doesn't work well with the ??? hole
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Xfuture",
  "-Ywarn-unused-import"     // 2.11 only
)