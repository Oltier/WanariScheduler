package com.wanari;

import akka.actor.UntypedActor;


abstract public class TaskJavaActor extends UntypedActor {
    @Override
    public final void onReceive(Object message) throws Exception {
        if(message instanceof SchedulerActor.ExecuteTask) {
            SchedulerActor.ExecuteTask x = (SchedulerActor.ExecuteTask) message;
            try {
                receive(x.param());
                sender().tell(new SchedulerActor.TaskFinished(x.taskId()), getSelf());
            } catch(Exception e) {
                sender().tell(new SchedulerActor.TaskFailed(x.taskId(), "", e), getSelf());
            }
        } else {
            receive(message);
        }
    }

    abstract public void receive(Object param);
}
