package pornhub.download.entity;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 视频
 */
@Data
@Accessors(chain = true)
public class Video {
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
}
