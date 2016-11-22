package com.wanari.scheduler

import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef, Cancellable}
import com.cronutils.model.CronType
import com.cronutils.model.definition.{CronDefinition, CronDefinitionBuilder}
import com.cronutils.model.time.ExecutionTime
import com.cronutils.parser.CronParser
import com.wanari.scheduler.SchedulerMessages._

import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration

class SchedulerActor(
                      dispatcher: ActorRef,
                      autoRefreshFrequency: FiniteDuration = FiniteDuration(5, TimeUnit.MINUTES)
                    ) extends Actor with akka.actor.ActorLogging {

  var schedulerEnabled: Boolean = false
  var scheduledAutoRefresh: Cancellable = null

  var innerTasks: mutable.Map[String, Task] = new mutable.HashMap[String, Task]()
  var scheduledTaskMessages: mutable.Map[String, Cancellable] = new mutable.HashMap[String, Cancellable]()
  var runningTaskIds: mutable.Set[String] = new mutable.HashSet[String]()

  def receive = {
    case ScheduledRunStart if !schedulerEnabled => startScheduledRunning()
    case ScheduledRunStart if schedulerEnabled => error("ScheduledRun already started!")

    case ScheduledRunStop if schedulerEnabled => stopScheduledRunning()
    case ScheduledRunStop if !schedulerEnabled => error("ScheduledRun already stopped!")

    case x: TaskList => refreshTaskList(x.taskList)

    case x: ManualRun => manualRunProcess(x)

    case x: TaskFinished => taskFinishedProcess(x)

    case x: TaskFailed => taskFailedProcess(x)

    case x: ExecuteTask => error("ExecuteTask message send only TaskActor!")

    case x: AnyRef => log.debug("Received an unknown message! " + x.toString)
  }

  override def postStop(): Unit = {
    log.warning("SchedulerActor stopped.")
    stopScheduledRunning()
  }

  def startScheduledRunning(): Unit = {
    log.info("Scheduled task run enabled.")
    schedulerEnabled = true

    log.debug("Task list refresh every " + autoRefreshFrequency.toSeconds + " seconds.")
    scheduledAutoRefresh = context.system.scheduler.schedule(
      FiniteDuration(0, TimeUnit.MINUTES),
      autoRefreshFrequency,
      dispatcher, TaskListRequest)(context.system.dispatcher, self)

    log.debug("Schedule all task: " + innerTasks.keySet.mkString(", "))
    innerTasks.values.foreach(scheduleTask(_, ZonedDateTime.now()))
  }

  def stopScheduledRunning(): Unit = {
    log.info("Scheduled task run disabled.")
    schedulerEnabled = false

    log.debug("Task list refresh stopped.")
    if (scheduledAutoRefresh != null)
      scheduledAutoRefresh.cancel()

    cancelAllScheduledTaskMessage()
  }

  def refreshTaskList(taskList: Seq[Task]): Unit = {
    log.debug("Task list refresh.")

    val removed = SchedulerActor.findRemovedTaskIds(innerTasks.toMap, taskList)
    log.info("Remove tasks: " + removed.mkString(", "))
    removed.foreach(removeTaskProcess(_))

    val changed = SchedulerActor.findChangedTask(innerTasks.toMap, taskList)
    log.info("New or changed task: " + changed.map(_.getId()).mkString(", "))
    changed.foreach(updateOrSaveTaskProcess(_))

    log.debug("Task list refresh finished.")
  }

  def manualRunProcess(x: ManualRun): Unit = {
    log.debug("Task manual running. TaskID=[" + x.taskId + "]")
    findTaskById(x.taskId) match {
      case Some(task) if !isRunningTask(task.getId()) =>
        sendTaskToActor(task)
        log.info("Task manual running. Task: " + SchedulerActor.taskToString(task))
      case Some(task) if isRunningTask(task.getId()) => error("This with given id (" + x.taskId + ") already running now! Please wait until finished.")
      case None => error("Task with given id (" + x.taskId + ") not found! Please refresh TaskList before call this method!")
    }
  }

  def taskFinishedProcess(x: TaskFinished): Unit = {
    log.debug("Task finished. TaskID=[" + x.taskId + "]")
    removeFromRunningTasks(x.taskId)
    dispatcher ! x
    scheduleTaskProcess(x.taskId, ZonedDateTime.now)
  }

  def taskFailedProcess(x: TaskFailed): Unit = {
    log.debug("Task failed. TaskID=[" + x.taskId + "]")
    removeFromRunningTasks(x.taskId)
    dispatcher ! x
    scheduleTaskProcess(x.taskId, ZonedDateTime.now)
  }

  def scheduledSendingProcess(taskId: String): Unit = {
    log.debug("Scheduled sending. TaskID=[" + taskId + "]")
    findTaskById(taskId) match {
      case Some(task) if schedulerEnabled && task.isEnabled() => sendTaskToActor(task)
      case None => log.error("Scheduled sending task not found. TaskID=[" + taskId + "]")
    }
  }

  def scheduleTaskProcess(taskId: String, time: ZonedDateTime): Unit =
    findTaskById(taskId) match {
      case Some(task) if task.isEnabled() && schedulerEnabled => scheduleTask(task, time)
      case Some(task) if !(task.isEnabled() && schedulerEnabled) => log.debug("ScheduleTaskProcess not scheduled because task is disabled. TaskID=[" + taskId + "]")
      case None => log.debug("ScheduleTaskProcess not found task. TaskID=[" + taskId + "]")
    }

  def scheduleTask(task: Task, time: ZonedDateTime): Unit = {
    val delay = SchedulerActor.getNextRunDuration(task.getCron(), time)
    if (delay == null) log.info("ScheduleTask failed. Wrong cron format. TaskID=[" + task.getId() + "]")
    else {
      val scheduledMessage = context.system.scheduler.scheduleOnce(delay)(scheduledSendingProcess(task.getId()))(context.system.dispatcher)
      addScheduledTaskMessage(task.getId() -> scheduledMessage)
      log.debug("Create scheduled task message after " + delay.toSeconds + " seconds Task: " + SchedulerActor.taskToString(task) + ".")
    }
  }

  def sendTaskToActor(task: Task): Unit = {
    if (!isRunningTask(task.getId())) {
      log.debug("Send ExecuteTask message to Actor. Task: " + SchedulerActor.taskToString(task))
      addToRunningTasks(task.getId())
      context.actorSelection(task.getActorName()) ! ExecuteTask(task.getId(), task.getParam())
      dispatcher ! TaskStarted(task.getId())
    } else {
      log.debug("SendTaskToActor task already running. TaskID=[" + task.getId() + "]")
    }
  }

  def updateOrSaveTaskProcess(task: Task): Unit = {
    log.debug("UpdateOrSave task: " + SchedulerActor.taskToString(task))
    val time = ZonedDateTime.now
    cancelScheduledTaskMessage(task.getId())
    if (!isValidCron(task.getCron())) error("Wrong cron format. TaskID=[" + task.getId() + "]")
    updateOrSaveTask(task)
    scheduleTaskProcess(task.getId(), time)
  }

  def isValidCron(cron: String): Boolean = SchedulerActor.getNextRunDuration(cron, ZonedDateTime.now()) != null

  def updateOrSaveTask(task: Task): Unit = {
    findTaskById(task.getId()) match {
      case Some(_) =>
        innerTasks(task.getId()) = task
        log.debug("Update task: " + SchedulerActor.taskToString(task))
      case None =>
        innerTasks += (task.getId() -> task)
        log.debug("Save task: " + SchedulerActor.taskToString(task))
    }
  }

  def removeTaskProcess(taskId: String): Unit = {
    log.debug("Remove task: " + taskId)
    cancelScheduledTaskMessage(taskId)
    innerTasks -= taskId
  }

  def addScheduledTaskMessage(task: (String, Cancellable)): Unit = {
    cancelScheduledTaskMessage(task._1)
    log.debug("Save scheduled task message. TaskID=[" + task._1 + "]")
    scheduledTaskMessages += task
  }

  def cancelScheduledTaskMessage(taskId: String): Unit = {
    scheduledTaskMessages.get(taskId) match {
      case Some(cancellable) =>
        if (cancellable.cancel()) {
          log.debug("Cancel scheduled task message. TaskID=[" + taskId + "]")
        }
        scheduledTaskMessages -= taskId
      case None =>
    }
  }

  def cancelAllScheduledTaskMessage(): Unit = {
    log.debug("Cancel all scheduled task message.")
    scheduledTaskMessages.values.foreach(_.cancel())
    scheduledTaskMessages.clear()
  }

  def isRunningTask(taskId: String): Boolean = runningTaskIds.contains(taskId)

  def addToRunningTasks(taskId: String): Unit = runningTaskIds += taskId

  def removeFromRunningTasks(taskId: String): Unit = runningTaskIds -= taskId

  def findTaskById(taskId: String): Option[Task] = innerTasks.get(taskId)

  def error(message: String): Unit = {
    log.warning(message)
    dispatcher ! SysError(message)
  }

}

object SchedulerActor {

  def taskToString(task: Task): String = {
    "id=[" + task.getId() + "] " +
      "actor=[" + task.getActorName() + "] " +
      "cron=[" + task.getCron() + "] " +
      "param=[" + task.getParam() + "] " +
      "enabled=[" + task.isEnabled() + "]"
  }

  def findRemovedTaskIds(original: Map[String, Task], newTasks: Seq[Task]) = {
    original.keySet -- newTasks.map(_.getId()).toSet
  }

  def findChangedTask(original: Map[String, Task], newTasks: Seq[Task]) = {
    newTasks.filter(newTask => {
      original.get(newTask.getId()) match {
        case Some(oldTask) =>
          !Task.same(newTask, oldTask)
        case None =>
          true
      }
    })
  }

  def getNextRunDuration(cron: String, time: ZonedDateTime): FiniteDuration = {
    try {
      val cronDef: CronDefinition = CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ)
      val parser: CronParser = new CronParser(cronDef)
      val executionTime: ExecutionTime = ExecutionTime.forCron(parser.parse(cron))
      val duration = executionTime.timeToNextExecution(time)
      FiniteDuration(duration.toNanos, TimeUnit.NANOSECONDS)
    } catch {
      case _: Throwable => null
    }
  }
}
