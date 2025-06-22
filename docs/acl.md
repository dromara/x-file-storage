# ACL 访问控制列表

也叫预定义访问策略，目前仅 华为云 OBS、阿里云 OSS、腾讯云 COS、百度云 BOS、Amazon S3、Amazon S3 V2、GoogleCloud Storage、Azure Blob Storage、火山引擎 TOS 平台支持

Azure Blob Storage 即使将文件的 ACL 设置为 公共读`PUBLIC_READ` ，上传成功后的 `url` 也无法通过浏览器直接公开访问 ，详情阅读 [兼容性说明-AzureBlobStorage](存储平台?id=OCI_AzureBlobStorage) 章节

## 设置 ACL

**方式一（强烈推荐）：**

```java
//判断对应的存储平台是否支持 ACL
FileStorage storage = fileStorageService.getFileStorage();
boolean supportACL = fileStorageService.isSupportAcl(storage);

//在上传文件时就设置 ACL 为私有
FileInfo fileInfo = fileStorageService.of(file)
        .thumbnail()//生成缩略图
        .setAcl(Constant.ACL.PRIVATE)//同时设置文件及缩略图的 ACL
        .upload();

FileInfo fileInfo = fileStorageService.of(file)
        .thumbnail()//生成缩略图
        .setFileAcl(Constant.ACL.PRIVATE)//单独设置文件的 ACL
        .setThFileAcl(Constant.ACL.PRIVATE)//单独设置缩略图的 ACL
        .upload();

//文件上传成功后修改 ACL 为公共读
fileStorageService.setFileAcl(fileInfo,Constant.ACL.PUBLIC_READ);
fileStorageService.setThFileAcl(fileInfo,Constant.ACL.PUBLIC_READ);

```

一般情况下使用 私有`PRIVATE`、公共读`PUBLIC_READ`、公共读写`PUBLIC_READ_WRITE`这三个就够了，这是所有支持 ACL 的平台都通用的

有些平台也定义了一些私有的 ACL，请查看 `org.dromara.x.file.storage.core.constant` 包下面的 `Constant` 接口中定义的常量

因为这样设置的 ACL 都是字符串，可以方便的保存到数据库中，所以推荐使用这种方式

**方式二（不推荐）：**

要是这些事先定义的 ACL 不满足要求，也可以通过以下方式使用

<!-- tabs:start -->

#### **华为云 OBS**



```java
//第一种：使用官方 SDK 中定义好的
fileStorageService.of(file).setFileAcl(AccessControlList.REST_CANNED_PRIVATE).upload();

//第二种：自己创建的，官方参考文档：https://support.huaweicloud.com/sdk-java-devg-obs/obs_21_0802.html
AccessControlList acl = new AccessControlList();
Owner owner = new Owner();
owner.setId("ownerid");
acl.setOwner(owner);
// 保留Owner的完全控制权限（注：如果不设置该权限，该对象Owner自身将没有访问权限）
acl.grantPermission(new CanonicalGrantee("ownerid"), Permission.PERMISSION_FULL_CONTROL);
// 为指定用户设置完全控制权限
acl.grantPermission(new CanonicalGrantee("userid"), Permission.PERMISSION_FULL_CONTROL);
// 为所有用户设置读权限
acl.grantPermission(GroupGrantee.ALL_USERS, Permission.PERMISSION_READ);

fileStorageService.of(file).setFileAcl(acl).upload();
```

#### **阿里云 OSS**

```java
//使用官方 SDK 中定义好的
fileStorageService.of(file).setFileAcl(CannedAccessControlList.Private).upload();
```

#### **腾讯云 COS**

```java
//使用官方 SDK 中定义好的
fileStorageService.of(file).setFileAcl(CannedAccessControlList.Private).upload();
```

#### **百度云 BOS**

```java
//使用官方 SDK 中定义好的
fileStorageService.of(file).setFileAcl(CannedAccessControlList.Private).upload();
```

#### **Amazon S3**

```java
//使用官方 SDK 中定义好的
fileStorageService.of(file).setFileAcl(CannedAccessControlList.Private).upload();
```

#### **Amazon S3 V2**

```java
//使用官方 SDK 中定义好的
fileStorageService.of(file).setFileAcl(ObjectCannedACL.PRIVATE).upload();
//或者直接使用 Amazon S3 V2 支持的字符串
fileStorageService.of(file).setFileAcl("public-read-write").upload();
```

#### **GoogleCloud Storage**

