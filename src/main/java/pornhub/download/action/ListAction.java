package pornhub.download.action;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import cn.hutool.http.server.action.Action;
import cn.hutool.log.Log;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Data;
import lombok.experimental.Accessors;
import pornhub.download.annotation.Path;
import pornhub.download.entity.User;
import pornhub.download.entity.Video;
import pornhub.download.util.UserUtil;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static pornhub.download.Main.CONFIG;

@Path("/list")
public class ListAction implements Action {
    public static List<UserVO> LIST = new ArrayList<>();
    public static final Gson gson = new GsonBuilder().enableComplexMapKeySerialization().create();
    public static final Log LOG = Log.get(ListAction.class);

    /**
     * 加载出列表
     */
    public static synchronized void loadList() {
        List<User> subscriptions = UserUtil.getSubscriptions(CONFIG.getUrl());
        LOG.info("loadList start");
        AtomicInteger index = new AtomicInteger(0);
        LIST = subscriptions.stream()
                .map(it -> {
                    LOG.info("{}/{}\t{}",
                            index.incrementAndGet(),
                            subscriptions.size(),
                            it.getName());

                    List<Video> videoList = UserUtil.getVideoList(it);
                    for (Video video : videoList) {
                        String url = video.getUrl();
                        File file = video.file();
                        DownloadAction.DownloadInfo downloadInfo = DownloadAction.downloadInfoMap.getOrDefault(url,
                                new DownloadAction.DownloadInfo()
                                        .setStart(Boolean.FALSE)
                                        .setEnd(Boolean.FALSE)
                                        .setDownloadLength(0L)
                                        .setLength(1024L)
                                        .setSpeed(0.0)
                        );
                        DownloadAction.downloadInfoMap.put(url, downloadInfo);
                        if (file.exists()) {
                            downloadInfo
                                    .setLength(file.length())
                                    .setDownloadLength(file.length())
                                    .setStart(Boolean.TRUE)
                                    .setEnd(Boolean.TRUE);
                        }
                        video.setDownloadInfo(downloadInfo);
                    }
                    UserVO userVO = new UserVO();
                    userVO.setUser(it)
                            .setVideoList(videoList);
                    return userVO;
                }).collect(Collectors.toList());
        LOG.info("loadList end");
    }

    static {
        // 定时刷新
        ThreadUtil.execute(() -> {
            try {
                loadList();
            } catch (Exception e) {
                LOG.error(e);
            }
        });
    }


    @Override
    public void doAction(HttpServerRequest req, HttpServerResponse res) {
        try {
            String json = gson.toJson(LIST);
            res.setContentType("application/json; charset=utf-8");
            res.sendOk();
            PrintWriter writer = res.getWriter();
            writer.write(json);
            writer.flush();
            writer.close();
        } catch (Exception e) {
            LOG.error(e);
        }
    }

    @Data
    @Accessors(chain = true)
    public static class UserVO {
        private User user;
        private List<Video> videoList;
    }
}
