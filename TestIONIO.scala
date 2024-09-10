package com.perikov.threadtest.ionio
import java.nio.channels.*
import java.net.InetSocketAddress
import com.perikov.threadtest.*

private val addr = InetSocketAddress("localhost", Constants.listenPort)

private val server = 
  val chan = ServerSocketChannel.open().bind(addr,30)
  val con = chan.accept()
  con.configureBlocking(false)
  val selector = Selector.open()
  chan.register(selector, SelectionKey.OP_READ)




def testIONIOUnsafe(numStreams: Int): Unit = ???

