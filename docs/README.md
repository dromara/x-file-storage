<h3 align="center">
	<img src="https://x-file-storage.xuyanwu.cn/assets/logo.svg" height="200px"  alt="logo"/><br />
	<span>原名 X Spring File Storage 现已捐赠至 <a target="_blank" href="https://dromara.org/zh">dromara</a> 开源组织<span>
</h3>

<p align="center">
    <a target="_blank" href="https://x-file-storage.dromara.org">x-file-storage.dromara.org</a> |
	<a target="_blank" href="https://x-file-storage.xuyanwu.cn">x-file-storage.xuyanwu.cn</a> |
	<a target="_blank" href="https://spring-file-storage.xuyanwu.cn">spring-file-storage.xuyanwu.cn</a>
</p>

<p align="center">
	<a target="_blank" href="https://search.maven.org/artifact/org.dromara/x-file-storage">
		<img src="https://img.shields.io/maven-central/v/org.dromara/x-file-storage.svg?label=Maven%20Central" />
	</a>
	<a target="_blank" href="https://www.apache.org/licenses/LICENSE-2.0">
		<img src="https://img.shields.io/badge/license-Apache%202-green.svg" />
	</a>
	<a target="_blank" href="https://www.oracle.com/technetwork/java/javase/downloads/index.html">
		<img src="https://img.shields.io/badge/JDK-8+-blue.svg" />
	</a>
	<a target="_blank" href='https://github.com/dromara/x-file-storage'>
		<img src="https://img.shields.io/github/stars/dromara/x-file-storage.svg?style=social" alt="github star"/>
	</a>
    <a href='https://gitee.com/dromara/x-file-storage'>
        <img src='https://gitee.com/dromara/x-file-storage/badge/star.svg?theme=dark' alt='star' />
    </a>
    <br />
    <a href='https://jq.qq.com/?_wv=1027&k=eGfeNqka'>
        <img src='https://img.shields.io/badge/QQ%E7%BE%A4-515706495-orange' alt='' />
    </a>
</p>

-------

# 📚简介

一行代码将文件存储到本地、FTP、SFTP、WebDAV、阿里云 OSS、华为云 OBS、七牛云 Kodo、腾讯云 COS、百度云 BOS、又拍云 USS、MinIO、
Amazon S3、GoogleCloud Storage、金山云 KS3、美团云 MSS、京东云 OSS、天翼云 OOS、移动 云EOS、沃云 OSS、
网易数帆 NOS、Ucloud US3、青云 QingStor、平安云 OBS、首云 OSS、IBM COS、其它兼容 S3 协议的存储平台。查看 [所有支持的存储平台](存储平台)

