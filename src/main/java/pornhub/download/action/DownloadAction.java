package pornhub.download.action;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import cn.hutool.http.server.action.Action;
import cn.hutool.log.Log;
import com.google.gson.Gson;
import lombok.Data;
import lombok.experimental.Accessors;
import pornhub.download.annotation.Path;
import pornhub.download.entity.Result;
import pornhub.download.entity.Video;
import pornhub.download.util.ExecutorUtil;
import pornhub.download.util.VideoUtil;

import java.io.File;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

@Path("/download")
public class DownloadAction implements Action {
    public static final Log LOG = Log.get(DownloadAction.class);
    public static final Gson GSON = new Gson();
    public static Boolean DOWNLOAD = Boolean.TRUE;

    @Override
    public synchronized void doAction(HttpServerRequest req, HttpServerResponse res) {
        res.setContentType("application/json; charset=utf-8");
        PrintWriter writer = res.getWriter();
        int activeCount = ((ThreadPoolExecutor) ExecutorUtil.executor).getActiveCount();

        if (ListAction.STATUS.getLoadIng()) {
            writer.write(GSON.toJson(Result.error().setMessage("还未加载完成")));
            writer.flush();
            writer.close();
            return;
        }

        if (!DOWNLOAD || activeCount > 0) {
            writer.write(GSON.toJson(Result.error().setMessage("已经开始下载了")));
            writer.flush();
            writer.close();
            return;
        }
        DOWNLOAD = Boolean.FALSE;
        ThreadUtil.execute(() -> {
            List<ListAction.UserVO> list = ListAction.LIST;
            for (ListAction.UserVO userVO : list) {
                List<Video> videoList = userVO.getVideoList();
                for (Video video : videoList) {
                    File file = video.file();
                    if (file.exists()) {
                        LOG.info("{} 已存在", file);
                        continue;
                    }
                    DownloadInfo downloadInfo = video.getDownloadInfo();
                    VideoUtil.download(video, downloadInfo);
                    LOG.info(video.toString());
                }
            }
            DOWNLOAD = Boolean.TRUE;
        });
        writer.write(GSON.toJson(Result.success().setMessage("开始下载")));
        writer.flush();
        writer.close();
    }

    @Data
    @Accessors(chain = true)
    public static class DownloadInfo implements Serializable {
        /**
         * 总字节
         */
        private Long length;

        /**
         * 已下载的字节
         */
        private Long downloadLength;

        /**
         * 是否已经开始
         */
        private Boolean start;

        /**
         * 下载完成
         */
        private Boolean end;

        /**
         * 异常
         */
        private Boolean error;

        /**
         * 下载速度
         */
        private Double speed;

        /**
         * 剩余时间
         */
        private Double timeRemaining;
    }
}
