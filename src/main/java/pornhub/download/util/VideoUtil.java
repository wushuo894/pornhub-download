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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.CookieManager;
import java.util.List;
import java.util.Map;
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
            ThreadUtil.sleep(3000);
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
    public static void download(String mp4Url, File file, DownloadAction.DownloadInfo downloadInfo) throws Exception {
//        FileUtil.del(file + ".tmp");
        File tmpFile = new File(file + ".tmp");
        AtomicReference<OutputStream> outputStream = new AtomicReference<>(null);
        AtomicReference<InputStream> inputStream = new AtomicReference<>(null);
        AtomicReference<Boolean> ok = new AtomicReference<>(Boolean.FALSE);
        try {
            HttpRequest httpRequest = HttpUtil.createGet(mp4Url, true);
            ProxyUtil.addProxy(httpRequest);
            httpRequest
                    .timeout(-1)
                    .then(res -> {
                        if (!res.isOk()) {
                            return;
                        }

                        long contentLength = res.contentLength();
                        outputStream.set(FileUtil.getOutputStream(tmpFile));
                        inputStream.set(res.bodyStream());

                        if (tmpFile.exists()) {
                            try {
                                inputStream.get().skip(tmpFile.length());
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }


                        downloadInfo
                                .setLength(contentLength);
                        IoUtil.copy(inputStream.get(), outputStream.get(), 81920, contentLength, new StreamProgress() {
                            @Override
                            public void start() {
                                LOG.info("开始下载 {}", file);

                                ThreadUtil.execute(() -> {
                                    while (!ok.get()) {
                                        Long length = downloadInfo.getLength();
                                        Long downloadLength = downloadInfo.getDownloadLength();
                                        if (downloadLength < 1) {
                                            continue;
                                        }
                                        ThreadUtil.sleep(1000);
                                        Long currentDownloadLength = downloadInfo.getDownloadLength();
                                        long current = currentDownloadLength - downloadLength;
                                        double timeRemaining = 99999.0;
                                        if (current < 1) {
                                            downloadInfo.setSpeed(0.0)
                                                    .setTimeRemaining(timeRemaining);
                                            continue;
                                        }
                                        long residue = length - currentDownloadLength;
                                        if (residue > 0) {
                                            timeRemaining = ((residue * 1.0) / current) / 60;
                                        }
                                        double downloadSpeed = current / 1.0 / (1024 * 1024);
                                        downloadInfo.setSpeed(downloadSpeed)
                                                .setTimeRemaining(timeRemaining);
                                    }
                                    downloadInfo.setSpeed(0.0);
                                });
                            }

                            @Override
                            public void progress(long total, long progressSize) {
                                downloadInfo
                                        .setDownloadLength(progressSize);
                            }

                            @Override
                            public void finish() {
                            }
                        });
                        if (downloadInfo.getDownloadLength() != contentLength) {
                            LOG.info("下载时出现异常");
                            return;
                        }

                        Map<String, String> env = System.getenv();
                        boolean b = Boolean.parseBoolean(env.getOrDefault("VERIFY", "FALSE"));
                        if (b) {
                            ProcessBuilder processBuilder = new ProcessBuilder("/usr/app/ffmpeg-7.0.1-amd64-static/ffmpeg", "-v", "error", "-i", tmpFile.toString(), "-f", "null", "-");
                            try {
                                Process process = processBuilder.start();
                                String s = IoUtil.readUtf8(process.getErrorStream());
                                process.waitFor();
                                if (s.contains("Error")) {
                                    LOG.info("视频异常 {}", file);
                                    return;
                                }
                                LOG.info("下载完成 {}", file);
                                downloadInfo.setEnd(Boolean.TRUE);
                                FileUtil.move(tmpFile, file, Boolean.TRUE);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                            return;
                        }

                        LOG.info("下载完成 {}", file);
                        downloadInfo.setEnd(Boolean.TRUE);
                        FileUtil.move(tmpFile, file, Boolean.TRUE);
                    });
        } catch (RuntimeException e) {
            throw e;
        } finally {
            IoUtil.close(outputStream.get());
            IoUtil.close(inputStream.get());
            ok.set(Boolean.TRUE);
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
        ExecutorUtil.submit(() -> {
            downloadInfo.setStart(Boolean.TRUE)
                    .setEnd(Boolean.FALSE)
                    .setError(Boolean.FALSE);
            UserUtil.downloadAvatar(user);
            int i = 0;
            Long retry = CONFIG.getRetry();
            Long retryInterval = CONFIG.getRetryInterval();
            Boolean retryWaitDoubled = CONFIG.getRetryWaitDoubled();
            do {
                try {
                    VideoUtil.download(mp4Url, file, downloadInfo);
                    if (downloadInfo.getEnd()) {
                        return;
                    }
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
            downloadInfo
                    .setSpeed(0.0)
                    .setError(Boolean.TRUE)
                    .setEnd(Boolean.TRUE);
        });
    }

}
