package com.wanari.scheduler

import akka.actor.Actor
import com.wanari.scheduler.SchedulerMessages._

abstract class TaskActor extends Actor {
  override final def receive: Receive = {
    case x: ExecuteTask =>
      try {
        onReceive(x.param)
        sender ! TaskFinished(x.taskId)
      } catch {
        case e: Exception => sender ! TaskFailed(x.taskId, "", e)
      }
    case x: Any =>
      onReceive(x)
  }

  def onReceive: Receive
}
