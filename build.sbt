import ProjectPlugin._

lazy val protocol = project in file("protocol") settings rpcProtocolSettings

lazy val client = project in file("client") settings clientRPCSettings dependsOn protocol

lazy val server = project in file("server") settings serverSettings dependsOn (protocol)

lazy val allRootModules: Seq[ProjectReference] = Seq(protocol, client, server)

lazy val allRootModulesDeps: Seq[ClasspathDependency] = allRootModules.map(ClasspathDependency(_, None))

lazy val root = project in file(".") settings (name := "my-smart-home") aggregate (allRootModules: _*) dependsOn (allRootModulesDeps: _*)

addCommandAlias("runServer", "server/runMain com.fortyseven.server.ServerApp")

addCommandAlias("runClient", "client/runMain com.fortyseven.client.ClientApp")
