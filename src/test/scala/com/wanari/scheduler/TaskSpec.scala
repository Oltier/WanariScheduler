package com.wanari.scheduler

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class TaskSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("MySpec"))

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "Task" must {
    "same task self" in {
      val task1 = new _Task("ID1", "Param1", "Cron1", true, "Actor1")
      Task.same(task1, task1) shouldBe true
    }

    "same task true" in {
      val task1 = new _Task("ID1", "Param1", "Cron1", true, "Actor1")
      val task2 = new _Task("ID1", "Param1", "Cron1", true, "Actor1")
      Task.same(task1, task2) shouldBe true
    }

    "same task different param" in {
      val task1 = new _Task("ID1", "Param1", "Cron1", true, "Actor1")
      val task2 = new _Task("ID1", "Param2", "Cron1", true, "Actor1")
      Task.same(task1, task2) shouldBe false
    }

    "same task different cron" in {
      val task1 = new _Task("ID1", "Param1", "Cron1", true, "Actor1")
      val task2 = new _Task("ID1", "Param1", "Cron2", true, "Actor1")
      Task.same(task1, task2) shouldBe false
    }

    "same task different enabled" in {
      val task1 = new _Task("ID1", "Param1", "Cron1", true, "Actor1")
      val task2 = new _Task("ID1", "Param1", "Cron1", false, "Actor1")
      Task.same(task1, task2) shouldBe false
    }

    "same task different id" in {
      val task1 = new _Task("ID1", "Param1", "Cron1", true, "Actor1")
      val task2 = new _Task("ID2", "Param1", "Cron1", true, "Actor1")
      Task.same(task1, task2) shouldBe false
    }

    "same task different actorname" in {
      val task1 = new _Task("ID1", "Param1", "Cron1", true, "Actor1")
      val task2 = new _Task("ID1", "Param1", "Cron1", true, "Actor2")
      Task.same(task1, task2) shouldBe false
    }
  }
}

