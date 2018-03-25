package com.roposo.core.util;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author muddassir on 4/5/17.
 */

public class BackgroundTask extends UniversalAsyncTask {
    private static ThreadPoolExecutor poolExecutor;

    public void executeOnPool(String... params){
        executeOnExecutor(getPoolExecutor(), params);
    }

    public static ThreadPoolExecutor getPoolExecutor() {
        if (poolExecutor == null) {
            poolExecutor = RoposoThreadPoolExecutor.getExecutor();
        }
        return poolExecutor;
    }
}
