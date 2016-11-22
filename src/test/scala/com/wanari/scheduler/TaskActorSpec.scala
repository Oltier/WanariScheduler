package com.wanari.scheduler

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import com.wanari.scheduler.SchedulerMessages._

class TaskActorSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("MySpec"))

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "TaskActor" must {
    "send back finished on execute" in {
      val taskId = "TestTaskID"
      val param = "param"
      val taskRef = TestActorRef(new _TaskActor)

      taskRef ! ExecuteTask(taskId, param)
      expectMsg(TaskFinished(taskId))
    }

    "send back failed on execute when exception" in {
      val exception = new Exception("excpetion msg")
      val taskId = "TestTaskID"
      val param = "param"
      val taskRef = TestActorRef(new _FailTaskActor(exception))

      taskRef ! ExecuteTask(taskId, param)
      expectMsg(TaskFailed(taskId, "", exception))
    }

    "receive message without execute message" in {
      val taskId = "TestTaskID"
      val param1 = "param msg 1"
      val param2 = "param msg 2"
      val taskRef = TestActorRef(new _TaskActor)
      val task = taskRef.underlyingActor

      taskRef ! param1
      task.receivedParam shouldBe param1

      taskRef ! ExecuteTask(taskId, param2)
      task.receivedParam shouldBe param2
      expectMsg(TaskFinished(taskId))
    }
  }

  "TaskJavaActor" must {
    "send back finished on execute" in {
      val taskId = "TestTaskID"
      val param = "param"
      val taskRef = TestActorRef(new _JavaTaskActor)

      taskRef ! ExecuteTask(taskId, param)
      expectMsg(TaskFinished(taskId))
    }

    "send back failed on execute when exception" in {
      val exception = new Exception("excpetion msg")
      val taskId = "TestTaskID"
      val param = "param"
      val taskRef = TestActorRef(new _FailJavaTaskActor(exception))

      taskRef ! ExecuteTask(taskId, param)
      expectMsg(TaskFailed(taskId, "", exception))
    }

    "receive message without execute message" in {
      val taskId = "TestTaskID"
      val param1 = "param msg 1"
      val param2 = "param msg 2"
      val taskRef = TestActorRef(new _JavaTaskActor)
      val task = taskRef.underlyingActor

      taskRef ! param1
      task.receivedParam shouldBe param1

      taskRef ! ExecuteTask(taskId, param2)
      task.receivedParam shouldBe param2
      expectMsg(TaskFinished(taskId))
    }

  }
}

class _TaskActor extends TaskActor {
  var receivedParam : String = null
  override def onReceive: Receive = {
    case param: String => receivedParam = param
  }
}

class _FailTaskActor(exception: Throwable) extends TaskActor {
  var receivedParam : String = null
  override def onReceive: Receive = {
    throw exception
  }
}

class _JavaTaskActor extends TaskJavaActor {
  var receivedParam : String = null

  override def receive(param: scala.Any): Unit = {
    receivedParam = param.toString
  }
}

class _FailJavaTaskActor(exception: Throwable) extends TaskJavaActor {
  var receivedParam : String = null
  override def receive(param: scala.Any): Unit = {
    throw exception
  }
}
