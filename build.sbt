name := "play2-data-resources"

organization := "play.api.data"

version := "0.1-SNAPSHOT"

libraryDependencies ++= Seq(
  "play" %% "play" % "2.1-SNAPSHOT",
  "org.specs2" %% "specs2" % "1.7.1" % "test",
  "junit" % "junit" % "4.8" % "test"  
)

publishTo <<=  version { (v: String) => 
    val base = "../../workspace_mandubian/mandubian-mvn"
	if (v.trim.endsWith("SNAPSHOT")) 
		Some(Resolver.file("snapshots", new File(base + "/snapshots")))
	else Some(Resolver.file("releases", new File(base + "/releases")))
}

publishMavenStyle := true

publishArtifact in Test := false