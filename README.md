# My Smart Home workshop

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->


- [What are we going to build?](#what-are-we-going-to-build)
- [Basic Freestyle-RPC Structure](#basic-freestyle-rpc-structure)
  - [How to run it](#how-to-run-it)
  - [Project structure](#project-structure)
    - [Protocol](#protocol)
    - [Server](#server)
    - [Client](#client)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->


## What are we going to build?

During the course of this workshop, we are going to build a couple of purely functional microservices, which are going to interact with each other in different ways but always via the RPC protocol. One as a server will play the role of a smart home and the other will be a client, a mobile app for instance, and the interactions will be:

- `IsEmpty`: Will be a unary RPC, that means that the smart home will return a single response to each request from the mobile, to let it know if there is anybody inside the home or there isn't.

- `getTemperature`: Will be a unidirectional streaming service from the server, where the smart home will return a stream of temperature values in real-time after a single request from the mobile.

- `comingBackMode`: Will be a unidirectional streaming service from the client, where the mobile app sends a stream of location coordinates and the smart home returns a list of operations that are being triggered. For instance:
   - If the client is about 30 minutes to come back, the home can start heating the living room and increase the power of the hot water heater.
   - If the client is only a 2-minute walk away, the home can turn some lights on and turn the irrigation system off.
   - If the client is in front of the main door, this can be unlocked and the alarms disabled.

## Basic Freestyle-RPC Structure

We are going to use the `rpc-server-client-pb` giter8 template to create the basic project structure, which provides a good basis to build upon. In this case, the template creates a multimodule project, with:
- The RPC protocol, which is very simple. It exposes a service to lookup a person given a name.
- The server, which with implements an interpreter of the service defined by the protocol and it runs an RPC server.
- The client, which consumes the RPC endpoint against the server, and it uses the protocol to know the schema.

To start:

```bash
sbt new frees-io/rpc-server-client-pb.g8
...
name [Project Name]: SmartHome
projectDescription [Project Description]: My SmartHome app
project [project-name]: smarthome
package [org.mycompany]: com.fortyseven
freesRPCVersion [0.14.0]:

Template applied in ./smarthome
```

### How to run it

Run the server:

```bash
sbt runServer
```

And the log will show:

```bash
INFO  - Starting server at localhost:19683
```

then, run the client:

```bash
sbt runClient
```

The client should log:

```bash
INFO  - Created new RPC client for (localhost,19683)
INFO  - Request: foo
INFO  - Result: GetPersonResponse(Person(foo,10))
INFO  - Removed 1 RPC clients from cache.
```

And the server:

```bash
INFO  - PeopleService - Request: GetPersonRequest(foo)
```

### Project structure

```bash
.
├── LICENSE
├── NOTICE.md
├── README.md
├── build.sbt
├── client
│   └── src
│       └── main
│           ├── resources
│           │   └── logback.xml
│           └── scala
│               ├── ClientApp.scala
│               ├── ClientRPC.scala
│               └── PeopleServiceClient.scala
├── project
│   ├── ProjectPlugin.scala
│   ├── build.properties
│   ├── plugins.sbt
│   └── project
├── protocol
│   └── src
│       └── main
│           └── scala
│               └── protocol.scala
├── server
│   └── src
│       └── main
│           ├── resources
│           │   └── logback.xml
│           └── scala
│               ├── PeopleServiceHandler.scala
│               ├── ServerApp.scala
│               └── ServerBoot.scala
└── version.sbt
```

#### Protocol

The protocol module includes the definition of the service and the messages that will be used both by the server and the client:

```bash
├── protocol
│   └── src
│       └── main
│           └── scala
│               └── protocol.scala
```

**_protocol.scala_**

We have to define the protocol. In this case is just an operation called `getPerson` that accepts a `GetPersonRequest` and returns a `GetPersonResponse` which are the messages that are going to "flow through the wire", and in this case we are choosing ProtoBuffer for serializing:

```scala
final case class Person(name: String, age: Int)

@message
final case class GetPersonRequest(name: String)

@message
final case class GetPersonResponse(person: Person)

@service(Protobuf) trait PeopleService[F[_]] {
  def getPerson(request: GetPersonRequest): F[GetPersonResponse]
}
```

#### Server

The server tackles mainly a couple of purposes: To run the RPC server and provide an interpreter to the service defined in the protocol.

```scala
├── server
│   └── src
│       └── main
│           ├── resources
│           │   └── logback.xml
│           └── scala
│               ├── PeopleServiceHandler.scala
│               ├── ServerApp.scala
│               └── ServerBoot.scala
```

**_PoepleServiceHandler.scala_**

This is the interpretation of the protocol `PeopleService`. In this case, the `getPerson` operation returns a trivial Person with the same name passed in the request.

```scala
class PeopleServiceHandler[F[_]: Sync](implicit L: Logger[F]) extends PeopleService[F] {

  override def getPerson(request: GetPersonRequest): F[GetPersonResponse] =
    L.info(s"PeopleService - Request: $request").as(GetPersonResponse(Person(request.name, 10)))
}
```

**_ServerBoot.scala_**

This streaming app instantiates the logger and uses them to run the RPC server.

```scala
abstract class ServerBoot[F[_]: Effect] extends StreamApp[F] {

  implicit val S: Scheduler = monix.execution.Scheduler.Implicits.global

  override def stream(args: List[String], requestShutdown: F[Unit]): Stream[F, StreamApp.ExitCode] =
    for {
      logger <- Stream.eval(Slf4jLogger.fromName[F]("Server"))
      exitCode <- serverStream(logger)
    } yield exitCode

  def serverStream(implicit L: Logger[F]): Stream[F, StreamApp.ExitCode]
}

```

**_ServerApp.scala_**

The implementation of the `serverStream` leverages the features of **GrpcServer** to deal with servers.

```scala
class ServerProgram[F[_]: Effect] extends ServerBoot[F] {

  override def serverStream(implicit L: Logger[F]): Stream[F, StreamApp.ExitCode] = {
    implicit val PS: PeopleService[F] = new PeopleServiceHandler[F]
    val port = 19683
    val grpcConfigs: List[GrpcConfig] = List(AddService(PeopleService.bindService[F]))
    Stream.eval(for {
      server <- GrpcServer.default[F](port, grpcConfigs)
      _ <- L.info(s"Starting server at localhost:$port")
      exitCode <- GrpcServer.server(server).as(StreamApp.ExitCode.Success)
    } yield exitCode)

  }
}

object ServerApp extends ServerProgram[IO]
```

#### Client

In this initial version of the client, it just runs a client for the `PeopleService` and it injects it in the streaming flow of the app.

```scala
├── client
│   └── src
│       └── main
│           ├── resources
│           │   └── logback.xml
│           └── scala
│               ├── ClientApp.scala
│               ├── ClientRPC.scala
│               └── PeopleServiceClient.scala
```

**_PeopleServiceClient.scala_**

This algebra is the via to connect to the server through the RPC client, using some Freestyle-RPC magic.

```scala
trait PeopleServiceClient[F[_]] {
  def getPerson(name: String): F[Person]
}

object PeopleServiceClient {

  def apply[F[_]: Effect](clientF: F[PeopleService.Client[F]])
  (implicit L: Logger[F]): PeopleServiceClient[F] = new PeopleServiceClient[F] {
      def getPerson(name: String): F[Person] = ???
    }

  def createClient[F[_]](hostname: String,
                         port: Int,
                         sslEnabled: Boolean = true,
                         tryToRemoveUnusedEvery: FiniteDuration,
                         removeUnusedAfter: FiniteDuration)(
      implicit F: Effect[F],
      L: Logger[F],
      TM: Timer[F],
      S: Scheduler): fs2.Stream[F, PeopleServiceClient[F]] =  ???

}
```

**_ClientRPC.scala_**

This object provides an RPC client for a given tuple of host and port. It's used in `PeopleServiceClient`.

**_ClientApp.scala_**

Similar to `ServerBoot`, this app instantiates the logger, the RPC client and it calls to `getPerson` as soon as it starts running.

```scala
object ClientApp {

  implicit val S: Scheduler = monix.execution.Scheduler.Implicits.global

  implicit val TM = Timer.derive(Effect[IO], IO.timer(S))

  def main(args: Array[String]): Unit = {
    (for {
      logger <- Stream.eval(Slf4jLogger.fromName[IO]("Client"))
      client <- {
        implicit val l = logger
        PeopleServiceClient.createClient[IO]("localhost", 19683)
      }
      getPersonResponse <- Stream.eval(client.getPerson("foo")).as(println)
    } yield (getPersonResponse)).compile.toVector.unsafeRunSync()

  }

}
```



<!-- DOCTOC SKIP -->
# Copyright

Freestyle-RPC is designed and developed by 47 Degrees

Copyright (C) 2017 47 Degrees. <http://47deg.com>

[comment]: # (End Copyright)
