# Metadata 元数据

## 使用

可以在上传时传入 Metadata 和 UserMetadata ，目前仅 华为云 OBS、阿里云 OSS、腾讯云 COS、百度云 BOS、七牛云 Kodo、又拍云 USS、MinIO、Amazon S3、GoogleCloud Storage 平台支持

```java
//判断是否支持 Metadata
FileStorage storage = fileStorageService.getFileStorage();
boolean supportMetadata = fileStorageService.isSupportMetadata(storage);

//上传并传入 Metadata
FileInfo fileInfo = fileStorageService.of(file)
        .putMetadata(Constant.Metadata.CONTENT_DISPOSITION,"attachment;filename=DownloadFileName.jpg")
        .putMetadata("Test-Not-Support","123456")//测试不支持的元数据，此数据并不会生效
        .putUserMetadata("role","666")
        .putThMetadata(Constant.Metadata.CONTENT_DISPOSITION,"attachment;filename=DownloadThFileName.jpg")
        .putThUserMetadata("role","777")
        .thumbnail()
        .upload();
```

> [!WARNING|label:重要提示：]
> 每个存储平台支持的 Metadata 有所不同，例如 七牛云 Kodo 和 又拍云 USS 就不支持 `Content-Disposition`，具体支持情况以每个存储平台的官方文档为准
>
> 在传入 UserMetadata 时，不用传入前缀，例如 `x-amz-meta-` `x-qn-meta-` `x-upyun-meta-`，SDK会自动处理


## 处理异常

默认在不支持的存储平台传入 UserMetadata 会抛出异常，可以通过以下方式不抛出异常

**第一种（全局）**
```yaml
dromara:
  x-file-storage:
    upload-not-support-metadata-throw-exception: false # 上传时
    copy-not-support-metadata-throw-exception: false # 复制时
    move-not-support-metadata-throw-exception: false # 移动时
```

**第二种（仅当前）**
```java
//上传时
FileInfo fileInfo = fileStorageService.of(file)
        .setNotSupportMetadataThrowException(false) //在不支持 Metadata 的存储平台不抛出异常
        .putUserMetadata("role","666")
        .upload();

//复制时
FileInfo fileInfo = fileStorageService.copy(fileInfo)
        .setNotSupportMetadataThrowException(false) //在不支持 Metadata 的存储平台不抛出异常
        .setPlatform("local-plus-1")
        .copy();

//移动时
FileInfo fileInfo = fileStorageService.move(fileInfo)
        .setNotSupportMetadataThrowException(false) //在不支持 Metadata 的存储平台不抛出异常
        .setPlatform("local-plus-1")
        .move();
```


