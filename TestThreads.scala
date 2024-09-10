package com.perikov.threadtest

import java.util.concurrent.ThreadFactory

def testNative(numThreads: Int)(using threadFactory: ThreadFactory): Unit =
  import java.net.ServerSocket
  val acceptor = threadFactory.newThread { () =>
    println("Started acceptor")
    val sock = ServerSocket(Constants.listenPort)
    while true do
      var cnt = 0L
      while true do
        cnt += 1
        val con = sock.accept()
        println(s"Accepted connection $cnt")
        Server(con, s"Server $cnt")
  }

  (1 to numThreads).foreach: n =>
    Client(s"Client $n")
    Thread.sleep(Constants.clientStartDelayMillis)
  acceptor.join()
end testNative

/** @see
  *   [yellow, red and
  *   others](https://stackoverflow.com/questions/25309748/what-is-thread-stack-size-option-xss-given-to-jvm-why-does-it-have-a-limit-of)
  *
  * @see
  *   [Stack zones explained](https://pangin.pro/posts/stack-overflow-handling)
  */
object Main:
  val nativeTH: ThreadFactory = Thread.ofPlatform().start(_)
  val loomTH: ThreadFactory = Thread.ofVirtual().start(_)
  def main(args: Array[String]): Unit =
    import scala.util.control.NonFatal

    val methods: Map[String, Int => Unit] = Map(
      "fs2" -> (n => fs2async.testFS2Unsafe(n)),
      "io" -> (n => io.testIOUnsafe(n)),
      "ionio" -> (n => ionio.testIONIOUnsafe(n)),
      "native" -> (n => testNative(n)(using nativeTH)),
      "loom" -> (n => testNative(n)(using loomTH))
    )

    val (numThreads, methodName, runner) =
      try
        val nt = args(0).toInt
        val mn = args(1)
        (nt, mn, methods(mn))
      catch
        case NonFatal(_) =>
          scala.sys.error(
            s"Usage: prog <num threads> <${methods.keys.mkString(" | ")}>"
          )

    println(
      s"Will measure thread performance for $numThreads threads, method: $methodName\n\n\n"
    )
    runner(numThreads)
