package com.fortyseven.protocol

import freestyle.rpc.protocol._
import fs2.Stream

object services {

  case class Temperature(value: Double, unit: String)

  case class Location(lat: Double, long: Double)

  @message
  final case class IsEmptyRequest()

  @message
  final case class IsEmptyResponse(result: Boolean)

  @message
  final case class ComingBackModeResponse(result: Boolean)

  @service(Protobuf) trait SmartHomeService[F[_]] {

    def isEmpty(request: IsEmptyRequest): F[IsEmptyResponse]

    def getTemperature(empty: Empty.type): Stream[F, Temperature]

    def comingBackMode(request: Stream[F, Location]): F[ComingBackModeResponse]

  }

}
