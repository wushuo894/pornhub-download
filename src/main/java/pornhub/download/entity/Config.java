package pornhub.download.entity;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Config {
    private String path = "Z://Media/pornhub";
    private String url = "https://www.pornhub.com/users/wushuo894/subscriptions";
    private String cron = "";
}