```java
//第一种：使用官方 SDK 中定义好的
fileStorageService.of(file).setFileAcl(Storage.PredefinedAcl.PRIVATE).upload();

//第二种，使用官方SDK中的 ACL 对象，详情：https://cloud.google.com/storage/docs/access-control#About-Access-Control-Lists
Acl acl = Acl.of(new Acl.User("123@xx.com"), Acl.Role.OWNER);
fileStorageService.of(file).setFileAcl(acl).upload();

//第二种可以一次设置多个
Acl acl2 = Acl.of(new Acl.User("456@xx.com"), Acl.Role.OWNER);
Acl acl3 = Acl.of(new Acl.User("789@xx.com"), Acl.Role.READER);
fileStorageService.of(file).setFileAcl(Arrays.asList(acl2,acl3)).upload();

```

#### **Azure Blob Storage**

ACL 参考文档：https://learn.microsoft.com/zh-cn/azure/storage/blobs/data-lake-storage-access-control <br>
SDK 参考文档：https://learn.microsoft.com/zh-cn/azure/storage/blobs/data-lake-storage-acl-java#set-acls

```java
//第一种：使用官方 SDK 中的 PathPermissions 对象
PathPermissions permissions = new PathPermissions()
    .setGroup(new RolePermissions().setReadPermission(true))
    .setOwner(new RolePermissions().setReadPermission(true).setWritePermission(true));
fileStorageService.of(file).setFileAcl(permissions).upload();

//第二种，使用官方 SDK 中的 PathAccessControlEntry 对象
PathAccessControlEntry acl = PathAccessControlEntry.parse("user::rw-");
fileStorageService.of(file).setFileAcl(acl).upload();

//第二种可以一次设置多个
List<PathAccessControlEntry> acl = PathAccessControlEntry.parseList("user::rw-,group::r--,other::---");
fileStorageService.of(file).setFileAcl(acl).upload();
```

#### **火山引擎 TOS**

```java
//使用官方 SDK 中定义好的
fileStorageService.of(file).setFileAcl(ACLType.ACL_PRIVATE).upload();
```

<!-- tabs:end -->

> [!WARNING|label:重要提示：] 
> 使用这种方式的 ACL 时，因为表示 ACL 的值不是字符串，是对应平台的 SDK 中的私有对象，所以在将文件记录保存到数据库或从数据库中读取时，需要手动转换 `FileInfo`中的`fileAcl`和`thFileAcl`属性，否则会发生错误

## 读取 ACL

如果上传的文件信息有保存到数据库中，那么可以通过`FileInfo`对象直接获取

```java
FileInfo fileInfo = fileStorageService.getFileInfoByUrl("https://file.abc.com/test/a.jpg");
fileInfo.getFileAcl();
fileInfo.getThFileAcl();
```

也可以获取到对应存储平台的 Client 手动获取，这里以华为云为例，其它平台类似
```java
FileInfo fileInfo = fileStorageService.getFileInfoByUrl("https://file.abc.com/test/a.jpg");
HuaweiObsFileStorage fileStorage = fileStorageService.getFileStorage(fileInfo.getPlatform());
ObsClient client = fileStorage.getClient();
AccessControlList acl = client.getObjectAcl(fileStorage.getBucketName(),fileStorage.getFileKey(fileInfo));
```
因为这种方式使用较少，且每个平台返回的的 ACL 都不一样，所以就没有封装统一的方法，一般情况下从`FileInfo`对象直接获取就行了


## 处理异常

默认在不支持的存储平台传入 ACL 会抛出异常，可以通过以下方式不抛出异常

**第一种（全局）**
```yaml
dromara:
  x-file-storage:
    upload-not-support-alc-throw-exception: false # 上传时
    copy-not-support-alc-throw-exception: false # 复制时
    move-not-support-alc-throw-exception: false # 移动时
```

**第二种（仅当前）**
```java
//上传时
FileInfo fileInfo = fileStorageService.of(file)
        .setNotSupportAclThrowException(false) //在不支持 ACL 的存储平台不抛出异常
        .setAcl(Constant.ACL.PRIVATE)
        .upload();

//复制时
FileInfo fileInfo = fileStorageService.copy(fileInfo)
        .setNotSupportAclThrowException(false) //在不支持 ACL 的存储平台不抛出异常
        .setPlatform("local-plus-1")
        .copy();

//移动时
FileInfo fileInfo = fileStorageService.move(fileInfo)
        .setNotSupportAclThrowException(false) //在不支持 ACL 的存储平台不抛出异常
        .setPlatform("local-plus-1")
        .move();
```
