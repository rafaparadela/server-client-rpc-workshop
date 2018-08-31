package com.fortyseven.server

import cats.effect.Sync
import com.fortyseven.protocol.services.Temperature
import fs2.Stream

import scala.util.Random

object TemperaturesGenerators {

  val seed = Temperature(25d, "Celsius")

  def get[F[_]: Sync]: Stream[F, Temperature] = {
    Stream.iterateEval(seed) { t =>
      println(s"* New Temperature 👍  --> $t")
      nextTemperature(t)
    }
  }

  def nextTemperature[F[_]](current: Temperature)(
      implicit F: Sync[F]): F[Temperature] = F.delay {
    Thread.sleep(2000)
    val increment: Double = Random.nextDouble() / 2d
    val signal = if (Random.nextBoolean()) 1 else -1
    val currentValue = current.value
    val nextValue = currentValue + (signal * increment)
    current.copy(value = (math rint nextValue * 100) / 100)
  }
}