💡 通过 WebDAV 连接到 Alist 后，可以使用百度网盘、天翼云盘、阿里云盘、迅雷网盘等常见存储服务，查看 [Alist 支持的存储平台](https://alist-doc.nn.ci/docs/webdav)

GitHub：https://github.com/dromara/x-file-storage
<br />
Gitee：https://gitee.com/dromara/x-file-storage

-------

# 📜更新记录

这里是简要的更新记录，查看 [详细的更新记录](更新记录)

`2.0.0` 更改项目名、包名，优化项目结构、捐赠至 [dromara](https://dromara.org/zh) 开源社区，查看 [更新记录](更新记录?id=_200)
<br />
`1.0.3` 修复了 FileStorageClientFactory 未自动加载等问题，查看 [更新记录](更新记录?id=_103)
<br />
`1.0.2` 修复了华为云 OBS 未加载的问题，查看 [更新记录](更新记录?id=_102)
<br />
`1.0.1` 修复了 MultipartFile 无法正确获取文件名等问题，查看 [更新记录](更新记录?id=_101)
<br />
`1.0.0` 包含了大量功能更新与问题修复，例如解决了内存占用过大问题，支持大文件上传、  [脱离 SpringBoot 单独使用](脱离SpringBoot单独使用) 等，AmazonS3 和 GoogleCloudStorage 存储平台配置名称与之前版本不兼容，查看 [更新记录](更新记录?id=_100)

-------

# 📅更新计划

- 接入存储平台：HDFS、FastDFS、杉岩 OBS、Samba、NFS
- 大文件手动分片上传
- 复制或移动文件
- 文件内容预加载
- 上传无需强制获取 Size
- 新增 Access 模块，通过 HTTP、FTP、WebDAV 等协议对外提供接口，方便其它程序使用

-------

# 📦使用

阅读 [快速入门](快速入门) 开始使用吧！

-------

# 🏗️添砖加瓦

## 🎋分支说明

X File Storage 的源码分为两个分支，功能如下：

| 分支    | 作用                                              |
|-------|-------------------------------------------------| 
| main	 | 主分支，release 版本使用的分支，与中央库提交的 jar 一致，不接收任何 pr 或修改 |
| dev	  | 开发分支，接受修改或 pr                                   |

## 🐞提供bug反馈或建议

提交问题反馈请说明正在 X File Storage 版本、相关依赖库版本、配置参数及问题代码

[Gitee issue](https://gitee.com/dromara/x-file-storage/issues)<br/>
[GitHub issue](https://github.com/dromara/x-file-storage/issues)

## 🧬贡献代码的步骤

1. 在 Gitee 或者 Github 上 fork 项目到自己的 repo
2. 把 fork 过去的项目也就是你的项目 clone 到你的本地
3. 修改代码（记得一定要修改 dev 分支）
4. commit后push到自己的库（ dev 分支）
5. 登录 Gitee 或 Github 在你首页可以看到一个 pull request 按钮，点击它，填写一些说明信息，然后提交即可
6. 等待维护者合并

## 📐PR遵照的原则

欢迎任何人为 X File Storage 添砖加瓦，贡献代码，为了易用性和可维护性，需要提交的 pr（pull request）符合一些规范，规范如下：

1. 逻辑清晰、注释完备，不易理解的代码段的说明等信息，必要时请添加单元测试，如果愿意，也可以加上你的大名
2. 提交到 dev 分支，main 分支不接受任何 pr 或修改
3. 如果我们关闭了你的 issues 或者 pr 请查看回复内容，我们会在回复中做出解释

-------

# 📋使用公司及组织登记
X File Storage 感谢各位小伙伴的信任与支持，如果您已经在项目中使用了 X File Storage，希望您留下您的公司或组织信息（公司或组织名称、官网地址、展示 Logo 图片）

您的公司信息将在项目官网进行展示：<br/>
<a target="_blank" href="https://x-file-storage.dromara.org">x-file-storage.dromara.org</a><br/>
<a target="_blank" href="https://x-file-storage.xuyanwu.cn">x-file-storage.xuyanwu.cn</a><br/>
<a target="_blank" href="https://spring-file-storage.xuyanwu.cn">spring-file-storage.xuyanwu.cn</a>

[在 Gitee 上登记](https://gitee.com/dromara/x-file-storage/issues/I83Q6R)<br/>
[在 GitHub 上登记](https://github.com/dromara/x-file-storage/issues/114)

-------

# 🌏知识星球

<img src="https://x-file-storage.xuyanwu.cn/assets/zsxq.png" height="200px" alt="知识星球">

-------

# 💳捐赠
如果你觉得这个项目不错，可以点个 Star 或捐赠请作者吃包辣条~，不想打赏的话用支付宝扫最后一个码可以领取个红包，在此表示感谢^_^

<img src="https://x-file-storage.xuyanwu.cn/assets/wx.png" style="height: 350px;margin-right: 20px" alt="微信">
<img src="https://x-file-storage.xuyanwu.cn/assets/zfb.jpg" style="height: 350px;margin-right: 20px" alt="支付宝">
<img src="https://x-file-storage.xuyanwu.cn/assets/zfb2.jpg" style="height: 350px" alt="支付宝2">

或者点击以下链接，将页面拉到最下方点击“捐赠”即可

[Gitee上捐赠](https://gitee.com/dromara/x-file-storage)

