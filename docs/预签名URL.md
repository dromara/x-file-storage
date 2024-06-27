# 预签名 URL

有时文件是存储在私有桶里的，或者文件的 ACL 是私有的，无法直接访问，这时候我们就可以通过生成一个预签名 URL 来达到临时访问的目的



## 生成

目前仅 华为云 OBS、阿里云 OSS、七牛云 Kodo、腾讯云 COS、百度云 BOS、MinIO、Amazon S3、GoogleCloud Storage、Azure Blob Storage 平台支持

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

华为云，支持所有操作：[文档](https://support.huaweicloud.com/sdk-java-devg-obs/obs_21_0901.html)

阿里云，支持 GET PUT 操作：[文档](https://help.aliyun.com/zh/oss/developer-reference/authorize-access-1?spm=a2c4g.11186623.0.0.21ec3b2bHHPzJn#section-8ii-3zg-2ib)

Amazon S3 https://docs.aws.amazon.com/zh_cn/AmazonS3/latest/userguide/using-presigned-url.html

腾讯云 https://cloud.tencent.com/document/product/436/35217

MinIO https://min.io/docs/minio/linux/developers/java/API.html#getPresignedObjectUrl

百度云 https://cloud.baidu.com/doc/BOS/s/Wl60p2b61

Azure Blob Storage  https://learn.microsoft.com/zh-cn/azure/storage/blobs/sas-service-create-java

七牛云 Kodo 支持 GET 操作：[文档](https://help.aliyun.com/zh/oss/developer-reference/authorize-access-1?spm=a2c4g.11186623.0.0.21ec3b2bHHPzJn#section-8ii-3zg-2ib)，更多功能可以通过 AWS S3 的 SDK 来实现：[兼容性说明](https://developer.qiniu.com/kodo/4086/aws-s3-compatible)
