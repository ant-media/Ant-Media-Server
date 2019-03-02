package io.antmedia.logger;

import java.util.concurrent.atomic.AtomicInteger;

public final class LoggerEnvironment {

    protected static final ThreadLocal<AtomicInteger> LOGGER_THREAD = new ThreadLocal<AtomicInteger>() {
        @Override
        protected AtomicInteger initialValue() {
            return new AtomicInteger();
        }
    };

    private LoggerEnvironment() {
    }

    public static void startManagingThread() {
        try {
            if (isManagingThread()) {
                //do nothing
            }
        } finally {
            LOGGER_THREAD.get().incrementAndGet();
        }
    }

    public static void stopManagingThread() {
        try {
            if (!isManagingThread()) {
                startManagingThread();
            }
        } finally {
            if (LOGGER_THREAD.get().decrementAndGet() == 0) {
                LOGGER_THREAD.remove();
            }
        }
    }

    public static boolean isManagingThread() {
        return LOGGER_THREAD.get().get() > 0;
    }

}
