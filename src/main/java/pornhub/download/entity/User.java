package pornhub.download.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * 用户
 */
@Data
@Accessors(chain = true)
public class User implements Serializable {
    /**
     * 名称
     */
    private String name;

    /**
     * 地址
     */
    private String url;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 视频
     */
    private List<Video> videoList;
}
