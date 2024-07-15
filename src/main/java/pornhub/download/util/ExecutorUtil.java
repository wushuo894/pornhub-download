package pornhub.download.util;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.log.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import static pornhub.download.Main.CONFIG;

public class ExecutorUtil {
    private static final Log log = Log.get(ExecutorUtil.class);
    public static ExecutorService executor;

    public static void init() {
        executor = Executors.newFixedThreadPool(CONFIG.getThreadNum());
    }

    public static void isInit() {
        if (ObjectUtil.isNull(executor)) {
            log.error("线程池未初始化");
        }
    }

    public static synchronized void await() {
        isInit();
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) executor;
        do {
            ThreadUtil.sleep(500);
        } while (threadPoolExecutor.getActiveCount() > CONFIG.getThreadNum() - 1);
    }

    public static synchronized void allAwait() {
        isInit();
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) executor;
        do {
            ThreadUtil.sleep(500);
        } while (threadPoolExecutor.getActiveCount() > 0);
    }

    public static synchronized void submit(Runnable task) {
        await();
        executor.submit(task);
    }
}
