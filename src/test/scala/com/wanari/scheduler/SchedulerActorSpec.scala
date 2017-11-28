package com.wanari.scheduler

import java.util.concurrent.TimeUnit

import akka.actor.{ActorSystem, Cancellable}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormatter
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration.FiniteDuration

class SchedulerActorSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("MySpec"))

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "SchedulerActor" must {
    "findRemovedTaskIds" in {
      val ret: Set[String] = SchedulerActor.findRemovedTaskIds(oldTasks, newTask)
      val expected: Seq[String] = Seq("ID7", "ID6")
      ret.size shouldBe expected.size
      (ret -- expected).isEmpty shouldBe true
    }

    "findChangedTask" in {
      val ret: Seq[Task] = SchedulerActor.findChangedTask(oldTasks, newTask)
      val expected: Seq[Task] = Seq(
        newTask.find(_.id == "ID2").get,
        newTask.find(_.id == "ID3").get,
        newTask.find(_.id == "ID4").get,
        newTask.find(_.id == "ID5").get,
        newTask.find(_.id == "ID8").get,
        newTask.find(_.id == "ID9").get
      )
      ret.size shouldBe expected.size
      ret.exists(!expected.contains(_)) shouldBe false
    }

    "getNextRunDuration daily" in {
      val time = DateTime.parse("2016-11-08T10:14:00.000Z")
      val cron = "0 0 12 1/1 * ? *" // Every days 12:00:00 -> next 2016-11-08T12:00:00.000Z
      val ret: FiniteDuration = SchedulerActor.getNextRunDuration(cron, time)
      val exp: FiniteDuration = FiniteDuration(106, TimeUnit.MINUTES)
      ret shouldBe exp
    }

    "getNextRunDuration 5 mins" in {
      val time = DateTime.parse("2016-11-08T10:14:00.000Z")
      val cron = "0 0/5 * 1/1 * ? *" // Every 5. min -> next 2016-11-08T10:15:00.000Z
      val ret: FiniteDuration = SchedulerActor.getNextRunDuration(cron, time)
      val exp: FiniteDuration = FiniteDuration(1, TimeUnit.MINUTES)
      ret shouldBe exp
    }

    "running task ids add" in {
      val scheduler = TestActorRef(new SchedulerActor(null)).underlyingActor
      val taskID = "taskID1"
      scheduler.isRunningTask(taskID) shouldBe false
      scheduler.addToRunningTasks(taskID)
      scheduler.isRunningTask(taskID) shouldBe true
    }

    "running task ids remove" in {
      val scheduler = TestActorRef(new SchedulerActor(null)).underlyingActor
      val taskID = "taskID1"
      scheduler.addToRunningTasks(taskID)
      scheduler.removeFromRunningTasks(taskID)
      scheduler.isRunningTask(taskID) shouldBe false
    }

    "running task ids multiple add, remove" in {
      val scheduler = TestActorRef(new SchedulerActor(null)).underlyingActor
      val taskID = "taskID1"
      scheduler.addToRunningTasks(taskID)
      scheduler.addToRunningTasks(taskID)
      scheduler.isRunningTask(taskID) shouldBe true
      scheduler.removeFromRunningTasks(taskID)
      scheduler.isRunningTask(taskID) shouldBe false
    }

    "scheduled task message add" in {
      val taskID = "taskID1"
      val scheduledTaskMessageMock = new Cancellable {
        var flag: Boolean = false

        override def isCancelled: Boolean = flag

        override def cancel(): Boolean = {
          flag = true
          flag
        }
      }
      val scheduler = TestActorRef(new SchedulerActor(null)).underlyingActor
      scheduler.scheduledTaskMessages.size shouldBe 0
      scheduler.addScheduledTaskMessage(taskID -> scheduledTaskMessageMock)
      scheduler.scheduledTaskMessages.size shouldBe 1
      scheduler.scheduledTaskMessages.get(taskID).get shouldBe scheduledTaskMessageMock
    }

    "scheduled task message cancel" in {
      val taskID = "taskID1"
      val scheduledTaskMessageMock = new Cancellable {
        var flag: Boolean = false

        override def isCancelled: Boolean = flag

        override def cancel(): Boolean = {
          flag = true
          flag
        }
      }
      val taskID2 = "taskID2"
      val scheduledTaskMessageMock2 = new Cancellable {
        var flag: Boolean = false

        override def isCancelled: Boolean = flag

        override def cancel(): Boolean = {
          flag = true
          flag
        }
      }
      val scheduler = TestActorRef(new SchedulerActor(null)).underlyingActor
      scheduler.addScheduledTaskMessage(taskID -> scheduledTaskMessageMock)
      scheduler.addScheduledTaskMessage(taskID2 -> scheduledTaskMessageMock2)
      scheduler.cancelScheduledTaskMessage(taskID)
      scheduler.scheduledTaskMessages.size shouldBe 1
      scheduler.scheduledTaskMessages.get(taskID2).get shouldBe scheduledTaskMessageMock2
      scheduledTaskMessageMock.isCancelled shouldBe true
      scheduledTaskMessageMock2.isCancelled shouldBe false
    }

    "scheduled task message cancel all" in {
      val taskID = "taskID1"
      val scheduledTaskMessageMock = new Cancellable {
        var flag: Boolean = false

        override def isCancelled: Boolean = flag

        override def cancel(): Boolean = {
          flag = true
          flag
        }
      }
      val taskID2 = "taskID2"
      val scheduledTaskMessageMock2 = new Cancellable {
        var flag: Boolean = false

        override def isCancelled: Boolean = flag

        override def cancel(): Boolean = {
          flag = true
          flag
        }
      }
      val scheduler = TestActorRef(new SchedulerActor(null)).underlyingActor
      scheduler.addScheduledTaskMessage(taskID -> scheduledTaskMessageMock)
      scheduler.addScheduledTaskMessage(taskID2 -> scheduledTaskMessageMock2)
      scheduler.cancelAllScheduledTaskMessage()
      scheduler.scheduledTaskMessages.size shouldBe 0
      scheduledTaskMessageMock.isCancelled shouldBe true
      scheduledTaskMessageMock2.isCancelled shouldBe true
    }

    "scheduled task message multiple add" in {
      val taskID = "taskID1"
      val scheduledTaskMessageMock = new Cancellable {
        var flag: Boolean = false

        override def isCancelled: Boolean = flag

        override def cancel(): Boolean = {
          flag = true
          flag
        }
      }
      val scheduledTaskMessageMock2 = new Cancellable {
        var flag: Boolean = false

        override def isCancelled: Boolean = flag

        override def cancel(): Boolean = {
          flag = true
          flag
        }
      }
      val scheduler = TestActorRef(new SchedulerActor(null)).underlyingActor
      scheduler.addScheduledTaskMessage(taskID -> scheduledTaskMessageMock)
      scheduler.addScheduledTaskMessage(taskID -> scheduledTaskMessageMock2)
      scheduler.scheduledTaskMessages.size shouldBe 1
      scheduler.scheduledTaskMessages.get(taskID).get shouldBe scheduledTaskMessageMock2
      scheduledTaskMessageMock.isCancelled shouldBe true
      scheduledTaskMessageMock2.isCancelled shouldBe false
    }

    "save task" in {
      val taskID = "taskID"
      val task = new _Task(taskID, "param1", "cron1", true, "actor1")
      val scheduler = TestActorRef(new SchedulerActor(null)).underlyingActor
      scheduler.innerTasks.size shouldBe 0
      scheduler.updateOrSaveTask(task)
      scheduler.innerTasks.size shouldBe 1
      scheduler.innerTasks.get(taskID).get shouldBe task
    }

    "save task update" in {
      val taskID = "taskID"
      val task = new _Task(taskID, "param1", "cron1", true, "actor1")
      val task2 = new _Task(taskID, "param2", "cron2", true, "actor1")
      val scheduler = TestActorRef(new SchedulerActor(null)).underlyingActor
      scheduler.updateOrSaveTask(task)
      scheduler.updateOrSaveTask(task2)
      scheduler.innerTasks.size shouldBe 1
      scheduler.innerTasks.get(taskID).get shouldBe task2
    }

    "remove task process" in {
      val taskID = "taskID"
      val task = new _Task(taskID, "param1", "cron1", true, "actor1")
      val scheduledTaskMessageMock = new Cancellable {
        var flag: Boolean = false

        override def isCancelled: Boolean = flag

        override def cancel(): Boolean = {
          flag = true
          flag
        }
      }
      val scheduler = TestActorRef(new SchedulerActor(null)).underlyingActor
      scheduler.addScheduledTaskMessage(taskID -> scheduledTaskMessageMock)
      scheduler.updateOrSaveTask(task)
      scheduler.removeTaskProcess(taskID)
      scheduler.innerTasks.size shouldBe 0
      scheduledTaskMessageMock.isCancelled shouldBe true
    }
  }

  val oldTasks = Map(
    "ID1" -> new _Task("ID1", "param1", "cron1", true, "actor1"),
    "ID2" -> new _Task("ID2", "param1", "cron1", true, "actor1"),
    "ID3" -> new _Task("ID3", "param1", "cron1", true, "actor1"),
    "ID4" -> new _Task("ID4", "param1", "cron1", true, "actor1"),
    "ID5" -> new _Task("ID5", "param1", "cron1", true, "actor1"),
    "ID6" -> new _Task("ID6", "param1", "cron1", true, "actor1"),
    "ID7" -> new _Task("ID7", "param1", "cron1", false, "actor1")
  )

  val newTask = Seq(
    new _Task("ID1", "param1", "cron1", true, "actor1"),
    new _Task("ID2", "param_", "cron1", true, "actor1"),
    new _Task("ID3", "param1", "cron_", true, "actor1"),
    new _Task("ID4", "param1", "cron1", false, "actor1"),
    new _Task("ID5", "param1", "cron1", true, "actor_"),
    new _Task("ID8", "param1", "cron1", true, "actor1"),
    new _Task("ID9", "param1", "cron1", true, "actor1")
  )
}