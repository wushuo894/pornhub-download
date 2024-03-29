package pornhub.download;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.cron.CronUtil;
import cn.hutool.cron.task.Task;
import cn.hutool.http.HttpUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSON;
import pornhub.download.entity.Config;
import pornhub.download.entity.User;
import pornhub.download.entity.Video;
import pornhub.download.util.UserUtil;
import pornhub.download.util.VideoUtil;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Main {

    public static Config config = new Config();

    private static final Log log = Log.get(Main.class);

    public static void main(String[] args) {
        File configFile = new File("config.json5");
        if (!configFile.exists()) {
            FileUtil.writeString(ResourceUtil.readUtf8Str("config.json5"), configFile, StandardCharsets.UTF_8);
        }
        config = JSON.parseObject(FileUtil.readUtf8String(configFile), Config.class);
        Runnable runnable = () -> ThreadUtil.execAsync(() -> {
            List<User> subscriptions = UserUtil.getSubscriptions(config.getUrl());
            for (User user : subscriptions) {
                downloadUser(user);
            }
        });
        String cron = config.getCron();
        if (StrUtil.isNotBlank(cron)) {
            log.info("定时任务开启");
            CronUtil.schedule(cron, (Task) () -> {
                log.info("定时任务");
                runnable.run();
            });
            CronUtil.start();
        } else {
            runnable.run();
        }
    }

    public static void downloadUser(User user) {
        String userName = user.getName();
        String avatar = user.getAvatar();

        File avatarFile = new File(config.getPath() + "/model/" + userName + "/avatar.jpg");
        if (!avatarFile.exists() && StrUtil.isNotBlank(avatar)) {
            HttpUtil.downloadFile(avatar, avatarFile);
        }

        try {
            List<Video> videoList = UserUtil.getVideoList(user);
            for (Video video : videoList) {
                String videoTitle = video.getTitle();
                File file = new File(config.getPath() + "/model/" + userName + "/" + videoTitle + ".mp4");
                if (file.exists()) {
                    log.info("已存在 {}", file);
                    continue;
                }
                download(video, file);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void download(Video video, File file) {
        String mp4Url = VideoUtil.getMp4Url(video);
        if (StrUtil.isBlank(mp4Url)) {
            return;
        }
        if (file.exists()) {
            log.info("存在 {}", file);
            return;
        }
        int i = 0;
        Long retry = config.getRetry();
        Long retryInterval = config.getRetryInterval();
        Boolean retryWaitDoubled = config.getRetryWaitDoubled();
        do {
            try {
                VideoUtil.download(mp4Url, file);
                return;
            } catch (Exception e) {
                e.printStackTrace();
                i++;
                log.info(String.valueOf(file));
                log.info("重试 {}", i);
            }
            // 重试等待时间
            if (retryInterval > 0) {
                ThreadUtil.sleep(retryInterval, TimeUnit.SECONDS);
            }
            // 等待时间翻倍
            if (retryWaitDoubled) {
                retryInterval = retryInterval * 2;
            }
        } while (retry < 1 || retry > i);
        log.info(String.valueOf(file));
        log.info("超过重试次数 {}", i);
    }

}
