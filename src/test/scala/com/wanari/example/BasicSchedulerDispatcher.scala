package com.wanari.example

import com.wanari.{Task, SchedulerDispatcher}

/**
 * Created by torcsi on 07/11/2016.
 */
class BasicSchedulerDispatcher extends SchedulerDispatcher{
	override def getAllTask(): Seq[Task] = Seq.empty[Task]
}
