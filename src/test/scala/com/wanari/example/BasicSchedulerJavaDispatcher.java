package com.wanari.example;

import com.wanari.SchedulerDispatcher;
import com.wanari.SchedulerJavaDispatcher;
import com.wanari.Task;
import scala.collection.immutable.List;

import java.util.ArrayList;

/**
 * Created by torcsi on 07/11/2016.
 */
public class BasicSchedulerJavaDispatcher extends SchedulerJavaDispatcher {

    @Override
    public java.util.List<Task> getAllTaskAsJava() {
        return new ArrayList<Task>();
    }


}
