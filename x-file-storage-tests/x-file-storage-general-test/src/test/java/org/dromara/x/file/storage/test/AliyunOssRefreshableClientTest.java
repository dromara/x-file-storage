package org.dromara.x.file.storage.test;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ReflectUtil;
import lombok.extern.slf4j.Slf4j;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.platform.FileStorage;
import org.dromara.x.file.storage.core.platform.RefreshableClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.InputStream;

/**
 * RefreshableClient 自愈验证（针对 <a href="https://github.com/dromara/x-file-storage/issues/331">Issue #331</a>）
 *
 * <p>场景：阿里云 OSS SDK 内部 Apache HttpClient 连接池被永久 shutdown 后，
 * RefreshableClient 应在下次请求时自动重建客户端，使后续请求恢复成功。
 */
@Slf4j
@SpringBootTest
class AliyunOssRefreshableClientTest {

    private static final String PLATFORM = "aliyun-oss-1";

    @Autowired
    private FileStorageService fileStorageService;

    /**
     * 反射 navigate 到 OSSClient 内部的 PoolingHttpClientConnectionManager 调 shutdown，
     * 模拟 OOM 后连接池被永久 shutdown 的终态，验证 RefreshableClient 自愈。
     */
    @Test
    public void reflectionShutdown() {
        log.info("==== step 1：第一次上传，触发 RefreshableClient 初始化 ====");
        FileInfo info1 = upload();
        log.info("第一次上传成功：{}", info1.getUrl());

        FileStorage storage = fileStorageService.getFileStorage(PLATFORM);
        Assert.notNull(storage, "未找到平台 {}，请检查 application.yml", PLATFORM);
        Object factory = ReflectUtil.invoke(storage, "getClientFactory");
        RefreshableClient<?> ref = ReflectUtil.invoke(factory, "getRefreshableClient");
        Object rawBefore = ref.getRaw();
        Assert.notNull(rawBefore, "raw client 应已初始化");

        log.info("==== step 2：反射调用 connectionManager.shutdown() 模拟连接池失效 ====");
        // OSSClient.serviceClient(private) -> DefaultServiceClient.connectionManager(protected,
        // 实际类型 PoolingHttpClientConnectionManager)
        Object serviceClient = ReflectUtil.getFieldValue(rawBefore, "serviceClient");
        Object connManager = ReflectUtil.getFieldValue(serviceClient, "connectionManager");
        ReflectUtil.invoke(connManager, "shutdown");
        log.info("connectionManager.shutdown() 已调用，hash={}", System.identityHashCode(connManager));

        log.info("==== step 3：第二次上传，预期抛 IllegalStateException(Connection pool shut down) ====");
        boolean threw = false;
        try {
            upload();
        } catch (Exception e) {
            threw = RefreshableClient.isApacheHttpClientPoolShutdown(e);
            if (threw) {
                log.info("第二次上传按预期抛连接池 shutdown 异常");
            } else {
                throw e;
            }
        }
        Assert.isTrue(threw, "第二次上传应抛 Connection pool shut down 异常");

        log.info("==== step 4：第三次上传，预期 RefreshableClient 已重建客户端 ====");
        FileInfo info3 = upload();
        log.info("第三次上传成功：{}", info3.getUrl());
        Object rawAfter = ref.getRaw();
        Assert.isTrue(rawBefore != rawAfter, "raw client 应已被重建（identity 不同）");
        log.info(
                "==== 验证完成：rawBefore.hash={} rawAfter.hash={} ====",
                System.identityHashCode(rawBefore),
                System.identityHashCode(rawAfter));
    }

    /**
     * 上传一个测试文件到 aliyun-oss-1 平台并立即清理
     */
    private FileInfo upload() {
        String filename = "image.jpg";
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(filename);
        FileInfo fileInfo = fileStorageService
                .of(in)
                .setOriginalFilename(filename)
                .setPlatform(PLATFORM)
                .setPath("test/refreshable/")
                .upload();
        Assert.notNull(fileInfo, "文件上传失败！");
        try {
            fileStorageService.delete(fileInfo);
        } catch (Exception ignored) {
            // 验证流程不强制清理，删除失败不影响断言
        }
        return fileInfo;
    }
}
