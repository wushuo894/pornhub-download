import cn.hutool.core.io.FileUtil;

import java.io.File;

public class Test1 {
    public static void main(String[] args) {
        File[] ls = FileUtil.ls("/Volumes/wushuo/Media/pornhub/");
        for (File l : ls) {
            System.out.println(l);
        }
    }
}
