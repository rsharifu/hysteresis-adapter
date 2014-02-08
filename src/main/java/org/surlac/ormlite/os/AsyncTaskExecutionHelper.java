package org.surlac.ormlite.os;

import android.annotation.TargetApi;
import android.os.AsyncTask;
import android.os.Build;

import java.util.concurrent.Executor;

/**
 * Work around for
 * <a href="http://code.google.com/p/android/issues/detail?id=20941">Bug 20941</a>
 *
 * @author Ruslan A. Sharifullin
 * @version 23.08.12
 */
public class AsyncTaskExecutionHelper {
    static class HoneycombExecutionHelper {
        @TargetApi(11)
        public static <P> void execute(AsyncTask<P, ?, ?> asyncTask, boolean parallel, P... params) {
            Executor executor = parallel ? AsyncTask.THREAD_POOL_EXECUTOR : AsyncTask.SERIAL_EXECUTOR;
            asyncTask.executeOnExecutor(executor, params);
        }
    }

    public static <P> void executeParallel(AsyncTask<P, ?, ?> asyncTask, P... params) {
        execute(asyncTask, true, params);
    }

    public static <P> void executeSerial(AsyncTask<P, ?, ?> asyncTask, P... params) {
        execute(asyncTask, false, params);
    }

    private static <P> void execute(AsyncTask<P, ?, ?> asyncTask, boolean parallel, P... params) {
        if (Build.VERSION.SDK_INT >= 11) {
            HoneycombExecutionHelper.execute(asyncTask, parallel, params);
        } else {
            asyncTask.execute(params);
        }
    }
}