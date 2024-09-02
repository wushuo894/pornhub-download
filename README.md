# ![app icon](https://github.com/wushuo894/pornhub-download/raw/master/logo.jpg) pornhub-download

[![GitHub Workflow Status](https://img.shields.io/github/actions/workflow/status/wushuo894/pornhub-download/maven.yml?branch=master)](https://github.com/wushuo894/pornhub-download/actions/workflows/maven.yml)
[![GitHub release (latest SemVer)](https://img.shields.io/github/v/release/wushuo894/pornhub-download?color=blue&label=download&sort=semver)](https://github.com/wushuo894/pornhub-download/releases/latest)
[![GitHub all releases](https://img.shields.io/github/downloads/wushuo894/pornhub-download/total?color=blue&label=github%20downloads)](https://github.com/wushuo894/pornhub-download/releases)
[![Docker Pulls](https://img.shields.io/docker/pulls/wushuo894/pornhub-download)](https://hub.docker.com/r/wushuo894/pornhub-download)

Emby 支持拼音首字母排序

## Docker部署

    docker run -d --name emby-pinyin -p 9198:9198 -e PORT="9198" -e HOST="http://192.168.5.4:8096" -e KEY="" -e ITEM="电影,番剧" -e CRON="0 1 * * *" -e RUN="TRUE" -e TZ=Asia/Shanghai --restart always wushuo894/emby-pinyin

| 参数   | 作用          | 默认值                   |
|------|-------------|-----------------------|
| PORT | 端口号         | 9198                  |
| HOST | emby 地址     | http://192.168.5.4:8096 |
| KEY  | API Key     | 空                     |
| ITEM | 媒体库(可以用,分割) | 电影,番剧                 |
| RUN  | 启动时运行       | TRUE                  |
| CRON | 计划任务        | 0 1 * * *             |
| TZ   | 时区          | Asia/Shanghai         |

## 设置 Webhooks

| 参数     | 设置               |
|--------|------------------|
| URL    | http://ip:端口     |
| 请求类型   | application/json |
| Events | 媒体库/新媒体已添加       |

![https://github.com/wushuo894/pornhub-download/raw/master/images/webhooks.png](images/webhooks.png)


    docker run -d --name pornhub-download -v ./video:/video -v ./config:/config -p 7093:7093 -e TZ=Asia/Shanghai --restart always wushuo894/pornhub-download

| 路径      | 作用        |
|---------|-----------|
| /config | 存放配置文件    |
| /video  | 视频下载保存的位置 |


![截图](https://github.com/wushuo894/pornhub-download/raw/master/images/0.png)

![截图](https://github.com/wushuo894/pornhub-download/raw/master/images/1.png)

