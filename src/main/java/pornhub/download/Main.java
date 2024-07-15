package pornhub.download;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.*;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.server.SimpleServer;
import cn.hutool.http.server.action.Action;
import cn.hutool.log.Log;
import com.google.gson.Gson;
import pornhub.download.action.RootAction;
import pornhub.download.annotation.Path;
import pornhub.download.entity.Config;
import pornhub.download.util.ExecutorUtil;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class Main {

    public static final Log LOG = Log.get(Main.class);
    public static Config CONFIG = new Config();

    public static void main(String[] args) {
        Map<String, String> env = System.getenv();
        String config = env.getOrDefault("CONFIG", "");
        File configFile = new File("config.json5");
        if (StrUtil.isNotBlank(config)) {
            configFile = new File(config + File.separator + "config.json5");
        }
        if (!configFile.exists()) {
            FileUtil.writeString(ResourceUtil.readUtf8Str("config.json5"), configFile, StandardCharsets.UTF_8);
        }
        Gson gson = new Gson();
        CONFIG = gson.fromJson(FileUtil.readUtf8String(configFile), Config.class);

        ExecutorUtil.init();

        SimpleServer server = HttpUtil.createServer(7093);

        server.addAction("/", new RootAction());
        Set<Class<?>> classes = ClassUtil.scanPackage("pornhub.download.action");
        for (Class<?> aClass : classes) {
            Path path = aClass.getAnnotation(Path.class);
            if (Objects.isNull(path)) {
                continue;
            }
            Object action = ReflectUtil.newInstanceIfPossible(aClass);
            server.addAction("/api" + path.value(), (Action) action);
        }

        boolean b = Boolean.parseBoolean(env.getOrDefault("VERIFY", "FALSE"));

        if (b) {
            ProcessBuilder processBuilder = new ProcessBuilder("tar", "-xvf", "/usr/app/ffmpeg-release-amd64-static.tar.xz");
            processBuilder.directory(new File("/usr/app/"));
            try {
                processBuilder.start().waitFor();
            } catch (Exception e) {
                LOG.error(e);
            }
        }
        ThreadUtil.execute(server::start);
    }
}
