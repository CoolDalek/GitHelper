name := "GitHelper"

version := "0.1"

scalaVersion := "2.13.7"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-effect" % "3.3.4",
  "org.kohsuke" % "github-api" % "1.301",
  "org.xerial" % "sqlite-jdbc" % "3.28.0",
  "com.github.pureconfig" %% "pureconfig" % "0.17.1",
  "org.tpolecat" %% "doobie-core" % "1.0.0-RC1",
  "eu.timepit" %% "refined-pureconfig" % "0.9.28",
  "eu.timepit" %% "refined" % "0.9.28",
  "eu.timepit" %% "refined-cats" % "0.9.28",
  "io.monix" %% "newtypes-core" % "0.0.1",
  "com.github.pureconfig" %% "pureconfig-cats" % "0.17.1",
  "com.softwaremill.sttp.client3" %% "async-http-client-backend-cats" % "3.3.18",
)
