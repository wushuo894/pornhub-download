package pornhub.download.util;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.http.HttpRequest;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import pornhub.download.entity.User;
import pornhub.download.entity.Video;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 用户工具
 */
public class UserUtil {
    /**
     * 获取订阅用户
     *
     * @param url
     * @return
     */
    public static List<User> getSubscriptions(String url) {
        try {
            HttpRequest httpRequest = HttpRequest.get(url);
            ProxyUtil.addProxy(httpRequest);

            String body = httpRequest
                    .execute()
                    .body();

            Element moreData = Jsoup.parse(body)
                    .body()
                    .getElementById("moreData");
            if (Objects.isNull(moreData)) {
                return List.of();
            }

            return moreData
                    .getElementsByTag("a")
                    .stream()
                    .map(el -> {
                        String href = el.attr("href");
                        if (!href.startsWith("/model/")) {
                            return null;
                        }
                        if (!el.classNames().contains("userLink")) {
                            return null;
                        }
                        href = "https://www.pornhub.com" + href;
                        Element img = el.getElementsByTag("img").get(0);
                        String src = img.attr("src");
                        String title = img.attr("title");
                        return new User()
                                .setName(removeNbsp(title))
                                .setUrl(href)
                                .setAvatar(src);
                    }).filter(Objects::nonNull).collect(Collectors.toList());
        } catch (Exception e) {
            ThreadUtil.sleep(10000);
            return getSubscriptions(url);
        }
    }

    public static String removeNbsp(String text) {
        text = text.trim();
        String nbsp = " ";
        if (text.startsWith(nbsp)) {
            text = text.substring(nbsp.length());
        } else if (text.endsWith(nbsp)) {
            text = text.substring(0, text.length() - nbsp.length());
        } else {
            return text.trim();
        }
        return removeNbsp(text);
    }

    /**
     * 获取视频列表
     *
     * @param user
     * @return
     */
    public static List<Video> getVideoList(User user) {
        try {
            String url = user.getUrl();

            HttpRequest httpRequest = HttpRequest.get(url + "/videos?");
            ProxyUtil.addProxy(httpRequest);

            Document parse = Jsoup.parse(httpRequest
                    .execute()
                    .body());
            int maxPageNumber = parse.body().getElementsByClass("page_number")
                    .stream()
                    .map(Element::text)
                    .filter(NumberUtil::isNumber)
                    .mapToInt(Integer::parseInt)
                    .max().orElse(1);

            maxPageNumber = Math.max(maxPageNumber, 1);

            List<Video> videoList = new ArrayList<>();

            for (int i = 1; i <= maxPageNumber; i++) {
                Document page = parse;
                if (i > 1) {
                    httpRequest = HttpRequest.get(url + "/videos");
                    ProxyUtil.addProxy(httpRequest);
                    page = Jsoup.parse(httpRequest
                            .form("page", i)
                            .execute()
                            .body());
                }
                Elements videoUList = page.getElementsByClass("videoUList");
                for (Element element : videoUList) {
                    Elements videoPreviewBg = element.getElementsByClass("videoPreviewBg");
                    for (Element video : videoPreviewBg) {
                        if (!video.tag().getName().equalsIgnoreCase("a")) {
                            continue;
                        }
                        var videoTitle = video.attr("title");
                        var ls = List.of("/", "\\", ":", "?", "*", "|", ">", "<", "\"");
                        for (String l : ls) {
                            videoTitle = videoTitle.replace(l, "");
                        }
                        String href = "https://www.pornhub.com" + video.attr("href");
                        videoList.add(new Video()
                                .setTitle(removeNbsp(videoTitle))
                                .setUser(user)
                                .setUrl(href));
                    }
                }
            }

            return videoList;
        } catch (Exception e) {
            ThreadUtil.sleep(10000);
            return getVideoList(user);
        }
    }

}
