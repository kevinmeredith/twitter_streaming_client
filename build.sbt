scalaVersion := "2.11.8" 

val http4sVersion = "0.14.11a"
val circeVersion = "0.5.4"

libraryDependencies ++= Seq(
  "org.http4s"    %% "http4s-blaze-server" % http4sVersion,
  "org.http4s"    %% "http4s-blaze-client" % http4sVersion,
  "org.http4s"    %% "http4s-circe"        % http4sVersion,
  "org.http4s"    %% "http4s-dsl"          % http4sVersion,
  "io.circe"      %% "circe-core"          % circeVersion,
  "io.circe"      %% "circe-generic"       % circeVersion,
  "io.circe"      %% "circe-parser"        % circeVersion,
  "org.scalatest" %% "scalatest"           % "3.0.0",
  "joda-time"     % "joda-time"            % "2.9.5",
  "org.joda"      % "joda-convert"         % "1.2" // http://stackoverflow.com/a/13856382/409976
)

fork := true

javaOptions in test += "-Dfile.encoding=UTF-16"

// credit: https://tpolecat.github.io/2014/04/11/scalac-flags.html
scalacOptions ++= Seq(
  //"-deprecation",
  "-encoding", "UTF-8",       // yes, this is 2 args
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-unchecked",
  "-Xfatal-warnings",
  "-Xlint",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",        // N.B. doesn't work well with the ??? hole
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Xfuture",
  "-Ywarn-unused-import"     // 2.11 only
)

javaOptions ++= Seq("-XX:MaxPermSize=128m", "-Xmx1G")