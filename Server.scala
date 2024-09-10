package com.perikov.threadtest

import java.util.concurrent.ThreadFactory

class Server(sock: java.net.Socket, name: String)(using
    threadFactory: ThreadFactory
):
  val thread = threadFactory.newThread { () =>
    val in = sock.getInputStream()
    while true do
      val buf = new Array[Byte](Constants.receiveBufferSize)
      val bytesRead = in.read(buf)
      Statistics.addData(bytesRead)
  }
  thread.setName(s"Server $name")
