package com.roposo.core.util;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.util.concurrent.CountDownLatch;

/**
 * Created by hchhabra on 31/05/16.
 */
public class DispatchQueue extends Thread {

    private volatile Handler handler = null;
    private CountDownLatch syncLatch = new CountDownLatch(1);

    public DispatchQueue(final String threadName) {
        super(threadName);
        start();
    }

    private void sendMessage(Message msg, int delay) {
        try {
            syncLatch.await();
            if (delay <= 0) {
                handler.sendMessage(msg);
            } else {
                handler.sendMessageDelayed(msg, delay);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public void cancelRunnable(Runnable runnable) {
        try {
            syncLatch.await();
            handler.removeCallbacks(runnable);
        } catch (InterruptedException e) {
        }
    }

    public void postRunnable(Runnable runnable) {
        postRunnable(runnable, 0);
    }

    public void postRunnable(Runnable runnable, long delay) {
        try {
            syncLatch.await();
            if (delay <= 0) {
                handler.post(runnable);
            } else {
                handler.postDelayed(runnable, delay);
            }
        } catch (InterruptedException e) {
        }
    }

    public void cleanupQueue() {
        try {
            syncLatch.await();
            if (handler != null) {
                handler.removeCallbacksAndMessages(null);
            }
        } catch (InterruptedException e) {
        }
    }

    @Override
    public void run() {
        Looper.prepare();
        handler = new Handler();
        syncLatch.countDown();
        Looper.loop();
    }

    public void finishAndShutdown() {
        postRunnable(new Runnable() {
            @Override
            public void run() {
                Looper looper = Looper.myLooper();
                if (looper != null) {
                    looper.quit();
                    handler = null;
                }
            }
        });
    }

    // TODO : forced kill method
}
