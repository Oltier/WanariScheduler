package com.wanari

import akka.actor.Actor
import com.wanari.SchedulerActor.{ExecuteTask, TaskFailed, TaskFinished}

import scala.util.Try

abstract class TaskActor extends Actor {
	override final def receive: Receive = {
		case x: ExecuteTask => {
			try {
				onReceive(x.param)
				sender ! TaskFinished(x.taskId)
			} catch {
				case e: Exception => sender ! TaskFailed(x.taskId, "", e)
			}
		}
		case x: Any => {
			onReceive(x)
		}
	}

	def onReceive: Receive
}
