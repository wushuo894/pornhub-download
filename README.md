# ![app icon](https://github.com/wushuo894/pornhub-download/raw/master/logo.jpg) pornhub-download

    docker run -d --name pornhub-download -v ./video:/video -v ./config:/config -p 7093:7093 -e TZ=Asia/Shanghai --restart always wushuo894/pornhub-download

| 路径      | 作用        |
|---------|-----------|
| /config | 存放配置文件    |
| /video  | 视频下载保存的位置 |


![截图](https://github.com/wushuo894/pornhub-download/raw/master/images/0.png)

![截图](https://github.com/wushuo894/pornhub-download/raw/master/images/1.png)

