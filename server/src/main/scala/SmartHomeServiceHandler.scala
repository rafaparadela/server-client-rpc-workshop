package com.fortyseven.server

import cats.effect.Sync
import cats.syntax.functor._
import com.fortyseven.protocol.services._
import freestyle.rpc.protocol.Empty
import fs2.Stream
import io.chrisdavenport.log4cats.Logger

import scala.util.Random

class SmartHomeServiceHandler[F[_]: Sync](implicit L: Logger[F])
    extends SmartHomeService[F] {

  override def isEmpty(request: IsEmptyRequest): F[IsEmptyResponse] =
    L.info(s"SmartHomeService - Request: $request").as(IsEmptyResponse(true))

  override def getTemperature(empty: Empty.type): Stream[F, Temperature] =
    for {
      _ <- Stream.eval(L.info(s"SmartHomeService - getTemperature Request"))
      temperatures <- TemperaturesGenerators.get[F]
    } yield temperatures

  override def comingBackMode(
      request: Stream[F, Location]): F[ComingBackModeResponse] = {
    (for {
      _ <- Stream.eval(L.info(s"SmartHomeService - comingBackMode Request"))
      _ <- request.attempt.map { l =>
        println(getActions)
      }
    } yield ()).compile.drain.map(_ => ComingBackModeResponse(true))
  }

  private def getActions: List[String] = {
    val ops = Seq(
      List("👮 - Enable Security cameras", "💡 - Turn off Lights"),
      List("😎 - Low the blinds", "📺 - Turn on TV"),
      List("🔥 - Increase thermostat power", "💦 -  Disable irrigation system"),
      List("🚪 - Unlock doors", "👩 - Connect Alexa")
    )
    ops(Random.nextInt(ops.length))
  }

}
