package com.perikov.threadtest



object Statistics:
  import java.util.concurrent.atomic.AtomicLong
  private val dataReceived = AtomicLong(0)
  private var lastTime = System.nanoTime()
  private var lastData = 0L
  private def printPerformance() =
    val now = System.nanoTime()
    val curData = dataReceived.get()
    val secondsDelta = (now - lastTime) / 1e9
    val rate = (curData - lastData).toDouble / (1024 * 1024) / secondsDelta
    lastTime = now
    lastData = curData
    println(s"$rate MiB per second")


  private val thread = new Thread("Data printer"):
    override def run() =
      while true do
        printPerformance()
        Thread.sleep(1000)

  thread.start()

  def addData(size: Int) = 
    dataReceived.accumulateAndGet(size, _ + _)
    ()
end Statistics