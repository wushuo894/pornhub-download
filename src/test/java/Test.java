import cn.hutool.core.io.FileTypeUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.log.Log;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static pornhub.download.Main.CONFIG;

public class Test {
    public static final Log LOG = Log.get(Test.class);

    public static void main(String[] args) {
        int threadNum = 8;
        List<File> files = ls("G:\\test");
        ExecutorService executor = Executors.newFixedThreadPool(threadNum);
        CountDownLatch countDownLatch = new CountDownLatch(files.size());
        for (File file : files) {
            if (!"mp4".equals(FileTypeUtil.getType(file))) {
                continue;
            }
            while (((ThreadPoolExecutor) executor).getActiveCount() > threadNum - 1) {
                ThreadUtil.sleep(500);
            }
            executor.submit(() -> {
                ProcessBuilder processBuilder = new ProcessBuilder("ffmpeg",
                        "-v", "error",
                        "-i", file.toString(),
                        "-f", "null", "-");
                try {
                    Process process = processBuilder.start();
                    String s = IoUtil.readUtf8(process.getErrorStream());
                    if (s.contains("Error")) {
                        LOG.info("error {}", file);
                        FileUtil.del(file);
                    } else {
                        LOG.info("ok {}", file);
                    }
                } catch (IOException e) {
                    LOG.error(e);
                }
                countDownLatch.countDown();
                LOG.info("{} / {}", files.size(), countDownLatch.getCount());
            });
        }
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            LOG.error(e);
        }
        LOG.info("完成");
        executor.shutdown();
    }

    public static List<File> ls(String path) {
        File[] files = ObjectUtil.defaultIfNull(new File(path).listFiles(), new File[]{});
        return Arrays.stream(files)
                .flatMap(file -> {
                    if (file.isFile()) {
                        return Stream.of(file);
                    }
                    return ls(file.getPath()).stream();
                }).collect(Collectors.toList());
    }
}
