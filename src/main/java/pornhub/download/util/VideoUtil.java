package pornhub.download.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.StreamProgress;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ReUtil;
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
import pornhub.download.entity.Video;

import javax.script.ScriptEngine;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.CookieManager;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * 视频工具
 */
public class VideoUtil {
    private static final Log log = Log.get(VideoUtil.class);
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
            log.info(url);
            log.error(e, e.getMessage());
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
    public static void download(String mp4Url, File file) {
        FileUtil.del(file + ".tmp");
        File tmpFile = new File(file + ".tmp");
        AtomicReference<OutputStream> outputStream = new AtomicReference<>(null);
        AtomicReference<InputStream> inputStream = new AtomicReference<>(null);
        try {
            outputStream.set(FileUtil.getOutputStream(tmpFile));
            HttpRequest httpRequest = HttpUtil.createGet(mp4Url, true);
            ProxyUtil.addProxy(httpRequest);
            httpRequest
                    .timeout(-1)
                    .then(res -> {
                        if (!res.isOk()) {
                            return;
                        }
                        inputStream.set(res.bodyStream());
                        IoUtil.copy(inputStream.get(), outputStream.get(), 81920, new StreamProgress() {
                            @Override
                            public void start() {
                                log.info("开始下载 {}", file);
                            }

                            @Override
                            public void progress(long total, long progressSize) {
                                System.out.print("\r" + (1.0 * progressSize / total * 100));
                            }

                            @Override
                            public void finish() {
                                log.info("下载完成 {}", file);
                            }
                        });
                        FileUtil.move(tmpFile, file, true);
                    });
        } catch (Exception e) {
            log.error(e, e.getMessage());
            tmpFile.deleteOnExit();
        } finally {
            IoUtil.close(outputStream.get());
            IoUtil.close(inputStream.get());
        }
    }

}
