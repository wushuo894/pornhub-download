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
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import pornhub.download.entity.Video;

import javax.script.ScriptEngine;
import java.io.File;
import java.io.OutputStream;
import java.net.CookieManager;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 视频工具
 */
public class VideoUtil {
    private static final Log log = Log.get(VideoUtil.class);

    /**
     * 获取视频mp4地址
     *
     * @param video
     * @return
     */
    public static String getMp4Url(Video video) {
        String videoUrl = null;
        String url = video.getUrl();
        try {
            CookieManager cookieManager = GlobalCookieManager.getCookieManager();
            HttpRequest httpRequest = HttpRequest.get(url);
            ProxyUtil.addProxy(httpRequest);
            Document parse = Jsoup.parse(httpRequest
                    .setFollowRedirectsCookie(true)
                    .enableDefaultCookie()
                    .execute()
                    .body());
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
            JSONObject jsonObject1 = JSON.parseObject(json);

            JSONArray mediaDefinitions = jsonObject1.getJSONArray("mediaDefinitions");
            List<JSONObject> collect = mediaDefinitions.toJavaList(JSONObject.class)
                    .stream().filter(item -> item.getString("format").equals("mp4")).collect(Collectors.toList());
            httpRequest = HttpRequest.get(collect
                    .get(0).getString("videoUrl"));
            ProxyUtil.addProxy(httpRequest);
            JSONArray objects = JSON.parseArray(httpRequest
                    .setFollowRedirectsCookie(true)
                    .cookie(cookieManager.getCookieStore().getCookies())
                    .execute().body());
            List<JSONObject> javaList = objects.toJavaList(JSONObject.class);
            JSONObject jsonObject = javaList.get(javaList.size() - 1);
            videoUrl = jsonObject.getString("videoUrl");
        } catch (Exception e) {
            log.info(url);
            e.printStackTrace();
            ThreadUtil.sleep(10000);
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
        File tmpFile = new File(file + ".tmp");
        FileUtil.del(tmpFile);
        OutputStream outputStream = null;
        try {
            outputStream = FileUtil.getOutputStream(tmpFile);
            HttpRequest httpRequest = HttpUtil.createGet(mp4Url, true);
            ProxyUtil.addProxy(httpRequest);
            httpRequest
                    .timeout(-1)
                    .execute()
                    .writeBody(outputStream, true, new StreamProgress() {
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
        } catch (Exception e) {
            e.printStackTrace();
        }
        IoUtil.close(outputStream);
    }

}
