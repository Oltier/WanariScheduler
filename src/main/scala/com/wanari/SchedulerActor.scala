package com.wanari

import akka.actor.Actor
import com.wanari.SchedulerActor._


class SchedulerActor extends Actor {
	def receive = {
		case ScheduledRunStart => {}
		case ScheduledRunStop => {}
		case ManualRun => {}
		case TaskList => {}
		case _ => {}
	}

}

object SchedulerActor {

	case class ScheduledRunStart()

	case class ScheduledRunStop()

	case class ManualRun(taskId: String)

	case class TaskList(taskList: Seq[Task])

	case class TaskListRequest()

	case class SysError(msg: String, ex: Exception = null)

	case class TaskStarted(taskId: String)

	case class TaskFinished(taskId: String)

	case class TaskFailed(taskId: String, msg: String = "", ex: Exception = null)

	case class ExecuteTask(taskId: String, param: String)

}
