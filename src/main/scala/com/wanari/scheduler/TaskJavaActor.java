package com.wanari.scheduler;

import akka.actor.UntypedActor;


abstract public class TaskJavaActor extends UntypedActor {
    @Override
    public final void onReceive(Object message) throws Exception {
        if (message instanceof SchedulerMessages.ExecuteTask) {
            SchedulerMessages.ExecuteTask x = (SchedulerMessages.ExecuteTask) message;
            try {
                receive(x.param());
                sender().tell(new SchedulerMessages.TaskFinished(x.taskId()), getSelf());
            } catch (Exception e) {
                sender().tell(new SchedulerMessages.TaskFailed(x.taskId(), "", e), getSelf());
            }
        } else {
            receive(message);
        }
    }

    abstract public void receive(Object param);
}
