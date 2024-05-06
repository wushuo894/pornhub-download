package pornhub.download.util;

import cn.hutool.http.HttpRequest;
import cn.hutool.log.Log;
import pornhub.download.Main;
import pornhub.download.entity.Config;

public class ProxyUtil {
    private static final Log log = Log.get(ProxyUtil.class);

    public static void addProxy(HttpRequest request) {
        String url = request.getUrl();
        request.setConnectionTimeout(10000)
                .setFollowRedirects(true);
        Config config = Main.CONFIG;
        Config.Proxy proxy = config.getProxy();
        if (proxy.hasProxy()) {
            log.info("proxy ===> {}", url);
            request.setHttpProxy(proxy.getHost(), proxy.getPort());
        } else {
            log.info("not proxy ===> {}", url);
        }
    }
}
