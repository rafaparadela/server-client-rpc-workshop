package com.fortyseven.server

import cats.effect.Sync
import cats.syntax.functor._
import com.fortyseven.protocol.services._
import io.chrisdavenport.log4cats.Logger

class PeopleServiceHandler[F[_]: Sync](implicit L: Logger[F])
    extends PeopleService[F] {

  override def getPerson(request: GetPersonRequest): F[GetPersonResponse] =
    L.info(s"PeopleService - Request: $request")
      .as(GetPersonResponse(Person(request.name, 10)))

}
