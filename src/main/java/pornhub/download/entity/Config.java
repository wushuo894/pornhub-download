package pornhub.download.entity;

import cn.hutool.core.util.StrUtil;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Objects;

@Data
@Accessors(chain = true)
public class Config implements Serializable {

    /**
     * 下载位置
     */
    private String path;

    /**
     * 订阅
     */
    private String url;

    /**
     * 重试次数 0 不重试 -1 一直重试 默认重试三次
     */
    private Long retry;

    /**
     * 重试间隔时间 单位 秒
     */
    private Long retryInterval;

    /**
     * 重试等待时间翻倍 默认 true
     */
    private Boolean retryWaitDoubled;

    /**
     * 代理 如 127.0.0.1:8080
     */
    private Proxy proxy;

    @Data
    public static class Proxy {
        /**
         * 主机
         */
        private String host;
        /**
         * 端口
         */
        private Integer port;

        /**
         * 是否已配置
         *
         * @return
         */
        public Boolean hasProxy() {
            return StrUtil.isNotBlank(host) && Objects.nonNull(port);
        }
    }
}
