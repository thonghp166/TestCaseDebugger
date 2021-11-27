package com.dse.thread;

import com.dse.guifx_v3.helps.GenerateTestdataTask;
import javafx.concurrent.Task;

/**
 * Represent a thread in aka
 */
public class AkaThread extends Thread {
    private AbstractAkaTask task;
    public AkaThread(AbstractAkaTask task) {
        super(task);
        this.task = task;
    }

    @Override
    public void interrupt() {
        super.interrupt();
        if (task != null)
            task.cancel();
    }

    public AbstractAkaTask getTask() {
        return task;
    }

    public void setTask(AbstractAkaTask task) {
        this.task = task;
    }
}
