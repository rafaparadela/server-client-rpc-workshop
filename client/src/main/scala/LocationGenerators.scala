package com.fortyseven.client

import cats.effect.Sync
import com.fortyseven.protocol.services._
import fs2.Stream

import scala.util.Random

object LocationGenerators {

  val seed = Location(47d, 47d)

  def get[F[_]: Sync]: Stream[F, Location] = {
    Stream.iterateEval(seed) { l =>
      println(s"* New Location 👍  --> $l")
      nextLocation(l)
    }
  }

  def nextLocation[F[_]](current: Location)(implicit F: Sync[F]): F[Location] =
    F.delay {
      Thread.sleep(3000)
      Location(closeLocation(current.lat), closeLocation(current.long))
    }

  def closeLocation(d: Double): Double = {
    val increment: Double = Random.nextDouble() / 10d
    val signal = if (Random.nextBoolean()) 1 else -1
    d + (signal * increment)
  }
}
