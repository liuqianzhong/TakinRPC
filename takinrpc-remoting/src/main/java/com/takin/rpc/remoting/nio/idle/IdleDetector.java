package com.takin.rpc.remoting.nio.idle;

import com.takin.emmet.concurrent.ConcurrentHashSet;
import com.takin.emmet.util.SystemClock;
import com.takin.rpc.remoting.netty4.ResponseFuture;
import com.takin.rpc.remoting.nio.channel.NioChannel;
import com.takin.rpc.remoting.nio.channel.NioChannelImpl;
import com.takin.rpc.remoting.nio.config.NioConfig;
import com.takin.rpc.remoting.nio.handler.Futures;
import com.takin.rpc.remoting.nio.handler.IoFutureListener;

/**
 * @author Robert HG (254963746@qq.com) on 1/24/16.
 */
public class IdleDetector {

    private final ConcurrentHashSet<NioChannel> channels = new ConcurrentHashSet<NioChannel>();

    public void addChannel(NioChannel channel) {
        channels.add(channel);

        //        channel.getCloseFuture().addListener(new IoFutureListener() {
        //            @Override
        //            public void operationComplete(ResponseFuture future) throws Exception {
        //                removeChannel(((Futures.CloseFuture) future).channel());
        //            }
        //        });
    }

    public void removeChannel(NioChannel channel) {
        channels.remove(channel);
    }

    private DetectorTask task = new DetectorTask();

    public void start() {
        new Thread(task).start();
    }

    public void stop() {
        task.stop();
    }

    private class DetectorTask implements Runnable {

        private volatile boolean stop = false;
        private volatile Thread thread;

        @Override
        public void run() {
            thread = Thread.currentThread();

            while (!stop) {
                long currentTime = SystemClock.now();
                for (NioChannel channel : channels) {
                    if (channel.isConnected()) {
                        idleCheck0((NioChannelImpl) channel, currentTime);
                    }
                }
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException ignored) {
                }
            }
        }

        public void stop() {
            stop = true;
            Thread thread = this.thread;
            if (thread != null) {
                thread.interrupt();
            }
        }

        private void idleCheck0(NioChannelImpl channel, long currentTime) {

            IdleInfo idle = channel.getIdleInfo();

            long lastReadTime = idle.getLastReadTime();
            long lastWriteTime = idle.getLastWriteTime();

            long lastIoTime = Math.max(lastReadTime, lastWriteTime);
            NioConfig config = channel.getConfig();

            if (config.getIdleTimeBoth() > 0) {
                notifyIdle(channel, IdleState.BOTH_IDLE, currentTime, config.getIdleTimeBoth() * 1000, Math.max(lastIoTime, idle.getLastBothIdleTime()));
            }

            if (config.getIdleTimeRead() > 0) {
                notifyIdle(channel, IdleState.READER_IDLE, currentTime, config.getIdleTimeRead() * 1000, Math.max(lastIoTime, idle.getLastReadIdleTime()));
            }

            if (config.getIdleTimeWrite() > 0) {
                notifyIdle(channel, IdleState.WRITER_IDLE, currentTime, config.getIdleTimeWrite() * 1000, Math.max(lastIoTime, idle.getLastWriteIdleTime()));
            }

            notifyWriteTimeout(channel, currentTime);
        }

        private void notifyWriteTimeout(NioChannelImpl channel, long currentTime) {
            // 将正在写的请求置为timeout
            // TODO
        }

        private void notifyIdle(NioChannelImpl channel, IdleState state, long currentTime, long idleTime, long lastIoTime) {
            if ((idleTime > 0) && (lastIoTime != 0) && (currentTime - lastIoTime >= idleTime)) {
                channel.fireChannelIdle(state, currentTime);
            }
        }
    }

}
