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
import pornhub.download.entity.Result;
import pornhub.download.entity.User;
import pornhub.download.entity.Video;
import pornhub.download.util.UserUtil;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static pornhub.download.Main.CONFIG;

@Path("/list")
public class ListAction implements Action {
    public static List<UserVO> LIST = new ArrayList<>();
    public static final Gson gson = new GsonBuilder().enableComplexMapKeySerialization().create();
    public static final Log LOG = Log.get(ListAction.class);

    public static final Status STATUS = new Status()
            .setLoadIng(Boolean.TRUE)
            .setList(LIST);

    /**
     * 加载出列表
     */
    public static synchronized void loadList() {
        List<User> subscriptions = UserUtil.getSubscriptions(CONFIG.getUrl());
        LOG.info("loadList start");
        AtomicInteger index = new AtomicInteger(0);
        for (User user : subscriptions) {
            LOG.info("{}/{}\t{}",
                    index.incrementAndGet(),
                    subscriptions.size(),
                    user.getName());

            List<Video> videoList = new ArrayList<>();
            UserVO userVO = new UserVO()
                    .setUser(user)
                    .setVideoList(videoList);
            LIST.add(userVO);
            videoList.addAll(UserUtil.getVideoList(user));
            for (Video video : videoList) {
                File file = video.file();
                DownloadAction.DownloadInfo downloadInfo = new DownloadAction.DownloadInfo()
                        .setStart(Boolean.FALSE)
                        .setEnd(Boolean.FALSE)
                        .setError(Boolean.FALSE)
                        .setDownloadLength(0L)
                        .setLength(1024L)
                        .setSpeed(0.0);
                if (file.exists()) {
                    downloadInfo
                            .setLength(file.length())
                            .setDownloadLength(file.length())
                            .setStart(Boolean.TRUE)
                            .setEnd(Boolean.TRUE);
                }
                video.setDownloadInfo(downloadInfo);
            }
        }
        STATUS.setLoadIng(Boolean.FALSE);
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
            String json = gson.toJson(Result.success(STATUS));
            res.setContentType("application/json; charset=utf-8");
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
    public static class Status {
        /**
         * 是否正在加载
         */
        private Boolean loadIng;

        /**
         * 列表
         */
        private List<UserVO> list;
    }

    @Data
    @Accessors(chain = true)
    public static class UserVO {
        private User user;
        private List<Video> videoList;
    }
}
