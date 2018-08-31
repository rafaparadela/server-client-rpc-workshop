package com.fortyseven.protocol

import freestyle.rpc.protocol._

object services {

  @message
  final case class IsEmptyRequest()

  @message
  final case class IsEmptyResponse(result: Boolean)

  @service(Protobuf) trait SmartHomeService[F[_]] {

    def isEmpty(request: IsEmptyRequest): F[IsEmptyResponse]

  }

}
