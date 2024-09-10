package com.perikov.threadtest

object Constants:
  val listenPort = 2321
  val sendDelayMillis = 50
  val clientStartDelayMillis = 20
  val msgRepeatCount = 3000
  val receiveBufferSize = 8192
  def generateMessage() = 
    new Array[Byte](3000 * 9)

