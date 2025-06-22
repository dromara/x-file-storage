# 在 Solon 中使用

> Solon 官网 https://solon.noear.org/

从 `2.2.0` 版本开始原生支持在 `Solon` 中使用，之前的版本可以参考 [脱离 SpringBoot 单独使用](脱离SpringBoot单独使用)

先引入本项目, 注意这里是 `x-file-storage-solon`，而不是 `x-file-storage-core`，之后再参考 [快速入门](快速入门) 引入对应平台的依赖

```xml
<dependency>
    <groupId>org.dromara.x-file-storage</groupId>
    <artifactId>x-file-storage-solon</artifactId>
    <version>2.3.0</version>
</dependency>
```

如果使用本地存储, 请务必添加 `solon.web.staticfiles` 依赖
```xml
<dependency>
    <groupId>org.noear</groupId>
    <artifactId>solon.web.staticfiles</artifactId>
    <version>2.7.1</version><!-- 当前行可去掉, 跟随 solon 的版本管理 -->
</dependency>
```


默认情况下，Solon 会自动装配 `SolonFileStorageProperties` , 文件配置完全等同于使用 Spring 的情况, 可以在 `app.yml` 中按照 Spring 配置文件的格式进行配置。
如:
```yaml
dromara:
  x-file-storage: #文件存储配置
    default-platform: minio-user #默认使用的存储平台
    thumbnail-suffix: ".min.jpg" #缩略图后缀，例如【.min.jpg】【.png】
    enable-multipart-file-wrapper: true #是否启用多文件包装器
    local-plus:
      - platform: local-plus-1 # 存储平台标识
        enable-storage: true  #启用存储
        enable-access: true #启用访问（线上请使用 Nginx 配置，效率更高）
        domain: http://127.0.0.1:8080/file/ # 访问域名，例如：“http://127.0.0.1:8030/file/”，注意后面要和 path-patterns 保持一致，“/”结尾，本地存储建议使用相对路径，方便后期更换域名
        base-path: local-plus/ # 基础路径
        path-patterns: /file/ # 访问路径
        storage-path: D:/temp/ # 存储路径
    minio:
      - platform: minio-user # 存储平台标识
        enable-storage: true  # 启用存储
        access-key: j9rMyECcmNH0lNBqPfOo
        secret-key: 0NYFJSl4D8msuxHirenthXA4lvju4c3QNdmQ29Ob
        end-point: http://127.0.0.1:9000
        bucket-name: user
        #        domain: ?? # 访问域名，注意“/”结尾，例如：http://minio.abc.com/abc/
        base-path: user/ # 基础路径
      - platform: minio-dept # 存储平台标识
        enable-storage: true  # 启用存储
        access-key: j9rMyECcmNH0lNBqPfOo
        secret-key: 0NYFJSl4D8msuxHirenthXA4lvju4c3QNdmQ29Ob
        end-point: http://127.0.0.1:9000
        bucket-name: dept
        #        domain: ?? # 访问域名，注意“/”结尾，例如：http://minio.abc.com/abc/
        base-path: dept/ # 基础路径
```

同时, 由于 Solon 和 Spring 环境不一致, 部分功能使用会有差异, 详情参见 [Solon 与 Spring Boot 的区别](https://solon.noear.org/article/compare-springboot)

在注入 `fileStorageService` 后, 可以使用 `fileStorageService` 进行文件操作, 例如上传、下载、删除等。完全等同 Spring 的使用方式。


