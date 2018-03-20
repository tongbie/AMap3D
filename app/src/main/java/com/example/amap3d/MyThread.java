package com.example.amap3d;

/**
 * Created by BieTong on 2018/3/20.
 */

public abstract class MyThread extends Thread {

    private boolean suspend = false;

    private String control = ""; // 只是需要一个对象而已，这个对象没有实际意义

    public void setSuspend(boolean suspend) {
        if (!suspend) {
            synchronized (control) {
                control.notifyAll();
            }
        }
        this.suspend = suspend;
    }

    public boolean isSuspend() {
        return this.suspend;
    }

    public void run() {
        while (true) {
            synchronized (control) {
                if (suspend) {
                    try {
                        control.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            this.runPersonelLogic();
        }
    }

    protected abstract void runPersonelLogic();

    public static void main(String[] args) throws Exception {
        MyThread myThread = new MyThread() {
            protected void runPersonelLogic() {
                System.out.println("myThead is running");
            }
        };
        myThread.start();
        Thread.sleep(3000);
        myThread.setSuspend(true);
        System.out.println("myThread has stopped");
        Thread.sleep(3000);
        myThread.setSuspend(false);
    }
}
