package pornhub.download.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.StreamProgress;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.cookie.GlobalCookieManager;
import cn.hutool.log.Log;
import cn.hutool.script.ScriptUtil;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import pornhub.download.action.DownloadAction;
import pornhub.download.entity.User;
import pornhub.download.entity.Video;

import javax.script.ScriptEngine;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.CookieManager;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static pornhub.download.Main.CONFIG;

/**
 * 视频工具
 */
public class VideoUtil {
    private static final Log LOG = Log.get(VideoUtil.class);
    private static final Gson GSON = new Gson();
    public static ExecutorService executor;

    /**
     * 获取视频mp4地址
     *
     * @param video
     * @return
     */
    public static String getMp4Url(Video video) {
        String videoUrl;
        String url = video.getUrl();
        try {
            CookieManager cookieManager = GlobalCookieManager.getCookieManager();
            HttpRequest httpRequest = HttpRequest.get(url);
            ProxyUtil.addProxy(httpRequest);

            Document parse = httpRequest
                    .setFollowRedirectsCookie(true)
                    .enableDefaultCookie()
                    .thenFunction(res -> Jsoup.parse(res.body()));
            Element body = parse.body();

            Element player = body.getElementById("player");
            if (Objects.isNull(player)) {
                return null;
            }
            Elements script = player.getElementsByTag("script");
            if (script.isEmpty()) {
                return null;
            }
            String html = script.get(0).html();

            String flashvars = ReUtil.get("(flashvars_\\d+)", html, 1);

            html = "playerObjList = {};\n" + html;
            html += html + ("\nvar json = " + "JSON.stringify(" + flashvars + ");");
            ScriptEngine jsEngine = ScriptUtil.createJsEngine();
            jsEngine.eval(html);
            String json = (String) jsEngine.eval("json");

            JsonObject jsonObject1 = GSON.fromJson(json, JsonObject.class);

            JsonArray mediaDefinitions = jsonObject1.getAsJsonArray("mediaDefinitions");
            List<JsonElement> collect = mediaDefinitions.asList()
                    .stream().filter(item -> item.getAsJsonObject().get("format").getAsString().equals("mp4")).collect(Collectors.toList());
            httpRequest = HttpRequest.get(collect
                    .get(0).getAsJsonObject().get("videoUrl").getAsString());
            ProxyUtil.addProxy(httpRequest);

            JsonArray objects = httpRequest
                    .setFollowRedirectsCookie(true)
                    .cookie(cookieManager.getCookieStore().getCookies())
                    .thenFunction(res -> GSON.fromJson(res.body(), JsonArray.class));
            List<JsonElement> javaList = objects.asList();
            JsonElement jsonObject = javaList.get(javaList.size() - 1);
            videoUrl = jsonObject.getAsJsonObject().get("videoUrl").getAsString();
        } catch (Exception e) {
            LOG.info(url);
            LOG.error(e, e.getMessage());
            ThreadUtil.sleep(10000);
            return getMp4Url(video);
        }
        return videoUrl;
    }

    /**
     * 下载视频
     *
     * @param mp4Url
     * @param file
     */
    public static void download(String mp4Url, File file, DownloadAction.DownloadInfo downloadInfo) {
        FileUtil.del(file + ".tmp");
        File tmpFile = new File(file + ".tmp");
        AtomicReference<OutputStream> outputStream = new AtomicReference<>(null);
        AtomicReference<InputStream> inputStream = new AtomicReference<>(null);
        try {
            HttpRequest httpRequest = HttpUtil.createGet(mp4Url, true);
            ProxyUtil.addProxy(httpRequest);
            httpRequest
                    .timeout(-1)
                    .then(res -> {
                        if (!res.isOk()) {
                            return;
                        }

                        outputStream.set(FileUtil.getOutputStream(tmpFile));
                        inputStream.set(res.bodyStream());
                        IoUtil.copy(inputStream.get(), outputStream.get(), 81920, new StreamProgress() {
                            long startTime;

                            @Override
                            public void start() {
                                LOG.info("开始下载 {}", file);
                                startTime = System.currentTimeMillis();
                            }

                            @Override
                            public void progress(long total, long progressSize) {
                                long contentLength = res.contentLength();
                                long currentTimeMillis = System.currentTimeMillis();
                                long totalTime = currentTimeMillis - startTime;
                                double downloadSpeed = progressSize / (totalTime / 1000.0) / (1024 * 1024);
                                downloadInfo
                                        .setSpeed(downloadSpeed)
                                        .setLength(contentLength)
                                        .setDownloadLength(progressSize);
                            }

                            @Override
                            public void finish() {
                                LOG.info("下载完成 {}", file);
                                downloadInfo.setEnd(Boolean.TRUE);
                            }
                        });
                        FileUtil.move(tmpFile, file, Boolean.TRUE);
                    });
        } catch (Exception e) {
            downloadInfo.setEnd(Boolean.TRUE)
                    .setError(Boolean.TRUE);
            LOG.error(e, e.getMessage());
            tmpFile.deleteOnExit();
        } finally {
            IoUtil.close(outputStream.get());
            IoUtil.close(inputStream.get());
        }
    }

    public static synchronized void download(Video video, DownloadAction.DownloadInfo downloadInfo) {
        File file = video.file();
        String mp4Url = getMp4Url(video);
        if (StrUtil.isBlank(mp4Url)) {
            downloadInfo.setEnd(Boolean.TRUE);
            return;
        }
        User user = video.getUser();
        while (((ThreadPoolExecutor) executor).getActiveCount() > CONFIG.getThreadNum() - 1) {
            ThreadUtil.sleep(500);
        }
        executor.submit(() -> {
            downloadInfo.setStart(Boolean.TRUE);
            UserUtil.downloadAvatar(user);
            int i = 0;
            Long retry = CONFIG.getRetry();
            Long retryInterval = CONFIG.getRetryInterval();
            Boolean retryWaitDoubled = CONFIG.getRetryWaitDoubled();
            do {
                try {
                    VideoUtil.download(mp4Url, file, downloadInfo);
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
            downloadInfo.setEnd(Boolean.TRUE);
        });
    }

}
