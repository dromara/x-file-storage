# 脱离 SpringBoot 单独使用

从 `0.8.0` 版本开始支持脱离 SpringBoot 单独使用

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
