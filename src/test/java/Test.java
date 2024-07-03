import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Test {
    public static final Log LOG = Log.get(Test.class);

    public static void main(String[] args) {
        int threadNum = 8;
        List<String> collect = Arrays.stream(ResourceUtil.readUtf8Str("files.txt").split("\n"))
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toList());
        List<File> files = ls("/Volumes/wushuo/Media/pornhub/")
                .stream()
                .filter(file -> file.getName().endsWith(".mp4"))
                .filter(file -> collect
                        .contains(file.getParentFile().toString()))
                .collect(Collectors.toList());
        ExecutorService executor = Executors.newFixedThreadPool(threadNum);
        CountDownLatch countDownLatch = new CountDownLatch(files.size());
        for (File file : files) {
            do {
                ThreadUtil.sleep(1000);
            } while (((ThreadPoolExecutor) executor).getActiveCount() > threadNum - 1);
            executor.submit(() -> {
                LOG.info(file.getName());
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
                .sorted(Comparator.comparingInt(file -> file.getName().trim().substring(0, 1).hashCode()))
                .flatMap(file -> {
                    if (file.isFile()) {
                        return Stream.of(file);
                    }
                    System.out.println(file);
                    return ls(file.toString()).stream();
                })
                .filter(File::isFile)
                .collect(Collectors.toList());
    }
}
