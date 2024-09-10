package com.perikov.threadtest.fs2async
import com.perikov.threadtest.{Constants, Statistics}
import cats.effect.*
import com.comcast.ip4s.*
import fs2.io.net.*
import scala.concurrent.duration.*

private val port: Port = Port.fromInt(Constants.listenPort).get

val acceptor: fs2.Stream[IO, Socket[IO]] = Network[IO].server(port = Some(port))

def socketProcessor(s: Socket[IO]): fs2.Stream[IO, Unit] =
  fs2.Stream
    .repeatEval(s.read(Constants.receiveBufferSize).map(_.get))
    .map(chunk => Statistics.addData(chunk.size))

def client(n: Long): fs2.Stream[IO, Nothing] =
  fs2.Stream
    .resource(
      Network[IO]
        .client(SocketAddress(host"localhost", port))
    )
    .flatMap: sock =>
      println(s"Starting client $n")

      fs2.Stream
        .awakeDelay[IO](Constants.sendDelayMillis.milli)
        .evalMap: _ =>
          val msg = Constants.generateMessage()

          sock.write(fs2.Chunk.array(msg))
        .drain

val serverProcessing: fs2.Stream[IO, Unit] =
  acceptor.map(socketProcessor).parJoinUnbounded

def clientStart(numStreams: Int): fs2.Stream[IO, fs2.Stream[IO, Nothing]] =
  fs2.Stream
    .awakeEvery[IO](Constants.clientStartDelayMillis.milli)
    .take(numStreams)
    .zipWithIndex
    .map(t => client(t._2))
    .parJoinUnbounded

def testFS2Unsafe(numStreams: Int) =
  import cats.effect.unsafe.implicits.global
  serverProcessing
    .concurrently(clientStart(numStreams))
    .compile
    .drain
    .unsafeRunSync()
