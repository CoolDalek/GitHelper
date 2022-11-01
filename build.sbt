name := "GitHelper"

version := "0.1"

scalaVersion := "3.2.1"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-effect" % "3.3.12",
  "org.xerial" % "sqlite-jdbc" % "3.39.3.0",
  "org.tpolecat" %% "doobie-core" % "1.0.0-RC2",
  "com.github.pureconfig" %% "pureconfig-core" % "0.17.1",
  "com.softwaremill.sttp.client3" %% "fs2" % "3.8.3",
  "com.outr" %% "scribe" % "3.10.4",
  "com.outr" %% "scribe-cats" % "3.10.4",
  "co.fs2" %% "fs2-core" % "3.3.0",
  "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core" % "2.17.6",
  "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros" % "2.17.6" % Compile
)

scalacOptions ++= Seq(
  "-source:future"
)