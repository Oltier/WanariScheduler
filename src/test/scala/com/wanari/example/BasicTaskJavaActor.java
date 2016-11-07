package com.wanari.example;

import com.wanari.TaskJavaActor;

/**
 * Created by torcsi on 07/11/2016.
 */
public class BasicTaskJavaActor extends TaskJavaActor {

    @Override
    public void receive(Object param) {
        if(param instanceof String) {
        } else
            unhandled(param);
    }
}
