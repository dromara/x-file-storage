# 计算哈希

可以在上传、下载的同时计算文件的哈希值，例如 `MD5` `SHA256` 等，更多哈希以实际API为准

### 直接上传文件时

```java
FileInfo fileInfo = fileStorageService.of(file)
        .setHashCalculatorMd5() //计算 MD5
        .setHashCalculatorSha256() //计算 SHA256
        .setHashCalculator(Constant.Hash.MessageDigest.MD2) //指定哈希名称，这里定义了一些常用的哈希名称
        .setHashCalculator("SHA-512")   //指定哈希名称，内部是通过 MessageDigest 来计算哈希值的，只要是 MessageDigest 支持的名称就都可以
        .setHashCalculator(MessageDigest.getInstance("SHA-384"))    //指定 MessageDigest
        .upload();

//上传成功后即可这样获取到对应的哈希值
HashInfo hashInfo = fileInfo.getHashInfo();
String md5 = hashInfo.getMd5();
String sha256 = hashInfo.getSha256();
```

### 手动分片上传-上传分片时

注意，在上传到本地、又拍云 USS 等存储平台时，会自动调用 setHashCalculatorMd5() 方法计算 MD5 作为分片的 etag 值，其它存储平台不会

```java
FilePartInfo filePartInfo = fileStorageService.uploadPart(fileInfo, partNumber, bytes, (long) bytes.length)
        .setHashCalculatorMd5() //计算 MD5
        .setHashCalculatorSha256() //计算 SHA256
        .setHashCalculator(Constant.Hash.MessageDigest.MD2) //指定哈希名称，这里定义了一些常用的哈希名称
        .setHashCalculator("SHA-512")   //指定哈希名称，内部是通过 MessageDigest 来计算哈希值的，只要是 MessageDigest 支持的名称就都可以
        .setHashCalculator(MessageDigest.getInstance("SHA-384"))    //指定 MessageDigest 
        .upload();

//上传成功后即可这样获取到对应的哈希值
HashInfo hashInfo = filePartInfo.getHashInfo();
String md5 = hashInfo.getMd5();
String sha256 = hashInfo.getSha256();
```

### 下载时

```java
Downloader downloader = fileStorageService.downloadTh(fileInfo)
        .setHashCalculatorMd5() //计算 MD5
        .setHashCalculatorSha256() //计算 SHA256
        .setHashCalculator(Constant.Hash.MessageDigest.MD2) //指定哈希名称，这里定义了一些常用的哈希名称
        .setHashCalculator("SHA-512")   //指定哈希名称，内部是通过 MessageDigest 来计算哈希值的，只要是 MessageDigest 支持的名称就都可以
        .setHashCalculator(MessageDigest.getInstance("SHA-384"));    //指定 MessageDigest

//下载为 byte[]
byte[] thBytes = downloader.bytes();

//下载成功后即可这样获取到对应的哈希值
HashInfo hashInfo = downloader.getHashCalculatorManager().getHashInfo();
String md5 = hashInfo.getMd5();
String sha256 = hashInfo.getSha256();
```

### 自定义计算哈希

只需要实现 `HashCalculator` 接口即可，这里用 `MD5` 举例

```java
FilePartInfo filePartInfo = fileStorageService.of(file)
        .setHashCalculator(new HashCalculator() {
            private final MessageDigest messageDigest = MessageDigest.getInstance("MD5");
        
            /**
             * 获取哈希名称，例如 MD5、SHA1、SHA256等
             */
            @Override
            public String getName() {
                return messageDigest.getAlgorithm();
            }
        
            /**
             * 获取哈希值，一般情况下获取后将不能继续增量计算哈希
             */
            @Override
            public String getValue() {
                return HexUtil.encodeHexStr(messageDigest.digest());
            }
        
            /**
             * 增量计算哈希
             * @param bytes 字节数组
             */
            @Override
            public void update(byte[] bytes) {
                messageDigest.update(bytes);
            }
        })
        .upload();

//上传成功后即可这样获取到对应的哈希值
HashInfo hashInfo = filePartInfo.getHashInfo();
String md5 = hashInfo.get("MD5");
```

