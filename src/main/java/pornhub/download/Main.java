package pornhub.download;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.*;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.server.SimpleServer;
import cn.hutool.http.server.action.Action;
import com.google.gson.Gson;
import pornhub.download.action.RootAction;
import pornhub.download.annotation.Path;
import pornhub.download.entity.Config;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Set;

public class Main {

    public static Config CONFIG = new Config();

    public static void main(String[] args) {
        File configFile = new File("config.json5");
        if (!configFile.exists()) {
            FileUtil.writeString(ResourceUtil.readUtf8Str("config.json5"), configFile, StandardCharsets.UTF_8);
        }
        Gson gson = new Gson();
        CONFIG = gson.fromJson(FileUtil.readUtf8String(configFile), Config.class);

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

        ThreadUtil.execute(server::start);
    }
}
