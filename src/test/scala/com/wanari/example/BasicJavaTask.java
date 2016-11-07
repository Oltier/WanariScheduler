package com.wanari.example;

import com.wanari.Task;

public class BasicJavaTask implements Task {
    String id;
    String cron;
    String param;
    String actorName;
    boolean enabled;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getActorName() {
        return actorName;
    }

    @Override
    public String getParam() {
        return param;
    }

    @Override
    public String getCron() {
        return cron;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
