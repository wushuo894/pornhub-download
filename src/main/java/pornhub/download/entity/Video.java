package pornhub.download.entity;

import lombok.Data;
import lombok.experimental.Accessors;
import pornhub.download.action.DownloadAction;

import java.io.File;
import java.io.Serializable;

import static pornhub.download.Main.CONFIG;

/**
 * 视频
 */
@Data
@Accessors(chain = true)
public class Video implements Serializable {
    /**
     * 地址
     */
    private String url;

    /**
     * 标题
     */
    private String title;

    /**
     * 用户
     */
    private User user;

    /**
     * 下载进度
     * @return
     */
    private DownloadAction.DownloadInfo downloadInfo;

    public File file() {
        String userName = user.getName();
        return new File(CONFIG.getPath() + "/" + userName + "/" + title + ".mp4");
    }
}
