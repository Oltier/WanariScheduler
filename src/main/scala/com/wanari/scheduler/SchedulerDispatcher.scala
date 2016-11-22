package com.wanari.scheduler

import akka.actor.Actor
import com.wanari.scheduler.SchedulerMessages._

abstract class SchedulerDispatcher extends Actor {
  def receive: Receive = {
    case TaskListRequest => sender ! TaskList(getAllTask())

    case x: TaskStarted => taskStarted(x.taskId)
    case x: TaskFinished => taskFinished(x.taskId)
    case x: TaskFailed => taskFailed(x.taskId, x.msg, x.ex)

    case x: SysError => sysError(x.msg, x.ex)
  }

  def getAllTask(): Seq[Task]

  def taskStarted(taskId: String) = {}

  def taskFinished(taskId: String) = {}

  def taskFailed(taskId: String, msg: String, ex: Exception) = {}

  def sysError(msg: String, ex: Exception) = {}
}
