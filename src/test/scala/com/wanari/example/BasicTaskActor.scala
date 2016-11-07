package com.wanari.example

import com.wanari.TaskActor

/**
 * Created by torcsi on 07/11/2016.
 */
class BasicTaskActor extends TaskActor {

	override def onReceive: Receive = {
		case param: String => {

		}
		case _ => {}
	}
	
}
