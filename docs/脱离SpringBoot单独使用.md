# 脱离 SpringBoot 单独使用

从 `1.0.0` 版本开始支持脱离 `SpringBoot` 单独使用

先引入本项目，注意这里是 `file-storage-core`，之后再参考 [快速入门](快速入门) 引入对应平台的依赖

```xml
<dependency>
    <groupId>cn.xuyanwu</groupId>
    <artifactId>file-storage-core</artifactId>
    <version>1.0.1</version>
</dependency>
```

最后手动初始化即可

```java
//配置文件定义存储平台
FileStorageProperties properties = new FileStorageProperties();
properties.setDefaultPlatform("ftp-1");
FtpConfig ftp = new FtpConfig();
ftp.setPlatform("ftp-1");
ftp.setHost("192.168.3.100");
ftp.setPort(2121);
ftp.setUser("root");
ftp.setPassword("123456");
ftp.setDomain("ftp://192.168.3.100:2121/");
ftp.setBasePath("ftp/");
ftp.setStoragePath("/");
properties.setFtp(Collections.singletonList(ftp));

//创建，自定义存储平台、 Client 工厂、切面等功能都有对应的添加方法
FileStorageService service = FileStorageServiceBuilder.create(properties).useDefault().build();

//初始化完毕，开始上传吧
FileInfo fileInfo = service.of(new File("D:\\Desktop\\a.png")).upload();
System.out.println(fileInfo);
```
