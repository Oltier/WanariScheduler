package com.wanari.scheduler


import java.util.concurrent.TimeUnit

import akka.actor.{ActorSystem, Kill, PoisonPill}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import com.wanari.scheduler.SchedulerMessages._
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, WordSpecLike}

import scala.concurrent.duration.{FiniteDuration, _}


class SchedulerActorBoxSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

  def this() = this(ActorSystem("MySpec"))

  var schedulerRef: TestActorRef[SchedulerActor] = null

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  override def afterEach = {
    if (schedulerRef != null)
      schedulerRef ! PoisonPill
  }

  "SchedulerActorBox" must {
    "init" in {
      schedulerRef = TestActorRef(new SchedulerActor(testActor))
      within(1 seconds) {
        expectNoMsg
      }
    }

    "start" in {
      schedulerRef = TestActorRef(new SchedulerActor(testActor, FiniteDuration(450, TimeUnit.MILLISECONDS)))
      within(400 milliseconds) {
        schedulerRef ! ScheduledRunStart
        expectMsg(TaskListRequest)
      }
    }

    "repeating request when start" in {
      schedulerRef = TestActorRef(new SchedulerActor(testActor, FiniteDuration(450, TimeUnit.MILLISECONDS)))
      within(1 seconds) {
        schedulerRef ! ScheduledRunStart
        expectMsg(TaskListRequest)
        expectMsg(TaskListRequest)
      }
    }

    "stop repeating request when stopping" in {
      schedulerRef = TestActorRef(new SchedulerActor(testActor, FiniteDuration(450, TimeUnit.MILLISECONDS)))
      within(1 seconds) {
        schedulerRef ! ScheduledRunStart
        expectMsg(TaskListRequest)
        schedulerRef ! ScheduledRunStop
        expectNoMsg
      }
    }

    "start error when already started" in {
      schedulerRef = TestActorRef(new SchedulerActor(testActor))
      schedulerRef ! ScheduledRunStart
      expectMsg(TaskListRequest)
      schedulerRef ! ScheduledRunStart
      expectMsgClass(classOf[SysError])
    }

    "stop error when not started" in {
      schedulerRef = TestActorRef(new SchedulerActor(testActor))
      schedulerRef ! ScheduledRunStop
      expectMsgClass(classOf[SysError])
    }

    "stop error when already stopped" in {
      schedulerRef = TestActorRef(new SchedulerActor(testActor))
      schedulerRef ! ScheduledRunStart
      expectMsg(TaskListRequest)
      schedulerRef ! ScheduledRunStop
      schedulerRef ! ScheduledRunStop
      expectMsgClass(classOf[SysError])
    }

    "task finished forward to dispatcher" in {
      val taskID = "TaskID1"
      schedulerRef = TestActorRef(new SchedulerActor(testActor))
      schedulerRef ! TaskFinished(taskID) // TaskActor -> Scheduler
      expectMsg(TaskFinished(taskID)) // Scheduler -> Dispatcher
    }

    "task failed forward to dispatcher" in {
      val taskID = "TaskID1"
      schedulerRef = TestActorRef(new SchedulerActor(testActor))
      schedulerRef ! TaskFailed(taskID) // TaskActor -> Scheduler
      expectMsg(TaskFailed(taskID)) // Scheduler -> Dispatcher
    }

    "manual run when not started" in {
      val param = "test param"
      val taskID = "TaskID1"
      val task = _Task(taskID, param, "0 0/1 * 1/1 * ? *", true, testActor.path.toString)

      schedulerRef = TestActorRef(new SchedulerActor(testActor))
      schedulerRef ! TaskList(Seq(task)) // Dispatcher -> Scheduler
      schedulerRef ! ManualRun(taskID) // Dispatcher -> Scheduler
      expectMsg(ExecuteTask(taskID, param)) // Scheduler -> TaskActor
      expectMsg(TaskStarted(taskID)) // Scheduler -> Dispatcher
    }

    "manual run when not started and disabled task" in {
      val param = "test param"
      val taskID = "TaskID1"
      val task = _Task(taskID, param, "0 0/1 * 1/1 * ? *", false, testActor.path.toString)

      schedulerRef = TestActorRef(new SchedulerActor(testActor))
      schedulerRef ! TaskList(Seq(task)) // Dispatcher -> Scheduler
      schedulerRef ! ManualRun(taskID) // Dispatcher -> Scheduler
      expectMsg(ExecuteTask(taskID, param)) // Scheduler -> TaskActor
      expectMsg(TaskStarted(taskID)) // Scheduler -> Dispatcher
    }

    "manual run when started but disabled task" in {
      val param = "test param"
      val taskID = "TaskID1"
      val task = _Task(taskID, param, "0 0/1 * 1/1 * ? *", false, testActor.path.toString)

      schedulerRef = TestActorRef(new SchedulerActor(testActor))
      schedulerRef ! ScheduledRunStart // Dispatcher -> Scheduler
      expectMsg(TaskListRequest) // Scheduler -> Dispatcher
      schedulerRef ! TaskList(Seq(task)) // Dispatcher -> Scheduler
      schedulerRef ! ManualRun(taskID) // Dispatcher -> Scheduler
      expectMsg(ExecuteTask(taskID, param)) // Scheduler -> TaskActor
      expectMsg(TaskStarted(taskID)) // Scheduler -> Dispatcher

    }

    "error manual run when wrong taskid" in {
      val taskID = "TaskID1"
      schedulerRef = TestActorRef(new SchedulerActor(testActor))
      schedulerRef ! ManualRun(taskID)
      expectMsgClass(classOf[SysError])
    }

    "error manual run when task running" in {
      val param = "test param"
      val taskID = "TaskID1"
      val task = _Task(taskID, param, "0 0/1 * 1/1 * ? *", false, testActor.path.toString)

      schedulerRef = TestActorRef(new SchedulerActor(testActor))
      schedulerRef ! ScheduledRunStart // Dispatcher -> Scheduler
      expectMsg(TaskListRequest) // Scheduler -> Dispatcher

      schedulerRef ! TaskList(Seq(task)) // Dispatcher -> Scheduler
      schedulerRef ! ManualRun(taskID) // Dispatcher -> Scheduler
      expectMsg(ExecuteTask(taskID, param)) // Scheduler -> TaskActor
      expectMsg(TaskStarted(taskID)) // Scheduler -> Dispatcher

      schedulerRef ! ManualRun(taskID) // Dispatcher -> Scheduler
      expectMsgClass(classOf[SysError]) // Scheduler -> Dispatcher
    }

    "manual rerun after finished" in {
      val param = "test param"
      val taskID = "TaskID1"
      val task = _Task(taskID, param, "0 0/1 * 1/1 * ? *", false, testActor.path.toString)

      schedulerRef = TestActorRef(new SchedulerActor(testActor))
      schedulerRef ! ScheduledRunStart // Dispatcher -> Scheduler
      expectMsg(TaskListRequest) // Scheduler -> Dispatcher

      schedulerRef ! TaskList(Seq(task)) // Dispatcher -> Scheduler
      schedulerRef ! ManualRun(taskID) // Dispatcher -> Scheduler
      expectMsg(ExecuteTask(taskID, param)) // Scheduler -> TaskActor
      expectMsg(TaskStarted(taskID)) // Scheduler -> Dispatcher
      schedulerRef ! TaskFinished(taskID) // TaskActor -> Scheduler
      expectMsg(TaskFinished(taskID)) // Scheduler -> Dispatcher

      schedulerRef ! ManualRun(taskID) // Dispatcher -> Scheduler
      expectMsg(ExecuteTask(taskID, param)) // Scheduler -> TaskActor
      expectMsg(TaskStarted(taskID)) // Scheduler -> Dispatcher
    }

    "manual rerun after failed" in {
      val param = "test param"
      val taskID = "TaskID1"
      val task = _Task(taskID, param, "0 0/1 * 1/1 * ? *", false, testActor.path.toString)

      schedulerRef = TestActorRef(new SchedulerActor(testActor))
      schedulerRef ! ScheduledRunStart // Dispatcher -> Scheduler
      expectMsg(TaskListRequest) // Scheduler -> Dispatcher

      schedulerRef ! TaskList(Seq(task)) // Dispatcher -> Scheduler
      schedulerRef ! ManualRun(taskID) // Dispatcher -> Scheduler
      expectMsg(ExecuteTask(taskID, param)) // Scheduler -> TaskActor
      expectMsg(TaskStarted(taskID)) // Scheduler -> Dispatcher
      schedulerRef ! TaskFailed(taskID) // TaskActor -> Scheduler
      expectMsg(TaskFailed(taskID)) // Scheduler -> Dispatcher

      schedulerRef ! ManualRun(taskID) // Dispatcher -> Scheduler
      expectMsg(ExecuteTask(taskID, param)) // Scheduler -> TaskActor
      expectMsg(TaskStarted(taskID)) // Scheduler -> Dispatcher
    }

    "scheduled run" in {
      val param = "test param"
      val taskID = "TaskID1"
      val every2SecondsCron = "*/2 * * * * ?"
      val task = _Task(taskID, param, every2SecondsCron, true, testActor.path.toString)

      within(5 seconds) {
        schedulerRef = TestActorRef(new SchedulerActor(testActor))
        schedulerRef ! ScheduledRunStart // Dispatcher -> Scheduler

        expectMsg(TaskListRequest) // Scheduler -> Dispatcher
        schedulerRef ! TaskList(Seq(task)) // Dispatcher -> Scheduler

        expectMsg(ExecuteTask(taskID, param)) // Scheduler -> TaskActor
        expectMsg(TaskStarted(taskID)) // Scheduler -> Dispatcher
        schedulerRef ! TaskFinished(taskID) // TaskActor -> Scheduler
        expectMsg(TaskFinished(taskID)) // Scheduler -> Dispatcher

        expectMsg(ExecuteTask(taskID, param)) // Scheduler -> TaskActor
        expectMsg(TaskStarted(taskID)) // Scheduler -> Dispatcher
        schedulerRef ! TaskFinished(taskID) // TaskActor -> Scheduler
        expectMsg(TaskFinished(taskID)) // Scheduler -> Dispatcher
      }
    }

    "scheduled run skip when previous running not stopped yet" in {
      val param = "test param"
      val taskID = "TaskID1"
      val every2SecondsCron = "*/2 * * * * ?"
      val task = _Task(taskID, param, every2SecondsCron, true, testActor.path.toString)

      within(5 seconds) {
        schedulerRef = TestActorRef(new SchedulerActor(testActor))
        schedulerRef ! ScheduledRunStart // Dispatcher -> Scheduler

        expectMsg(TaskListRequest) // Scheduler -> Dispatcher
        schedulerRef ! TaskList(Seq(task)) // Dispatcher -> Scheduler

        expectMsg(ExecuteTask(taskID, param)) // Scheduler -> TaskActor
        expectMsg(TaskStarted(taskID)) // Scheduler -> Dispatcher
      }

      within(3 seconds) {
        schedulerRef ! TaskFinished(taskID) // TaskActor -> Scheduler
        expectMsg(TaskFinished(taskID)) // Scheduler -> Dispatcher

        expectMsg(ExecuteTask(taskID, param)) // Scheduler -> TaskActor
        expectMsg(TaskStarted(taskID)) // Scheduler -> Dispatcher
        schedulerRef ! TaskFinished(taskID) // TaskActor -> Scheduler
        expectMsg(TaskFinished(taskID)) // Scheduler -> Dispatcher
      }
    }

    "scheduled run skip when manual running" in {
      val param = "test param"
      val taskID = "TaskID1"
      val everySecondsCron = "*/1 * * * * ?"
      val task = _Task(taskID, param, everySecondsCron, true, testActor.path.toString)

      within(3 seconds) {
        schedulerRef = TestActorRef(new SchedulerActor(testActor))
        schedulerRef ! TaskList(Seq(task)) // Dispatcher -> Scheduler
        schedulerRef ! ManualRun(taskID)
        expectMsg(ExecuteTask(taskID, param)) // Scheduler -> TaskActor
        expectMsg(TaskStarted(taskID)) // Scheduler -> Dispatcher

        schedulerRef ! ScheduledRunStart // Dispatcher -> Scheduler

        expectMsg(TaskListRequest) // Scheduler -> Dispatcher
        schedulerRef ! TaskList(Seq(task)) // Dispatcher -> Scheduler

        expectNoMsg
      }
      within(2 seconds) {
        schedulerRef ! TaskFinished(taskID) // TaskActor -> Scheduler
        expectMsg(TaskFinished(taskID)) // Scheduler -> Dispatcher

        expectMsg(ExecuteTask(taskID, param)) // Scheduler -> TaskActor
        expectMsg(TaskStarted(taskID)) // Scheduler -> Dispatcher
        schedulerRef ! TaskFinished(taskID) // TaskActor -> Scheduler
        expectMsg(TaskFinished(taskID)) // Scheduler -> Dispatcher
      }
    }

    "scheduling refresh when task list refresh" in {
      val param = "test param"
      val param2 = "test param2"
      val taskID = "TaskID1"
      val every2SecondsCron = "*/2 * * * * ?"
      val everySecondsCron = "*/1 * * * * ?"
      val task = _Task(taskID, param, every2SecondsCron, true, testActor.path.toString)
      val task2 = _Task(taskID, param2, everySecondsCron, true, testActor.path.toString)

      within(4500 milliseconds) {
        schedulerRef = TestActorRef(new SchedulerActor(testActor))
        schedulerRef ! ScheduledRunStart // Dispatcher -> Scheduler

        expectMsg(TaskListRequest) // Scheduler -> Dispatcher
        schedulerRef ! TaskList(Seq(task)) // Dispatcher -> Scheduler

        expectMsg(ExecuteTask(taskID, param)) // Scheduler -> TaskActor
        expectMsg(TaskStarted(taskID)) // Scheduler -> Dispatcher
        schedulerRef ! TaskFinished(taskID) // TaskActor -> Scheduler
        expectMsg(TaskFinished(taskID)) // Scheduler -> Dispatcher

        expectMsg(ExecuteTask(taskID, param)) // Scheduler -> TaskActor
        expectMsg(TaskStarted(taskID)) // Scheduler -> Dispatcher
        schedulerRef ! TaskFinished(taskID) // TaskActor -> Scheduler
        expectMsg(TaskFinished(taskID)) // Scheduler -> Dispatcher

        expectNoMsg
      }

      within(3 seconds) {
        schedulerRef ! TaskList(Seq(task2)) // Dispatcher -> Scheduler

        expectMsg(ExecuteTask(taskID, param2)) // Scheduler -> TaskActor
        expectMsg(TaskStarted(taskID)) // Scheduler -> Dispatcher
        schedulerRef ! TaskFinished(taskID) // TaskActor -> Scheduler
        expectMsg(TaskFinished(taskID)) // Scheduler -> Dispatcher

        expectMsg(ExecuteTask(taskID, param2)) // Scheduler -> TaskActor
        expectMsg(TaskStarted(taskID)) // Scheduler -> Dispatcher
        schedulerRef ! TaskFinished(taskID) // TaskActor -> Scheduler
        expectMsg(TaskFinished(taskID)) // Scheduler -> Dispatcher
      }
    }

    "scheduling remove when task removed" in {
      val param = "test param"
      val taskID = "TaskID1"
      val everySecondsCron = "*/1 * * * * ?"
      val task = _Task(taskID, param, everySecondsCron, true, testActor.path.toString)

      within(4 seconds) {
        schedulerRef = TestActorRef(new SchedulerActor(testActor))
        schedulerRef ! ScheduledRunStart // Dispatcher -> Scheduler

        expectMsg(TaskListRequest) // Scheduler -> Dispatcher
        schedulerRef ! TaskList(Seq(task)) // Dispatcher -> Scheduler

        expectMsg(ExecuteTask(taskID, param)) // Scheduler -> TaskActor
        expectMsg(TaskStarted(taskID)) // Scheduler -> Dispatcher
        schedulerRef ! TaskFinished(taskID) // TaskActor -> Scheduler
        expectMsg(TaskFinished(taskID)) // Scheduler -> Dispatcher

        expectMsg(ExecuteTask(taskID, param)) // Scheduler -> TaskActor
        expectMsg(TaskStarted(taskID)) // Scheduler -> Dispatcher
        schedulerRef ! TaskFinished(taskID) // TaskActor -> Scheduler
        expectMsg(TaskFinished(taskID)) // Scheduler -> Dispatcher

        schedulerRef ! TaskList(Seq.empty[Task]) // Dispatcher -> Scheduler

        expectNoMsg
      }

    }


    "disabled task not scheduled running" in {
      val param = "test param"
      val taskID = "TaskID1"
      val everySecondsCron = "*/1 * * * * ?"
      val task = _Task(taskID, param, everySecondsCron, false, testActor.path.toString)

      within(2 seconds) {
        schedulerRef = TestActorRef(new SchedulerActor(testActor))
        schedulerRef ! ScheduledRunStart // Dispatcher -> Scheduler

        expectMsg(TaskListRequest) // Scheduler -> Dispatcher
        schedulerRef ! TaskList(Seq(task)) // Dispatcher -> Scheduler

        expectNoMsg
      }

    }

    "error when ExecuteTask" in {
      schedulerRef = TestActorRef(new SchedulerActor(testActor))
      schedulerRef ! ExecuteTask("TaskID", "param")
      expectMsgClass(classOf[SysError]) // Scheduler -> Dispatcher
    }
  }
}