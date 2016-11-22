package com.wanari.scheduler

import scala.collection.JavaConverters._


abstract class SchedulerJavaDispatcher extends SchedulerDispatcher {
  override def getAllTask(): Seq[Task] = getAllTaskAsJava().asScala

  def getAllTaskAsJava(): java.util.List[Task]
}
