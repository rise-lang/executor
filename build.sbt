lazy val buildExecutor = taskKey[Unit]("Builds C executor library")

buildExecutor := {
  import scala.language.postfixOps
  import scala.sys.process._
  //noinspection PostfixMethodCall
  "echo y" #| (baseDirectory.value + "/buildExecutor.sh") !
}

lazy val executor = (project in file("."))
  .settings(
    name    := "OpenCL executor",
    version := "1.0",
    scalaVersion := "3.1.2",
    organization := "org.rise-lang",
    libraryDependencies += "junit" % "junit" % "4.13.2",
    libraryDependencies += "org.scala-lang.modules" %% "scala-parallel-collections" % "1.0.4",

    compileOrder := CompileOrder.JavaThenScala,

    compile := ((Compile / compile) dependsOn buildExecutor).value,
    test    := ((Test / test) dependsOn buildExecutor).value
  )
