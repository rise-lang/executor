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
    scalaVersion := "3.0.0-RC1",
    organization := "org.rise-lang",
    libraryDependencies += "junit" % "junit" % "4.11",
    libraryDependencies += "org.scala-lang.modules" %% "scala-parallel-collections" % "1.0.1",

    compileOrder := CompileOrder.JavaThenScala,

    compile := ((compile in Compile) dependsOn buildExecutor).value,
    test    := ((test in Test) dependsOn buildExecutor).value
  )
