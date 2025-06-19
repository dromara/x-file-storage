# 预签名 URL

有时文件是存储在私有桶里的，或者文件的 ACL 是私有的，无法直接访问，这时候我们就可以通过生成一个预签名 URL 来达到临时访问的目的

又或者想实现客户端上传等，也可以通过此功能来实现

目前仅 华为云 OBS、阿里云 OSS、七牛云 Kodo、腾讯云 COS、百度云 BOS、MinIO、Amazon S3、Amazon S3 V2、GoogleCloud Storage、Azure Blob Storage、火山引擎 TOS 平台支持

## 基本用法

基本的下载用法，`2.2.0` 之前的版本仅支持这种用法，仅能生成用于访问或下载的 URL

```java
//判断对应的存储平台是否支持预签名 URL
FileStorage storage = fileStorageService.getFileStorage();
boolean supportPresignedUrl = fileStorageService.isSupportPresignedUrl(storage);

//快速生成用于访问或下载的 URL ，有效期为1小时
String presignedUrl = fileStorageService.generatePresignedUrl(fileInfo, DateUtil.offsetHour(new Date(), 1));
System.out.

println("文件授权访问地址："+presignedUrl);

String thPresignedUrl = fileStorageService.generateThPresignedUrl(fileInfo, DateUtil.offsetHour(new Date(), 1));
System.out.

println("缩略图文件授权访问地址："+thPresignedUrl);
```

## 高级用法

`2.2.0` 开始支持更多高级用法，主要通过 `Method` 控制，例如上传是 `PUT` ，下载是 `GET` ，删除是 `DELETE`，详情可以参考以下链接

