<h3 align="center">
	<img src="https://x-file-storage.xuyanwu.cn/assets/logo.svg" height="200px"  alt="logo"/><br />
	<span>原名 X Spring File Storage 现已捐赠至 <a target="_blank" href="https://dromara.org/zh">dromara</a> 开源组织</span>
</h3>

<p align="center">
    <a target="_blank" href="https://x-file-storage.dromara.org">x-file-storage.dromara.org</a> |
	<a target="_blank" href="https://x-file-storage.xuyanwu.cn">x-file-storage.xuyanwu.cn</a> |
	<a target="_blank" href="https://spring-file-storage.xuyanwu.cn">spring-file-storage.xuyanwu.cn</a>
</p>

<p align="center">
	<a target="_blank" href="https://central.sonatype.com/search?q=org.dromara.x-file-storage">
		<img src="https://img.shields.io/maven-central/v/org.dromara.x-file-storage/x-file-storage-core.svg?label=Maven%20Central" />
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
    <a target="_blank" href='https://gitee.com/dromara/x-file-storage'>
        <img src='https://gitee.com/dromara/x-file-storage/badge/star.svg?theme=dark' alt='star' />
    </a>
    <a href='https://gitcode.com/dromara/x-file-storage'>
        <img src='https://gitcode.com/dromara/x-file-storage/star/badge.svg' alt='star' />
    </a>
    <a target="_blank" href='https://jq.qq.com/?_wv=1027&k=eGfeNqka'>
        <img src='https://img.shields.io/badge/QQ%E7%BE%A4-515706495-orange' alt='515706495' />
    </a>
    <a target="_blank" href='https://x-file-storage.xuyanwu.cn/#/?id=🌶%ef%b8%8fvip交流群'>
        <img src='https://img.shields.io/badge/付费-VIP交流群-brightgreen' alt='' />
    </a>
</p>

[tg.md](https://x-file-storage.xuyanwu.cn/assets/tg/tg.md ':include')

-------

# 📚简介

一行代码将文件存储到本地、FTP、SFTP、WebDAV、阿里云 OSS、华为云 OBS、七牛云 Kodo、腾讯云 COS、百度云 BOS、又拍云 USS、MinIO、
Amazon S3、Amazon S3 V2、GoogleCloud Storage、FastDFS、 Azure Blob Storage、Mongo GridFS、Mongo GridFS、go-fastdfs、
火山引擎 TOS、Cloudflare R2、金山云 KS3、美团云 MSS、京东云 OSS、天翼云 OOS、移动 云EOS、沃云 OSS、
网易数帆 NOS、Ucloud US3、青云 QingStor、平安云 OBS、首云 OSS、IBM COS、其它兼容 S3 协议的存储平台。查看 [所有支持的存储平台](存储平台)

💡 通过 WebDAV 连接到 Alist 后，可以使用百度网盘、天翼云盘、阿里云盘、迅雷网盘等常见存储服务，查看 [Alist 支持的存储平台](https://alist.nn.ci/zh/guide/webdav.html#webdav-%E5%AD%98%E5%82%A8%E6%94%AF%E6%8C%81)

🚚 支持在不同存储平台之间迁移文件，详情查看 [迁移文件](迁移文件)

GitHub：https://github.com/dromara/x-file-storage
<br />
Gitee：https://gitee.com/dromara/x-file-storage
<br />
GitCode：https://gitcode.com/dromara/x-file-storage

-------

# 📜更新记录

这里是简要的更新记录，查看 [详细的更新记录](更新记录)

`2.3.0` 新增 Mongo GridFS、go-fastdfs、Amazon S3 V2、火山引擎 TOS 存储平台，修复资源泄露、上传等问题 [更新记录](更新记录?id=_230)
`2.2.1` 修复某些情况下哈希计算错误的问题、七牛云 Kodo 预签名 URL 无法使用的问题 [更新记录](更新记录?id=_221)
<br />
`2.2.0` 修复大量问题，新增获取文件、列举文件，重构预签名 URL 支持客户端上传、下载、删除等操作，新增 Solon 插件，优化手动分片上传等功能，详情查看 [更新记录](更新记录?id=_220)
<br />
`2.1.0` 修复大量问题，新增存储平台 FastDFS 和 Azure Blob Storage，新增复制、移动（重命名）文件，手动分片上传（断点续传）和计算哈希等功能，详情查看 [更新记录](更新记录?id=_210)
<br />
`2.0.0` 捐赠至 [dromara](https://dromara.org/zh) 开源社区，更改项目名、包名，优化项目结构、支持 Metadata 元数据等，从旧版升级需要注意，详情查看 [更新记录](更新记录?id=_200)

-------

# 📅更新计划

- 接入存储平台：HDFS、Samba、NFS
- 追加缩略图
- 文件内容预加载
- 新增 Access 模块，尝试通过 HTTP、FTP、WebDAV 等协议对外提供接口，方便其它程序使用
- 追加文件
- 分片下载
- 直接输出到 HttpServletResponse 的响应流中
- 其它更多功能

-------

# 📦使用

阅读 [快速入门](快速入门) 开始使用吧！

-------

# 💳捐赠
如果你觉得这个项目不错，可以点个 Star 或捐赠请作者吃包辣条~，不想打赏的话用支付宝扫最后一个码可以领取个红包，在此表示感谢^_^

<img src="https://x-file-storage.xuyanwu.cn/assets/wx.png" style="height: 350px;margin-right: 20px" alt="微信">
<img src="https://x-file-storage.xuyanwu.cn/assets/zfb.jpg" style="height: 350px;margin-right: 20px" alt="支付宝">
<img src="https://x-file-storage.xuyanwu.cn/assets/zfb2.jpg" style="height: 350px" alt="支付宝2">
<img src="https://x-file-storage.xuyanwu.cn/assets/elm.jpg" style="height: 350px;margin-right: 20px" alt="饿了么">
<img src="https://x-file-storage.xuyanwu.cn/assets/mt.jpeg" style="height: 350px" alt="美团外卖">

或者点击以下链接，将页面拉到最下方点击“捐赠”即可

[Gitee上捐赠](https://gitee.com/dromara/x-file-storage)

-------

# 🌶️VIP交流群

扫描上方二维码捐赠 99 元，截图发我 `QQ1171736840` 即可加入 VIP 交流群（超过一年则需要再次捐赠，否则将进行清退）

也可以点击添加免费交流群
<a target="_blank" href='https://jq.qq.com/?_wv=1027&k=eGfeNqka'>
<img src='https://img.shields.io/badge/QQ%E7%BE%A4-515706495-orange' alt='515706495' />
</a> 一起交流

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
[GitHub issue](https://github.com/dromara/x-file-storage/issues)<br/>
[GitCode issue](https://gitcode.com/dromara/x-file-storage/issues)

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
[在 GitHub 上登记](https://github.com/dromara/x-file-storage/issues/114)<br/>
[在 GitCode 上登记](https://gitcode.com/dromara/x-file-storage/issues/1)

-------

# 🌏知识星球

<img src="https://x-file-storage.xuyanwu.cn/assets/zsxq.png" height="200px" alt="知识星球">

[member-project.md](https://x-file-storage.xuyanwu.cn/assets/link/member-project.md ':include')
