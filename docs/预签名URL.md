# 预签名 URL

有时文件是存储在私有桶里的，或者文件的 ACL 是私有的，无法直接访问，这时候我们就可以通过生成一个预签名 URL 来达到临时访问的目的



## 生成

目前仅 华为云 OBS、阿里云 OSS、七牛云 Kodo、腾讯云 COS、百度云 BOS、MinIO、Amazon S3、GoogleCloud Storage 平台支持

```java
//判断对应的存储平台是否支持预签名 URL
FileStorage storage = fileStorageService.getFileStorage();
boolean supportPresignedUrl = fileStorageService.isSupportPresignedUrl(storage);

//生成 URL ，有效期为1小时
String presignedUrl = fileStorageService.generatePresignedUrl(fileInfo,DateUtil.offsetHour(new Date(),1));
System.out.println("文件授权访问地址：" + presignedUrl);

String thPresignedUrl = fileStorageService.generateThPresignedUrl(fileInfo,DateUtil.offsetHour(new Date(),1));
System.out.println("缩略图文件授权访问地址：" + thPresignedUrl);
```
