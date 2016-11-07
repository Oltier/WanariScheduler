package com.wanari.example

import com.wanari.Task

case class BasicTask(
											id: String,
											param: String,
											cron: String,
											enabled: Boolean,
											actorName: String
											) extends Task {
	override def getId(): String = id

	override def getParam(): String = param

	override def getCron(): String = cron

	override def isEnabled(): Boolean = enabled

	override def getActorName(): String = actorName
}
