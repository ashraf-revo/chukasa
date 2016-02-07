# chukasa 

Web camera, video file, PT2 and PT3 HTTP Live Streaming (HLS) Server for OS X, iOS and tvOS

[![Build Status](https://travis-ci.org/hirooka/chukasa.svg?branch=master)](https://travis-ci.org/hirooka/chukasa) [![Build Status](https://circleci.com/gh/hirooka/chukasa.png?style=shield)](https://circleci.com/gh/hirooka/chukasa)

# 1. Overview

* Web camera real-time transcoding and streaming
* Video file transcoding and streaming
* PT2 / PT3 real-time transcoding and streaming
* PT2 / PT3 recording (EXPERIMENTAL)
* PT2 / PT3 追っかけ再生 (EXPERIMENTAL)

# 2. Client

* OX 11.10 Safari
* iOS 9 Safari
* iOS App (chukasa-ios) [https://github.com/hirooka/chukasa-ios](https://github.com/hirooka/chukasa-ios)
* tvOS (play video via AirPlay)

# 3. Server Installation

Ubuntu, Docker, AWS Elastic Beanstalk といった様々な環境で動作します．  

## Examples of use

|   | Streaming only<br>(Runnable jar) | Streaming and Recording<br>(Runnable jar + MongoDB) |
|:---:|:---:|:---:|
| Ubuntu<br>(Local) | [procedure](procedure/procedure_ubuntu_local_jar.txt) | [procedure](procedure/procedure_ubuntu_local_jar_db.txt) |
| Ubuntu<br>(Docker) | [procedure](procedure/procedure_ubuntu_local_docker_jar.txt) | [procedure](procedure/procedure_ubuntu_local_docker_jar_db.txt) |
| AWS Elastic Beanstalk<br>(Java) | [procedure](procedure/procedure_aws_elastic_beanstalk_jar.txt) | N/A |
| AWS Elastic Beanstalk<br>(Docker) | [procedure](procedure/procedure_aws_elastic_beanstalk_docker_jar.txt) | N/A |
| AWS Elastic Beanstalk<br>(Multi-container Docker) | N/A | [procedure](procedure/procedure_aws_elastic_beanstalk_multi_container_docker_jar_db.txt) |


# 4. Server Requirements

## 4.1. Software

* Ubuntu 15.10
* Oracle Java 8
* Docker 1.10.0 (アプリケーションを Docker で実行する場合）
* PT2 driver or PT3 driver (PT2/PT3 ストリーミングを行う場合)
* Web camera (Web camera ストリーミングを行う場合) 

## 4.1. Hardware

* TBD