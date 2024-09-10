package com.perikov.threadtest

/** This is purely NIO implementatoin with native threads
  */
object PureNIONative:
  def test(numStreams: Int): Unit = ???
end PureNIONative

@main
def studyThreadStackSize =
  val numThreads = 1000
  val stackSize = 4096
  val waitMillis = 100_000
  val threads = Seq.fill(numThreads)(
    Thread
      .ofPlatform()
      .stackSize(stackSize)
      .start(() => Thread.sleep(waitMillis))
  )
  println(s"Started $numThreads threads with stacks of $stackSize bytes, wait millis = $waitMillis")
  threads.zipWithIndex.foreach:(t,i) =>
    t.join()
    println(s"Joined thread $i")

