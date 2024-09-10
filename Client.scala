package com.perikov.threadtest

import java.util.concurrent.ThreadFactory

class Client(name: String)(using threadFactory: ThreadFactory):
  val sender = threadFactory.newThread { () =>
    println(s"Started client $name")
    import java.net.*
    val sock = Socket("localhost", Constants.listenPort)
    val out = sock.getOutputStream()
    while true do
      val msg = Constants.generateMessage()
      out.write(msg)
      Thread.sleep(Constants.sendDelayMillis)
  }
  sender.setName(name)
