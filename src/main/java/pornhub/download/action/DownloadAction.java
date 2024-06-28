package pornhub.download.action;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import cn.hutool.http.server.action.Action;
import cn.hutool.log.Log;
import lombok.Data;
import lombok.experimental.Accessors;
import pornhub.download.annotation.Path;
import pornhub.download.entity.Video;
import pornhub.download.util.VideoUtil;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

@Path("/download")
public class DownloadAction implements Action {
    public static final Map<String, DownloadInfo> downloadInfoMap = new HashMap<>();
    public static final Log LOG = Log.get(DownloadAction.class);

    public static Boolean DOWNLOAD = Boolean.TRUE;

    @Override
    public synchronized void doAction(HttpServerRequest req, HttpServerResponse res) {
        int activeCount = ((ThreadPoolExecutor) VideoUtil.executor).getActiveCount();
        if (!DOWNLOAD || activeCount > 0) {
            res.sendOk();
            return;
        }
        DOWNLOAD = Boolean.FALSE;
        ThreadUtil.execute(() -> {
            List<ListAction.UserVO> list = ListAction.LIST;
            for (ListAction.UserVO userVO : list) {
                List<Video> videoList = userVO.getVideoList();
                for (Video video : videoList) {
                    String url = video.getUrl();
                    File file = video.file();
                    DownloadInfo downloadInfo = downloadInfoMap.get(url);
                    if (file.exists()) {
                        downloadInfo
                                .setLength(file.length())
                                .setDownloadLength(file.length())
                                .setStart(Boolean.TRUE)
                                .setEnd(Boolean.TRUE);
                    }
                }
            }
            for (ListAction.UserVO userVO : list) {
                List<Video> videoList = userVO.getVideoList();
                for (Video video : videoList) {
                    File file = video.file();
                    if (file.exists()) {
                        continue;
                    }
                    String url = video.getUrl();
                    DownloadInfo downloadInfo = downloadInfoMap.get(url);
                    VideoUtil.download(video, downloadInfo);
                    LOG.info(video.toString());
                }
            }
            DOWNLOAD = Boolean.TRUE;
        });
        res.sendOk();
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
    }
}
