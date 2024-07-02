import cn.hutool.core.io.FileTypeUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static pornhub.download.Main.CONFIG;

public class Test {
    public static void main(String[] args) {
        List<File> files = ls("/Volumes/wushuo/Media/pornhub");
        ExecutorService executor = Executors.newFixedThreadPool(8);
        for (File file : files) {
            if (!"mp4".equals(FileTypeUtil.getType(file))) {
                continue;
            }
            while (((ThreadPoolExecutor) executor).getActiveCount() > 8 - 1) {
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
                        System.out.println("error\t" + file);
                        FileUtil.del(file);
                    } else {
                        System.out.println("ok\t" + file);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
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
