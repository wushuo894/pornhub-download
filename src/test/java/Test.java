import cn.hutool.core.io.FileTypeUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class Test {
    public static void main(String[] args) {
        List<File> files = FileUtil.loopFiles("Z:\\Media\\pornhub");
        for (File file : files) {
            if (!"mp4".equals(FileTypeUtil.getType(file))) {
                continue;
            }
            ProcessBuilder processBuilder = new ProcessBuilder("ffmpeg", "-v", "error", "-i", file.toString(), "-f", "null", "-");
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
        }
    }
}
