# ![app icon](https://github.com/wushuo894/pornhub-download/raw/master/logo.jpg) pornhub-download

[![GitHub Workflow Status](https://img.shields.io/github/actions/workflow/status/wushuo894/pornhub-download/maven.yml?branch=master)](https://github.com/wushuo894/pornhub-download/actions/workflows/maven.yml)
[![GitHub release (latest SemVer)](https://img.shields.io/github/v/release/wushuo894/pornhub-download?color=blue&label=download&sort=semver)](https://github.com/wushuo894/pornhub-download/releases/latest)
[![GitHub all releases](https://img.shields.io/github/downloads/wushuo894/pornhub-download/total?color=blue&label=github%20downloads)](https://github.com/wushuo894/pornhub-download/releases)
[![Docker Pulls](https://img.shields.io/docker/pulls/wushuo894/pornhub-download)](https://hub.docker.com/r/wushuo894/pornhub-download)


    docker run -d --name pornhub-download -v ./video:/video -v ./config:/config -p 7093:7093 -e TZ=Asia/Shanghai --restart always wushuo894/pornhub-download

| 路径      | 作用        |
|---------|-----------|
| /config | 存放配置文件    |
| /video  | 视频下载保存的位置 |


![截图](https://github.com/wushuo894/pornhub-download/raw/master/images/0.png)

![截图](https://github.com/wushuo894/pornhub-download/raw/master/images/1.png)

