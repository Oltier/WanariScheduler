package com.wanari.scheduler

trait Task {
  def getId(): String

  def getActorName(): String

  def getParam(): AnyRef

  def getCron(): String

  def isEnabled(): Boolean
}

object Task {
  def same(a: Task, b: Task) = {
    a.getId() == b.getId() &&
      a.getActorName() == b.getActorName() &&
      a.getParam() == b.getParam() &&
      a.getCron() == b.getCron() &&
      a.isEnabled() == b.isEnabled()
  }
}
