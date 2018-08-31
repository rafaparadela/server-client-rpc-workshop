package com.fortyseven.server

import cats.effect.Sync
import cats.syntax.functor._
import com.fortyseven.protocol.services._
import io.chrisdavenport.log4cats.Logger

class SmartHomeServiceHandler[F[_]: Sync](implicit L: Logger[F])
    extends SmartHomeService[F] {

  override def isEmpty(request: IsEmptyRequest): F[IsEmptyResponse] =
    L.info(s"SmartHomeService - Request: $request").as(IsEmptyResponse(true))

}
