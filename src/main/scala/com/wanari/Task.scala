package com.wanari


trait Task {
	def getId() : String
	def getActorName() : String
	def getParam(): String
	def getCron(): String
	def isEnabled(): Boolean
}
