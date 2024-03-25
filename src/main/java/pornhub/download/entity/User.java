package pornhub.download.entity;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 用户
 */
@Data
@Accessors(chain = true)
public class User {
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
}
