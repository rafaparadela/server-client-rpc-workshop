package com.fortyseven.client

import cats.effect._
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.fortyseven.protocol.services._
import freestyle.rpc.protocol.Empty
import fs2.Stream
import io.grpc.{CallOptions, ManagedChannel}
import monix.execution.Scheduler
import io.chrisdavenport.log4cats.Logger

import scala.concurrent.duration._

trait SmartHomeServiceClient[F[_]] {

  def isEmpty(): F[Boolean]

  def getTemperature(): Stream[F, Temperature]

  def comingBackMode(locations: Stream[F, Location]): F[Boolean]

}
object SmartHomeServiceClient {

  def apply[F[_]: Effect](clientF: F[SmartHomeService.Client[F]])(
      implicit L: Logger[F]): SmartHomeServiceClient[F] =
    new SmartHomeServiceClient[F] {

      def isEmpty(): F[Boolean] =
        for {
          client <- clientF
          response <- client.isEmpty(IsEmptyRequest())
          _ <- L.info(s"Result: $response")
        } yield response.result

      def getTemperature(): Stream[F, Temperature] =
        for {
          client <- Stream.eval(clientF)
          response <- client.getTemperature(Empty)
          _ <- Stream.eval(L.info(s"Result: $response"))
        } yield response

      def comingBackMode(locations: Stream[F, Location]): F[Boolean] =
        for {
          client <- clientF
          response <- client.comingBackMode(locations)
          _ <- L.info(s"Result: $response")
        } yield response.result

    }

  def createClient[F[_]](hostname: String,
                         port: Int,
                         sslEnabled: Boolean = false,
                         tryToRemoveUnusedEvery: FiniteDuration = 30 minutes,
                         removeUnusedAfter: FiniteDuration = 1 hour)(
      implicit F: Effect[F],
      L: Logger[F],
      TM: Timer[F],
      S: Scheduler): fs2.Stream[F, SmartHomeServiceClient[F]] = {

    def fromChannel(channel: ManagedChannel): SmartHomeService.Client[F] =
      SmartHomeService.clientFromChannel(channel, CallOptions.DEFAULT)

    ClientRPC
      .clientCache((hostname, port).pure[F],
                   sslEnabled,
                   tryToRemoveUnusedEvery,
                   removeUnusedAfter,
                   fromChannel)
      .map(cache => SmartHomeServiceClient(cache.getClient))
  }

}
