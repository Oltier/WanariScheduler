package com.wanari.scheduler

import java.util

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import com.wanari.scheduler.SchedulerMessages.{SysError, TaskFailed, TaskFinished, TaskStarted, _}

class SchedulerDispatcherSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("MySpec"))

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "SchedulerDispatcher" must {

    "default handlers" in {
      val dispatcher = TestActorRef(new SchedulerDispatcher {
        override def getAllTask(): Seq[Task] = Seq.empty[Task]
      })
      dispatcher ! TaskListRequest
      expectMsg(TaskList(List()))
      dispatcher ! TaskStarted("taskid")
      dispatcher ! TaskFinished("taskid")
      dispatcher ! TaskFailed("taskid")
      dispatcher ! SysError("error")
    }

    "send back list of task on request" in {
      val dispatcher = TestActorRef(new _SchedulerDispatcher)
      dispatcher ! TaskListRequest
      expectMsg(TaskList(List()))
    }

    "send back list of task on request with task" in {
      val task = new _Task("", "", "", true, "")
      val dispatcherRef = TestActorRef(new _SchedulerDispatcher)
      dispatcherRef.underlyingActor.tasks = Seq(task)
      dispatcherRef ! TaskListRequest
      expectMsg(TaskList(List(task)))
    }

    "call handler start" in {
      val taskId = ""
      val dispatcherRef = TestActorRef(new _SchedulerDispatcher)
      val dispatcher = dispatcherRef.underlyingActor

      dispatcherRef ! TaskStarted(taskId)
      dispatcher.taskStartedFlag shouldBe true
      dispatcher.taskFinishedFlag shouldBe false
      dispatcher.taskFailedFlag shouldBe false
      dispatcher.sysErrorFlag shouldBe false
    }

    "call handler finshed" in {
      val taskId = ""
      val dispatcherRef = TestActorRef(new _SchedulerDispatcher)
      val dispatcher = dispatcherRef.underlyingActor

      dispatcherRef ! TaskFinished(taskId)
      dispatcher.taskStartedFlag shouldBe false
      dispatcher.taskFinishedFlag shouldBe true
      dispatcher.taskFailedFlag shouldBe false
      dispatcher.sysErrorFlag shouldBe false
    }

    "call handler failed" in {
      val taskId = ""
      val dispatcherRef = TestActorRef(new _SchedulerDispatcher)
      val dispatcher = dispatcherRef.underlyingActor

      dispatcherRef ! TaskFailed(taskId)
      dispatcher.taskStartedFlag shouldBe false
      dispatcher.taskFinishedFlag shouldBe false
      dispatcher.taskFailedFlag shouldBe true
      dispatcher.sysErrorFlag shouldBe false
    }

    "call handler syserror" in {
      val sysErrorMsg = ""
      val dispatcherRef = TestActorRef(new _SchedulerDispatcher)
      val dispatcher = dispatcherRef.underlyingActor

      dispatcherRef ! SysError(sysErrorMsg)
      dispatcher.taskStartedFlag shouldBe false
      dispatcher.taskFinishedFlag shouldBe false
      dispatcher.taskFailedFlag shouldBe false
      dispatcher.sysErrorFlag shouldBe true
    }

  }

  "SchedulerJavaDispatcher" must {

    "send back list of task on request" in {
      val dispatcher = TestActorRef(new _SchedulerJavaDispatcher)
      dispatcher ! TaskListRequest
      expectMsg(TaskList(List()))
    }

    "send back list of task on request with task" in {
      val task = new _Task("", "", "", true, "")
      val dispatcherRef = TestActorRef(new _SchedulerJavaDispatcher)
      dispatcherRef.underlyingActor.tasks.add(task)
      dispatcherRef ! TaskListRequest
      expectMsg(TaskList(List(task)))
    }

    "call handler start" in {
      val taskId = ""
      val dispatcherRef = TestActorRef(new _SchedulerJavaDispatcher)
      val dispatcher = dispatcherRef.underlyingActor

      dispatcherRef ! TaskStarted(taskId)
      dispatcher.taskStartedFlag shouldBe true
      dispatcher.taskFinishedFlag shouldBe false
      dispatcher.taskFailedFlag shouldBe false
      dispatcher.sysErrorFlag shouldBe false
    }

    "call handler finshed" in {
      val taskId = ""
      val dispatcherRef = TestActorRef(new _SchedulerJavaDispatcher)
      val dispatcher = dispatcherRef.underlyingActor

      dispatcherRef ! TaskFinished(taskId)
      dispatcher.taskStartedFlag shouldBe false
      dispatcher.taskFinishedFlag shouldBe true
      dispatcher.taskFailedFlag shouldBe false
      dispatcher.sysErrorFlag shouldBe false
    }

    "call handler failed" in {
      val taskId = ""
      val dispatcherRef = TestActorRef(new _SchedulerJavaDispatcher)
      val dispatcher = dispatcherRef.underlyingActor

      dispatcherRef ! TaskFailed(taskId)
      dispatcher.taskStartedFlag shouldBe false
      dispatcher.taskFinishedFlag shouldBe false
      dispatcher.taskFailedFlag shouldBe true
      dispatcher.sysErrorFlag shouldBe false
    }

    "call handler syserror" in {
      val sysErrorMsg = ""
      val dispatcherRef = TestActorRef(new _SchedulerJavaDispatcher)
      val dispatcher = dispatcherRef.underlyingActor

      dispatcherRef ! SysError(sysErrorMsg)
      dispatcher.taskStartedFlag shouldBe false
      dispatcher.taskFinishedFlag shouldBe false
      dispatcher.taskFailedFlag shouldBe false
      dispatcher.sysErrorFlag shouldBe true
    }
  }

}

class _SchedulerDispatcher extends SchedulerDispatcher {
  var getAllTaskFlag: Boolean = false
  var taskStartedFlag: Boolean = false
  var taskFinishedFlag: Boolean = false
  var taskFailedFlag: Boolean = false
  var sysErrorFlag: Boolean = false
  var tasks: Seq[Task] = Seq.empty[Task]

  override def getAllTask(): Seq[Task] = tasks

  override def taskStarted(taskId: String): Unit = {
    taskStartedFlag = true
  }

  override def taskFinished(taskId: String): Unit = {
    taskFinishedFlag = true
  }

  override def taskFailed(taskId: String, msg: String, ex: Exception): Unit = {
    taskFailedFlag = true
  }

  override def sysError(msg: String, ex: Exception): Unit = {
    sysErrorFlag = true
  }

  def checkAllTaskFlag(): Boolean = {
    val ret = getAllTaskFlag
    getAllTaskFlag = false
    ret
  }
}

class _SchedulerJavaDispatcher extends SchedulerJavaDispatcher {
  var taskStartedFlag: Boolean = false
  var taskFinishedFlag: Boolean = false
  var taskFailedFlag: Boolean = false
  var sysErrorFlag: Boolean = false
  var tasks: util.List[Task] = new util.ArrayList[Task]()

  override def getAllTaskAsJava(): util.List[Task] = tasks

  override def taskStarted(taskId: String): Unit = {
    taskStartedFlag = true
  }

  override def taskFinished(taskId: String): Unit = {
    taskFinishedFlag = true
  }

  override def taskFailed(taskId: String, msg: String, ex: Exception): Unit = {
    taskFailedFlag = true
  }

  override def sysError(msg: String, ex: Exception): Unit = {
    sysErrorFlag = true
  }

}

