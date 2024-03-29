package pornhub.download.util;

import cn.hutool.http.HttpRequest;
import pornhub.download.Main;
import pornhub.download.entity.Config;

public class ProxyUtil {
    public static HttpRequest addProxy(HttpRequest request) {
        request.setConnectionTimeout(3000)
                .timeout(3000);
        Config config = Main.config;
        Config.Proxy proxy = config.getProxy();
        if (proxy.hasProxy()) {
            request.setHttpProxy(proxy.getHost(), proxy.getPort());
        }
        return request;
    }
}
