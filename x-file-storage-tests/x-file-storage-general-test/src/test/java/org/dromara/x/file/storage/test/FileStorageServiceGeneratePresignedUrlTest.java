package org.dromara.x.file.storage.test;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import java.util.Arrays;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.constant.Constant;
import org.dromara.x.file.storage.core.presigned.GeneratePresignedUrlResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
class FileStorageServiceGeneratePresignedUrlTest {

    @Autowired
    private FileStorageService fileStorageService;

    /**
     * 测试生成预签名 URL
     */
    @Test
    public void generatePresignedUrl() {
        if (!fileStorageService.isSupportPresignedUrl()) {
            log.info("暂不支持生成预签名 URL");
            return;
        }

        byte[] bytes = IoUtil.readBytes(this.getClass().getClassLoader().getResourceAsStream("image.jpg"));

        // ======================= 测试上传 =======================
        GeneratePresignedUrlResult result = upload(bytes);
        // ======================= 测试访问 =======================
        download(result.getPath(), result.getFilename(), bytes);
        // ======================= 测试删除 =======================
        delete(result.getPlatform(), result.getBasePath(), result.getPath(), result.getFilename());
    }

    /**
     * 测试生成预签名 URL，主要用来测试 specialParam （特殊操作符）
     */
    @Test
    public void generatePresignedUrlSpecialParam() {
        if (!fileStorageService.isSupportPresignedUrl()) {
            log.info("暂不支持生成预签名 URL");
            return;
        }
        // ======================= 测试初始化分段上传任务 =====================
        GeneratePresignedUrlResult result = multipartUploadInit();
        // ======================= 测试删除分段上传任务 =====================
        delete(result.getPlatform(), result.getBasePath(), result.getPath(), result.getFilename());
    }

    /**
     * 测试分段上传-初始化
     */
    public GeneratePresignedUrlResult multipartUploadInit() {
        GeneratePresignedUrlResult result = fileStorageService
                .generatePresignedUrl()
                .setPath("test/")
                .setFilename("image-m.jpg")
                .setMethod(Constant.GeneratePresignedUrl.Method.POST)
                .putHeaders(Constant.Metadata.CONTENT_TYPE, "image/jpeg")
                .setSpecialParam("UPLOADS")
                .setExpiration(DateUtil.offsetMinute(new Date(), 10))
                .generatePresignedUrl();
        Assert.notNull(result, "生成分段上传任务-初始化任务预签名 URL 失败！");
        log.info("生成分段上传任务-初始化预签名 URL 结果：{}", result);

        String response = HttpRequest.of(result.getUrl())
                .method(Method.POST)
                .addHeaders(result.getHeaders())
                .execute()
                .body();

        String uploadId = StrUtil.subBetween(response, "<UploadId>", "</UploadId>");
        log.info("生成分段上传任务-初始化任务结果 uploadId：{}", uploadId);
        Assert.notBlank(uploadId, "生成分段上传任务-初始化任务预签名 URL 失败，uploadId 为空！");
        return result;
    }

    /**
     * 测试上传
     */
    private GeneratePresignedUrlResult upload(byte[] bytes) {

        GeneratePresignedUrlResult uploadResult = fileStorageService
                .generatePresignedUrl()
                .setPath("test/")
                .setFilename("image.jpg")
                .setMethod(Constant.GeneratePresignedUrl.Method.PUT)
                .setExpiration(DateUtil.offsetMinute(new Date(), 10))
                .putHeaders(Constant.Metadata.CONTENT_TYPE, "image/jpeg")
                .putHeaders(Constant.Metadata.CONTENT_DISPOSITION, "attachment;filename=DownloadFileName.jpg")
                .putUserMetadata("role", "666")
                .putQueryParams("admin", "123456")
                .generatePresignedUrl();
        Assert.notNull(uploadResult, "生成上传预签名 URL 失败！");
        log.info("生成上传预签名 URL 结果：{}", uploadResult);
        String body = HttpRequest.of(uploadResult.getUrl())
                .method(Method.PUT)
                .addHeaders(uploadResult.getHeaders())
                .body(bytes)
                .execute()
                .body();
        Assert.isTrue(StrUtil.isBlank(body), "生成上传预签名 URL 失败：{}", body);

        log.info("上传成功");
        return uploadResult;
    }

    /**
     * 测试访问
     */
    private void download(String path, String filename, byte[] bytes) {
        GeneratePresignedUrlResult downloadResult = fileStorageService
                .generatePresignedUrl()
                .setPath(path)
                .setFilename(filename)
                .setMethod(Constant.GeneratePresignedUrl.Method.GET)
                .setExpiration(DateUtil.offsetMinute(new Date(), 10))
                .generatePresignedUrl();
        Assert.notNull(downloadResult, "生成访问预签名 URL 失败！");
        log.info("生成访问预签名 URL 结果：{}", downloadResult);
        try {
            byte[] downloadBytes = HttpUtil.downloadBytes(downloadResult.getUrl());
            Assert.notNull(downloadBytes);
            Assert.isTrue(Arrays.equals(downloadBytes, bytes));
        } catch (Exception e) {
            Assert.isTrue(false, "生成访问预签名 URL 失败，无法访问 URL");
        }
        log.info("生成访问预签名 URL 通过");
    }

    /**
     * 测试删除
     */
    private void delete(String platform, String basePath, String path, String filename) {
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

        String deleteHttpResponse = HttpRequest.of(deleteResult.getUrl())
                .method(Method.DELETE)
                .addHeaders(deleteResult.getHeaders())
                .execute()
                .body();

        boolean exists = fileStorageService.exists(new FileInfo(basePath, path, filename).setPlatform(platform));
        Assert.isFalse(exists, "删除失败：{}", deleteHttpResponse);
        log.info("删除成功");
    }
}
