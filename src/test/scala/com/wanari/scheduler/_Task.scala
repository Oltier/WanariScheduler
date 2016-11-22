package com.wanari.scheduler

case class _Task(
                      id: String,
                      param: AnyRef,
                      cron: String,
                      enabled: Boolean,
                      actorName: String
                    ) extends Task {
  override def getId(): String = id

  override def getParam(): AnyRef = param

  override def getCron(): String = cron

  override def isEnabled(): Boolean = enabled

  override def getActorName(): String = actorName
}
