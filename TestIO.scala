package com.perikov.threadtest.io
import com.perikov.threadtest.*
import java.net.*
import cats.effect.*
import concurrent.duration.*
import cats.implicits.*

def processor(sock: Socket): IO[Nothing] =
  println("Started processor")
  val in = sock.getInputStream()
    
  IO.blocking:
      val buf = new Array[Byte](8192)
      Statistics.addData(in.read(buf))
  .foreverM
end processor

def client(name: String): IO[Nothing] =
  IO.println(s"Starting client", name) >>
    IO.blocking(Socket("localhost", Constants.listenPort))
      .map(_.getOutputStream())
      .flatMap(out =>
        IO.println("Started client " + name) >>
          IO.blocking{out.write(Constants.generateMessage())}
            .andWait(Constants.sendDelayMillis.milli)
            .foreverM
      )

val server: IO[Nothing] =
  val serverSock = IO.blocking(ServerSocket(Constants.listenPort))
  serverSock.flatMap: server =>
    IO.blocking { server.accept() }
      .map(processor)
      .flatMap(_.start)
      .foreverM

def testIOUnsafe(numStreams: Int): Unit =
  import unsafe.implicits.global
  val testing =
    server.start >>
      (1 to numStreams).toVector
        .map(_.toString())
        .map(client)
        .map(_.start.andWait(Constants.clientStartDelayMillis.milli))
        .sequence_
      >> IO.never

  testing.unsafeRunSync()
