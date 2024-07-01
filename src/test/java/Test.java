import cn.hutool.core.io.FileTypeUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class Test {
    public static void main(String[] args) {
        final byte[] data = {
                0x21, 0x11, 0x45, 0x00, 0x14, 0x50, 0x01, 0x47,
        };
        final byte[] dataA = new byte[data.length];
        List<File> files = FileUtil.loopFiles("Z:\\Media\\pornhub");
        for (File file : files) {
            if (!"mp4".equals(FileTypeUtil.getType(file))) {
                continue;
            }
            BufferedInputStream inputStream = FileUtil.getInputStream(file);
            try {
                inputStream.skip(file.length()-data.length);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try {
                inputStream.read(dataA,0,dataA.length);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (Arrays.equals(dataA,data)) {
                System.out.println("ok " + file);
            }else {
                System.out.println("no " + file);
            }
            IoUtil.close(inputStream);
        }
    }
}
