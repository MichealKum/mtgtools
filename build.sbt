import com.typesafe.sbt.SbtNativePackager.autoImport.NativePackagerHelper._
import com.typesafe.sbt.packager.SettingsHelper._

name := "MTGTools"

organization := "com.kum.mtg"

scalaVersion := "2.12.4"
scalacOptions := Seq("-feature")

parallelExecution in Test := false

resolvers += Resolver.mavenLocal
resolvers += Resolver.sonatypeRepo("public")

libraryDependencies += "org.xerial.snappy" % "snappy-java" % "1.1.2"
libraryDependencies += "org.apache.httpcomponents" % "httpclient" % "4.5.3"
libraryDependencies += "org.jsoup" % "jsoup" % "1.11.2"
libraryDependencies += "com.github.scopt" %% "scopt" % "3.7.0"
libraryDependencies += "org.scalaj" %% "scalaj-http" % "2.3.0"

// Java-test
libraryDependencies += "junit" % "junit" % "4.12" % Test
libraryDependencies += "org.hamcrest" % "hamcrest-all" % "1.3" % Test
libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % Test
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % Test
libraryDependencies += "org.scalamock" %% "scalamock-scalatest-support" % "3.6.0" % Test

// scalastyle
//lazy val compileScalastyle = taskKey[Unit]("compileScalastyle")
//compileScalastyle := org.scalastyle.sbt.ScalastylePlugin.scalastyle.in(Compile).toTask("").value
//(compile in Compile) := ((compile in Compile) dependsOn compileScalastyle).value

