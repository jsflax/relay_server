logLevel := Level.Warn

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "gseitz@github" at "http://gseitz.github.com/maven/"

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.5.1")

addSbtPlugin("com.github.gseitz" % "sbt-protobuf" % "0.5.1")
