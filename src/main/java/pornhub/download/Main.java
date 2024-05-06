package pornhub.download;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.cron.CronUtil;
import cn.hutool.cron.task.Task;
import cn.hutool.log.Log;
import com.google.gson.Gson;
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

    public static Config CONFIG = new Config();

    private static final Log LOG = Log.get(Main.class);

    public static void main(String[] args) {
        File configFile = new File("config.json5");
        if (!configFile.exists()) {
            FileUtil.writeString(ResourceUtil.readUtf8Str("config.json5"), configFile, StandardCharsets.UTF_8);
        }
        Gson gson = new Gson();
        CONFIG = gson.fromJson(FileUtil.readUtf8String(configFile), Config.class);
        Runnable runnable = () -> ThreadUtil.execAsync(() -> {
            try {
                List<User> subscriptions = UserUtil.getSubscriptions(CONFIG.getUrl());
                for (User user : subscriptions) {
                    UserUtil.downloadAvatar(user);
                }
                for (User user : subscriptions) {
                    LOG.info(user.toString());
                    downloadUser(user);
                }
            } catch (Exception e) {
                LOG.error(e, e.getMessage());
            }
        });
        String cron = CONFIG.getCron();
        if (StrUtil.isNotBlank(cron)) {
            LOG.info("定时任务开启");
            CronUtil.schedule(cron, (Task) () -> {
                LOG.info("定时任务");
                runnable.run();
            });
            CronUtil.start();
        } else {
            runnable.run();
        }
    }

    public static void downloadUser(User user) {
        String userName = user.getName();
        try {
            List<Video> videoList = UserUtil.getVideoList(user);
            for (Video video : videoList) {
                LOG.info(video.toString());
                String videoTitle = video.getTitle();
                File file = new File(CONFIG.getPath() + "/" + userName + "/" + videoTitle + ".mp4");
                if (file.exists()) {
                    LOG.info("已存在 {}", file);
                    continue;
                }
                download(video, file);
            }
        } catch (Exception e) {
            LOG.error(e, e.getMessage());
        }
    }

    public static void download(Video video, File file) {
        String mp4Url = VideoUtil.getMp4Url(video);
        if (StrUtil.isBlank(mp4Url)) {
            return;
        }
        if (file.exists()) {
            LOG.info("存在 {}", file);
            return;
        }
        int i = 0;
        Long retry = CONFIG.getRetry();
        Long retryInterval = CONFIG.getRetryInterval();
        Boolean retryWaitDoubled = CONFIG.getRetryWaitDoubled();
        do {
            try {
                VideoUtil.download(mp4Url, file);
                return;
            } catch (Exception e) {
                LOG.error(e, e.getMessage());
                i++;
                LOG.info(String.valueOf(file));
                LOG.info("重试 {}", i);
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
        LOG.info(String.valueOf(file));
        LOG.info("超过重试次数 {}", i);
    }

}
