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
    <a target="_blank" href='https://jq.qq.com/?_wv=1027&k=eGfeNqka'>
        <img src='https://img.shields.io/badge/QQ%E7%BE%A4-515706495-orange' alt='515706495' />
    </a>
</p>

[tg.md](https://x-file-storage.xuyanwu.cn/assets/tg/tg.md ':include')

-------

# 📚简介

一行代码将文件存储到本地、FTP、SFTP、WebDAV、阿里云 OSS、华为云 OBS、七牛云 Kodo、腾讯云 COS、百度云 BOS、又拍云 USS、MinIO、
Amazon S3、GoogleCloud Storage、FastDFS、 Azure Blob Storage、Cloudflare R2、金山云 KS3、美团云 MSS、京东云 OSS、天翼云 OOS、移动 云EOS、沃云 OSS、
网易数帆 NOS、Ucloud US3、青云 QingStor、平安云 OBS、首云 OSS、IBM COS、其它兼容 S3 协议的存储平台。查看 [所有支持的存储平台](存储平台)

💡 通过 WebDAV 连接到 Alist 后，可以使用百度网盘、天翼云盘、阿里云盘、迅雷网盘等常见存储服务，查看 [Alist 支持的存储平台](https://alist-doc.nn.ci/docs/webdav)

GitHub：https://github.com/dromara/x-file-storage
<br />
Gitee：https://gitee.com/dromara/x-file-storage

-------

# 📜更新记录

这里是简要的更新记录，查看 [详细的更新记录](更新记录)

`2.0.0` 修复了大量问题，新增存储平台 FastDFS 和 Azure Blob Storage，新增复制、移动（重命名）文件，大文件手动分片上传（断点续传）和计算哈希等功能，详情查看 [更新记录](更新记录?id=_210)
<br />
`2.0.0` 捐赠至 [dromara](https://dromara.org/zh) 开源社区，更改项目名、包名，优化项目结构、支持 Metadata 元数据等，从旧版升级需要注意，详情查看 [更新记录](更新记录?id=_200)
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
- 大文件手动分片上传（1.0.0 已支持大文件自动分片上传）
- 复制或移动文件
- 文件内容预加载
- 新增 Access 模块，尝试通过 HTTP、FTP、WebDAV 等协议对外提供接口，方便其它程序使用

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

# 🚀Dromara成员项目

<style>
    .member-project {
        display: flex;
        flex-wrap: wrap;
    }

    .member-project a {
        padding: 10px;
    }

    .member-project a img {
        height: 40px;
    }
</style>
<div class="member-project">
    <a href="https://gitee.com/dromara/TLog" target="_blank">
        <img src="https://x-file-storage.xuyanwu.cn/assets/link/tlog.png" alt="TLog" title="一个轻量级的分布式日志标记追踪神器，10分钟即可接入，自动对日志打标签完成微服务的链路追踪">
    </a>
    <a href="https://gitee.com/dromara/liteFlow" target="_blank">
        <img src="https://x-file-storage.xuyanwu.cn/assets/link/liteflow.png" alt="liteFlow" title="轻量，快速，稳定，可编排的组件式流程引擎">
    </a>
    <a href="https://hutool.cn/" target="_blank">
        <img src="https://x-file-storage.xuyanwu.cn/assets/link/hutool.jpg" alt="hutool" title="小而全的Java工具类库，使Java拥有函数式语言般的优雅，让Java语言也可以“甜甜的”。">
    </a>
    <a href="https://sa-token.cc/" target="_blank">
        <img src="https://x-file-storage.xuyanwu.cn/assets/link/sa-token.png" alt="sa-token" title="一个轻量级 java 权限认证框架，让鉴权变得简单、优雅！">
    </a>
    <a href="https://gitee.com/dromara/hmily" target="_blank">
        <img src="https://x-file-storage.xuyanwu.cn/assets/link/hmily.png" alt="hmily" title="高性能一站式分布式事务解决方案。">
    </a>
    <a href="https://gitee.com/dromara/Raincat" target="_blank">
        <img src="https://x-file-storage.xuyanwu.cn/assets/link/raincat.png" alt="Raincat" title="强一致性分布式事务解决方案。">
    </a>
    <a href="https://gitee.com/dromara/myth" target="_blank">
        <img src="https://x-file-storage.xuyanwu.cn/assets/link/myth.png" alt="myth" title="可靠消息分布式事务解决方案。">
    </a>
    <a href="https://cubic.jiagoujishu.com/" target="_blank">
        <img src="https://x-file-storage.xuyanwu.cn/assets/link/cubic.png" alt="cubic" title="一站式问题定位平台，以agent的方式无侵入接入应用，完整集成arthas功能模块，致力于应用级监控，帮助开发人员快速定位问题">
    </a>
    <a href="https://maxkey.top/" target="_blank">
        <img src="https://x-file-storage.xuyanwu.cn/assets/link/maxkey.png" alt="maxkey" title="业界领先的身份管理和认证产品">
    </a>
    <a href="http://forest.dtflyx.com/" target="_blank">
        <img src="https://x-file-storage.xuyanwu.cn/assets/link/forest-logo.png" alt="forest" title="Forest能够帮助您使用更简单的方式编写Java的HTTP客户端">
    </a>
    <a href="https://jpom.top/" target="_blank">
        <img src="https://x-file-storage.xuyanwu.cn/assets/link/jpom.png" alt="jpom" title="一款简而轻的低侵入式在线构建、自动部署、日常运维、项目监控软件">
    </a>
    <a href="https://su.usthe.com/" target="_blank">
        <img src="https://x-file-storage.xuyanwu.cn/assets/link/sureness.png" alt="sureness" title="面向 REST API 的高性能认证鉴权框架">
    </a>
    <a href="https://easy-es.cn/" target="_blank">
        <img src="https://x-file-storage.xuyanwu.cn/assets/link/easy-es2.png" alt="easy-es" title="傻瓜级ElasticSearch搜索引擎ORM框架">
    </a>
    <a href="https://gitee.com/dromara/northstar" target="_blank">
        <img src="https://x-file-storage.xuyanwu.cn/assets/link/northstar_logo.png" alt="northstar" title="Northstar盈富量化交易平台">
    </a>
    <a href="https://hertzbeat.com/" target="_blank">
        <img src="https://x-file-storage.xuyanwu.cn/assets/link/hertzbeat-brand.svg" alt="hertzbeat" title="易用友好的云监控系统">
    </a>
    <a href="https://dromara.gitee.io/fast-request/" target="_blank">
        <img src="https://x-file-storage.xuyanwu.cn/assets/link/fast-request.gif" alt="fast-request" title="Idea 版 Postman，为简化调试API而生">
    </a>
    <a href="https://www.jeesuite.com/" target="_blank">
        <img src="https://x-file-storage.xuyanwu.cn/assets/link/mendmix.png" alt="mendmix" title="开源分布式云原生架构一站式解决方案">
    </a>
    <a href="https://gitee.com/dromara/koalas-rpc" target="_blank">
        <img src="https://x-file-storage.xuyanwu.cn/assets/link/koalas-rpc2.png" alt="koalas-rpc" title="企业生产级百亿日PV高可用可拓展的RPC框架。">
    </a>
    <a href="https://async.sizegang.cn/" target="_blank">
        <img src="https://x-file-storage.xuyanwu.cn/assets/link/gobrs-async.png" alt="gobrs-async" title="配置极简功能强大的异步任务动态编排框架">
    </a>
    <a href="https://dynamictp.cn/" target="_blank">
        <img src="https://x-file-storage.xuyanwu.cn/assets/link/dynamic-tp.png" alt="dynamic-tp" title="基于配置中心的轻量级动态可监控线程池">
    </a>
    <a href="https://www.x-easypdf.cn" target="_blank">
        <img src="https://x-file-storage.xuyanwu.cn/assets/link/x-easypdf.png" alt="x-easypdf" title="一个用搭积木的方式构建pdf的框架（基于pdfbox）">
    </a>
    <a href="http://dromara.gitee.io/image-combiner" target="_blank">
        <img src="https://x-file-storage.xuyanwu.cn/assets/link/image-combiner.png" alt="image-combiner" title="一个专门用于图片合成的工具，没有很复杂的功能，简单实用，却不失强大">
    </a>
    <a href="https://www.herodotus.cn/" target="_blank">
        <img src="https://x-file-storage.xuyanwu.cn/assets/link/dante-cloud2.png" alt="dante-cloud" title="Dante-Cloud 是一款企业级微服务架构和服务能力开发平台。">
    </a>
    <a href="http://www.mtruning.club" target="_blank">
        <img src="https://x-file-storage.xuyanwu.cn/assets/link/go-view.png" alt="go-view" title="低代码数据可视化开发平台">
    </a>
    <a href="https://tangyh.top/" target="_blank">
        <img src="https://x-file-storage.xuyanwu.cn/assets/link/lamp-cloud.png" alt="lamp-cloud" title="微服务中后台快速开发平台，支持租户(SaaS)模式、非租户模式">
    </a>
    <a href="https://www.redisfront.com/" target="_blank">
        <img src="https://x-file-storage.xuyanwu.cn/assets/link/redis-front.png" alt="redis-front" title="RedisFront 是一款开源免费的跨平台 Redis 桌面客户端工具, 支持单机模式, 集群模式, 哨兵模式以及 SSH 隧道连接, 可轻松管理Redis缓存数据.">
    </a>
    <a href="https://www.yuque.com/u34495/mivcfg" target="_blank">
        <img src="https://x-file-storage.xuyanwu.cn/assets/link/electron-egg.png" alt="electron-egg" title="一个入门简单、跨平台、企业级桌面软件开发框架">
    </a>
    <a href="https://gitee.com/dromara/open-capacity-platform" target="_blank">
        <img src="https://x-file-storage.xuyanwu.cn/assets/link/open-capacity-platform.jpg" alt="open-capacity-platform" title="简称ocp是基于Spring Cloud的企业级微服务框架(用户权限管理，配置中心管理，应用管理，....)">
    </a>
    <a href="http://easy-trans.fhs-opensource.top/" target="_blank">
        <img src="https://x-file-storage.xuyanwu.cn/assets/link/easy_trans.png" alt="Easy-Trans" title="Easy-Trans 一个注解搞定数据翻译,减少30%SQL代码量">
    </a>
    <a href="https://gitee.com/dromara/neutrino-proxy" target="_blank">
        <img src="https://x-file-storage.xuyanwu.cn/assets/link/neutrino-proxy.svg" alt="neutrino-proxy" title="一款基于 Netty 的、开源的内网穿透神器。">
    </a>
    <a href="https://chatgpt.cn.obiscr.com/" target="_blank">
        <img src="https://x-file-storage.xuyanwu.cn/assets/link/chatgpt.png" alt="chatgpt" title="一个支持在 JetBrains 系列 IDE 上运行的 ChatGPT 的插件。">
    </a>
    <a href="https://gitee.com/dromara/zyplayer-doc" target="_blank">
        <img src="https://x-file-storage.xuyanwu.cn/assets/link/zyplayer-doc.png" alt="zyplayer-doc" title="zyplayer-doc是一款适合团队和个人使用的WIKI文档管理工具，同时还包含数据库文档、Api接口文档。">
    </a>
    <a href="https://gitee.com/dromara/payment-spring-boot" target="_blank">
        <img src="https://x-file-storage.xuyanwu.cn/assets/link/payment-spring-boot.png" alt="payment-spring-boot" title="最全最好用的微信支付V3 Spring Boot 组件。">
    </a>
    <a href="https://www.j2eefast.com/" target="_blank">
        <img src="https://x-file-storage.xuyanwu.cn/assets/link/j2eefast.png" alt="j2eefast" title="J2eeFAST 是一个致力于中小企业 Java EE 企业级快速开发平台,我们永久开源!">
    </a>
    <a href="https://gitee.com/dromara/data-compare" target="_blank">
        <img src="https://x-file-storage.xuyanwu.cn/assets/link/dataCompare.png" alt="data-compare" title="数据库比对工具：hive 表数据比对，mysql、Doris 数据比对，实现自动化配置进行数据比对，避免频繁写sql 进行处理，低代码(Low-Code) 平台">
    </a>
    <a href="https://gitee.com/dromara/open-giteye-api" target="_blank">
        <img src="https://x-file-storage.xuyanwu.cn/assets/link/open-giteye-api.svg" alt="open-giteye-api" title="giteye.net 是专为开源作者设计的数据图表服务工具类站点，提供了包括 Star 趋势图、贡献者列表、Gitee指数等数据图表服务。">
    </a>
    <a href="https://gitee.com/dromara/RuoYi-Vue-Plus" target="_blank">
        <img src="https://x-file-storage.xuyanwu.cn/assets/link/RuoYi-Vue-Plus.png" alt="RuoYi-Vue-Plus" title="后台管理系统 重写 RuoYi-Vue 所有功能 集成 Sa-Token + Mybatis-Plus + Jackson + Xxl-Job + SpringDoc + Hutool + OSS 定期同步">
    </a>
    <a href="https://gitee.com/dromara/RuoYi-Cloud-Plus" target="_blank">
        <img src="https://x-file-storage.xuyanwu.cn/assets/link/RuoYi-Cloud-Plus.png" alt="RuoYi-Cloud-Plus" title="微服务管理系统 重写RuoYi-Cloud所有功能 整合 SpringCloudAlibaba Dubbo3.0 Sa-Token Mybatis-Plus MQ OSS ES Xxl-Job Docker 全方位升级 定期同步">
    </a>
    <a href="https://gitee.com/dromara/stream-query" target="_blank">
        <img src="https://x-file-storage.xuyanwu.cn/assets/link/stream-query.png" alt="stream-query" title="允许完全摆脱 Mapper 的 mybatis-plus 体验！封装 stream 和 lambda 操作进行数据返回处理。">
    </a>
    <a href="https://wind.kim/" target="_blank">
        <img src="https://x-file-storage.xuyanwu.cn/assets/link/sms4j.png" alt="sms4j" title="短信聚合工具，让发送短信变的更简单。">
    </a>
    <a href="https://cloudeon.top/" target="_blank">
        <img src="https://x-file-storage.xuyanwu.cn/assets/link/cloudeon.png" alt="cloudeon" title="简化kubernetes上大数据集群的运维管理">
    </a>
    <a href="https://github.com/dromara/hodor" target="_blank">
        <img src="https://x-file-storage.xuyanwu.cn/assets/link/hodor.png" alt="hodor" title="Hodor是一个专注于任务编排和高可用性的分布式任务调度系统。">
    </a>
    <a href="http://nsrule.com/" target="_blank">
        <img src="https://x-file-storage.xuyanwu.cn/assets/link/test-hub.png" alt="test-hub" title="流程编排，插件驱动，测试无限可能">
    </a>
    <a href="https://gitee.com/dromara/disjob" target="_blank">
        <img src="https://x-file-storage.xuyanwu.cn/assets/link/disjob-2.png" alt="disjob" title="Disjob是一个分布式的任务调度框架">
    </a>
    <a href="https://gitee.com/dromara/binlog4j" target="_blank">
        <img src="https://x-file-storage.xuyanwu.cn/assets/link/Binlog4j.png" alt="binlog4j" title="轻量级 Mysql Binlog 客户端, 提供宕机续读, 高可用集群等特性">
    </a>
    <a href="https://gitee.com/dromara/yft-design" target="_blank">
        <img src="https://x-file-storage.xuyanwu.cn/assets/link/yft-design.png" alt="yft-design" title="基于 Canvas 的开源版 创客贴 支持导出json，svg, image文件。">
    </a>
    <a href="https://x-file-storage.xuyanwu.cn" target="_blank">
        <img src="https://x-file-storage.xuyanwu.cn/assets/logo3.svg" alt="x-file-storage" title="">
    </a>
    <a href="https://wemq.nicholasld.cn" target="_blank">
        <img src="https://x-file-storage.xuyanwu.cn/assets/link/WeMQ.png" alt="WeMQAQ" title="开源、高性能、安全、功能强大的物联网调试和管理解决方案。">
    </a>
    <a href="https://www.yuque.com/may-fly/mayfly-go" target="_blank">
        <img src="https://x-file-storage.xuyanwu.cn/assets/link/mayfly-go.png" alt="Mayfly-Go" title="web版 linux、数据库、redis、mongo统一管理操作平台。">
    </a>
    <a href="https://akali.yomahub.com" target="_blank">
        <img src="https://x-file-storage.xuyanwu.cn/assets/link/Akali.png" alt="Akali" title="Akali(阿卡丽)，轻量级本地化热点检测/降级框架，10秒钟即可接入使用！大流量下的神器">
    </a>
    <a href="https://dromara.org/zh/projects/" target="_blank">
        <img src="https://x-file-storage.xuyanwu.cn/assets/link/dromara.png" alt="dromara" title="让每一位开源爱好者，体会到开源的快乐。">
    </a>
</div>

为往圣继绝学，一个人或许能走的更快，但一群人会走的更远。

<a href="https://x-file-storage.xuyanwu.cn/assets/logo-all.zip" target="_blank">下载本项目 logo 合集</a>

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

