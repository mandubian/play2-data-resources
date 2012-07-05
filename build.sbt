name := "play2-data-resources"

organization := "play.api.data"

version := "0.1-SNAPSHOT"

libraryDependencies ++= Seq(
  "play" %% "play" % "2.1-SNAPSHOT",
  "org.specs2" %% "specs2" % "1.7.1" % "test",
  "junit" % "junit" % "4.8" % "test"  
)

resolvers += Resolver.file("local repository", file("/Volumes/PVO/workspaces/workspace_zen/Play20/repository/local"))(Resolver.ivyStylePatterns)

//resolvers += "Typesafe repository snapshots" at "http://repo.typesafe.com/typesafe/snapshots/"

//resolvers += "Typesafe repository releases" at "http://repo.typesafe.com/typesafe/releases/" 

//resolvers += "Scala-Tools" at "https://oss.sonatype.org/content/groups/scala-tools/"

//resolvers += "Scala-Tools-Snapshot" at "https://oss.sonatype.org/content/repositories/snapshots/"