| 存储平台                | 说明                                                                                                                 | 参考链接                                                                                                                                  |
|---------------------|--------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------|
| 华为云 OBS             | 支持 GET、POST、PUT、DELETE、HEAD                                                                                        | [查看](https://support.huaweicloud.com/sdk-java-devg-obs/obs_21_0901.html)                                                              |
| 阿里云 OSS             | 支持 GET、PUT                                                                                                         | [查看](https://help.aliyun.com/zh/oss/developer-reference/authorize-access-1?spm=a2c4g.11186623.0.0.21ec3b2bHHPzJn#section-8ii-3zg-2ib) |
| 七牛云 Kodo            | 官方 SDK 仅支持 GET ，可以通过 Amazon S3 的 SDK 来使用七牛云 Kodo ：[兼容性说明](https://developer.qiniu.com/kodo/4086/aws-s3-compatible) | [查看](https://help.aliyun.com/zh/oss/developer-reference/authorize-access-1?spm=a2c4g.11186623.0.0.21ec3b2bHHPzJn#section-8ii-3zg-2ib) |
| 腾讯云 COS             | 支持 GET、POST、PUT、DELETE、HEAD                                                                                        | [查看](https://cloud.tencent.com/document/product/436/35217)                                                                            |
| 百度云 BOS             | 支持 GET、PUT、DELETE、HEAD                                                                                             | [查看](https://cloud.baidu.com/doc/BOS/s/Wl60p2b61)                                                                                     |
| MinIO               | 支持 GET、POST、PUT、DELETE、HEAD                                                                                        | [查看](https://min.io/docs/minio/linux/developers/java/API.html#getPresignedObjectUrl)                                                  |
| Amazon S3           | 支持 GET、POST、PUT、DELETE、HEAD                                                                                        | [查看](https://docs.aws.amazon.com/zh_cn/AmazonS3/latest/userguide/using-presigned-url.html)                                            |
| Amazon S3 V2        | 支持 GET、PUT、DELETE、CreateMultipartUpload、CompleteMultipartUpload、AbortMultipartUpload、UploadPart                    | [查看](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/examples-s3-presign.html)                                        |
| GoogleCloud Storage | 支持 GET、POST、PUT、DELETE、HEAD                                                                                        | [查看](https://cloud.google.com/storage/docs/access-control/signed-urls?hl=zh-cn)                                                       |
| Azure Blob Storage  | 默认支持 GET、PUT、DELETE  ，可在配置文件中通过 methodToPermissionMap 参数自行扩展                                                       | [查看](https://learn.microsoft.com/zh-cn/azure/storage/blobs/sas-service-create-java)                                                   |
| 火山引擎 TOS            | 支持 GET、PUT、DELETE、HEAD                                                                                             | [查看](https://www.volcengine.com/docs/6349/74839)                                                                                      |

> [!TIP|label:说明：]
> 在使用过程中，由于每个存储平台支持情况各不相同，可能会出现某些功能不生效的情况，可自行查询相关文档

### 下载

```java
// 获取一个 FileInfo
FileInfo fileInfo = fileStorageService.getFileInfoByUrl("https://aa.bb.com/aa/bb/cc.jpg");

// 生成下载或访问用的 URL
GeneratePresignedUrlResult downloadResult = fileStorageService
        .generatePresignedUrl()
        .setPlatform(fileInfo.getPlatform()) // 存储平台，不传使用默认的
        .setPath(fileInfo.getPath()) // 文件路径
        .setFilename(fileInfo.getFilename()) // 文件名，也可以换成缩略图的文件名
        .setMethod(Constant.GeneratePresignedUrl.Method.GET) // 签名方法
        .setExpiration(DateUtil.offsetMinute(new Date(), 10)) // 过期时间 10 分钟
        .putResponseHeaders(
                // 设置一个响应头，将下载时的文件名改成 NewDownloadFileName.jpg，不需要可省略
                // 这里也可以设置其它的想要的响应头，每个存储平台支持情况都不太相同，可以自行测试或查询相关文档
                Constant.Metadata.CONTENT_DISPOSITION, "attachment;filename=NewDownloadFileName.jpg")
        .generatePresignedUrl();

info("生成访问预签名 URL 结果：{}",downloadResult);

//根据获得的 URL 下载文件
byte[] downloadBytes = HttpUtil.downloadBytes(downloadResult.getUrl());
```

### 上传

生成上传 URL 给客户端，从而实现客户端直传文件，不经过后台，目前这种方式不支持附带缩略图，将在后续版本提供追加缩略图功能，也不支持设置 `ACL`，可以上传后通过后台设置 `ACL`

**第一步：**

服务端根据需求生成上传 URL 及 Headers

```java
GeneratePresignedUrlResult uploadResult = fileStorageService
                .generatePresignedUrl()
                .setPlatform("google-cloud-1") // 存储平台，不传使用默认的
                .setPath("test/") // 设置路径
                .setFilename("image.jpg") // 设置保存的文件名
                .setMethod(Constant.GeneratePresignedUrl.Method.PUT)    // 签名方法
                .setExpiration(DateUtil.offsetMinute(new Date(), 10))   // 设置过期时间 10 分钟
                .putHeaders(Constant.Metadata.CONTENT_TYPE, "image/jpeg") // 设置要上传文件 MIME 类型
                .putHeaders(Constant.Metadata.CONTENT_DISPOSITION, "attachment;filename=DownloadFileName.jpg") //设置其它元数据，不需要可省略
                .putUserMetadata("role", "666") //设置自定义用户元数据，不需要可省略
                .putQueryParams("admin", "123456") //设置自定义查询参数，不需要可省略
                .generatePresignedUrl();
Assert.notNull(uploadResult, "生成上传预签名 URL 失败！");
log.info("生成上传预签名 URL 结果：{}", uploadResult);
```

**第二步：**

客户端根据得到的 URL 和 Headers 进行上传。注意这里使用 REST 风格上传，body 直接就是文件内容，不是传统的 FormData 形式

Java 写法

```java
byte[] bytes = FileUtil.readBytes("C:\\001.jpg");
String body = HttpRequest.of(uploadResult.getUrl())
        .method(Method.PUT)
        .addHeaders(uploadResult.getHeaders())  //需要添加 Headers ，这一步必不可少
        .body(bytes) //文件内容的 byte 数据
        .execute()
        .body();

//根据返回值判断是否上传成功
Assert.isTrue(StrUtil.isBlank(body), "生成上传预签名 URL 失败：{}", body);
```

JS 写法

```js
let input = document.createElement('input')
input.type = 'file'
input.accept = '.jpg'
input.oninput = async _ => {
    console.log(input.files)
    let file = input.files[0]

    //使用 fetch 上传
    let response = await fetch(uploadResult.url, {
        method: 'PUT',
        headers: uploadResult.headers,
        body: file,
        onProgress: (progress) => {
            console.log(`上传进度: ${Math.round((progress.loaded / progress.total) * 100)}%`);
        }
    });

    //验证是否上传成功
    if (response.ok) {
        console.log('File uploaded successfully!');
    } else {
        console.error('Error uploading file:', response.status);
    }
}
input.click()
```

> [!WARNING|label:重要提示：]
> 使用 JS 在浏览器中进行上传文件等操作时，可能会遇到跨域问题，请到对应存储平台的控制台操作 CORS 相关配置<br />
> GoogleCloud Storage 不支持在控制台中设置，可以使用以下方式通过代码设置，其它存储平台自行查询相关文档

```java
// 每个存储通只需要设置一次即可，参考文档：https://cloud.google.com/storage/docs/cross-origin?hl=zh-cn
GoogleCloudStorageFileStorage storage = fileStorageService.getFileStorage("google-cloud-1"); //获取对应的存贮平台
Storage client = storage.getClient();
Bucket bucket = client.get(storage.getBucketName());
String origin = "*"; // 域名，例如 www.baidu.com  或者 *.baidu.com 允许全部直接设置成 *
HttpMethod method = HttpMethod.PUT; // 请求方法，上传文件是 PUT 下载文件是 GET
String responseHeader = "*"; // 响应头
Integer maxAgeSeconds = 3600; // 时间
Cors cors = Cors.newBuilder()
        .setOrigins(ImmutableList.of(Cors.Origin.of(origin)))
        .setMethods(ImmutableList.of(method))
        .setResponseHeaders(ImmutableList.of(responseHeader))
        .setMaxAgeSeconds(maxAgeSeconds)
        .build();
bucket.toBuilder().setCors(ImmutableList.of(cors)).build().update();
```

**第三步：**

参考 [获取文件](基础功能?id=获取文件)  查询已上传的文件，这部分可省略

```java
//自行传入 path 及 filename 获取文件信息
RemoteFileInfo info3 = fileStorageService.getFile().setPath("test/").setFilename("image.jpg").getFile();
Assert.notNull(info3, "文件不存在");
log.info("获取文件结果：{}", info3);
//文件元数据
MapProxy metadata = info3.getKebabCaseInsensitiveMetadata();
//文件用户元数据
MapProxy userMetadata = info3.getKebabCaseInsensitiveUserMetadata();

//转换成 FileInfo 可方便进行其它操作或保存到数据库中
FileInfo fileInfo = info3.toFileInfo();
```

### 删除

```java
GeneratePresignedUrlResult deleteResult = fileStorageService
        .generatePresignedUrl()
        .setPath("test/")
        .setFilename("image.jpg")
        .setMethod(Constant.GeneratePresignedUrl.Method.DELETE)
        .setExpiration(DateUtil.offsetMinute(new Date(), 10))
        // Hutool HTTP 工具默认发送的 Content-Type 是 application/x-www-form-urlencoded
        // 会导致签名错误，这里强制传入一个 Content-Type 用于覆盖默认的可以解决这个问题
        // 其它请求工具类可以忽略这个参数
        .putHeaders(Constant.Metadata.CONTENT_TYPE, "image/jpeg")
        .generatePresignedUrl();

// 使用 Hutool HTTP 工具访问 URL 实现删除文件，其它语言也类似，浏览器中也会有跨域问题，可以参考签名的上传章节
String deleteHttpResponse = HttpRequest.of(deleteResult.getUrl())
        .method(Method.DELETE)
        .addHeaders(deleteResult.getHeaders())
        .execute()
        .body();
```
