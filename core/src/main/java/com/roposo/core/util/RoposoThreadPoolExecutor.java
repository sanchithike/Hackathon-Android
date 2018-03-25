package com.roposo.core.util;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author muddassir on 3/13/18.
 */

public class RoposoThreadPoolExecutor {
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = Math.max(2, Math.min(CPU_COUNT - 1, 4));
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
    private static final int KEEP_ALIVE_SECONDS = 5;
    private static ThreadPoolExecutor poolExecutor;

    public static ThreadPoolExecutor getExecutor() {
        if (poolExecutor == null) {
            poolExecutor = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE_SECONDS, TimeUnit.SECONDS
                    , new LinkedBlockingDeque<Runnable>(128), new ThreadPoolExecutor.DiscardOldestPolicy());
        }
        return poolExecutor;
    }
}
