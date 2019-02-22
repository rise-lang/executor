
scalaVersion     := "2.12.8"
version          := "0.1.0-SNAPSHOT"
organization     := "com.example"
 organizationName := "example"

lazy val root = (project in file("."))
  .settings(
    name := "opencl/executor"
  )
