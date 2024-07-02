package org.dromara.x.file.storage.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.constant.Constant;
import org.dromara.x.file.storage.core.get.ListFilesResult;
import org.dromara.x.file.storage.core.get.RemoteDirInfo;
import org.dromara.x.file.storage.core.get.RemoteFileInfo;
import org.dromara.x.file.storage.core.util.KebabCaseInsensitiveMap;
import org.dromara.x.file.storage.test.service.FileDetailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
class FileStorageServiceTransferTest {

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private FileDetailService fileDetailService;

    /**
     * 使用数据库迁移文件
     */
    @Test
    public void transferByDb() {
        // 目标存储平台
        String toPlatform = "azure-blob-1";

        // 从数据库中读取要迁移的文件信息
        List<FileInfo> list = fileDetailService.list().stream()
                .map(detail -> {
                    try {
                        return fileDetailService.toFileInfo(detail);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());

        for (FileInfo fileInfo : list) {
            // 使用复制文件，会保留旧文件，推荐
            fileStorageService
                    .copy(fileInfo)
                    .setPlatform(toPlatform)
                    .setNotSupportMetadataThrowException(true) // 不支持元数据时抛出异常
                    .setNotSupportAclThrowException(true) // 不支持 ACL 时抛出异常
                    .setProgressListener((progressSize, allSize) -> log.info(
                            "文件 {}{}{} 迁移进度：{} {}%",
                            fileInfo.getBasePath(),
                            fileInfo.getPath(),
                            fileInfo.getFilename(),
                            progressSize,
                            progressSize * 100 / allSize))
                    .copy();

            // 使用移动文件，会删除旧文件，不推荐
            // fileStorageService
            //        .move(fileInfo)
            //        .setPlatform(toPlatform)
            //        .setNotSupportMetadataThrowException(true) // 不支持元数据时抛出异常
            //        .setNotSupportAclThrowException(true) // 不支持 ACL 时抛出异常
            //        .setProgressListener((progressSize, allSize) -> log.info(
            //                "文件 {}{}{} 迁移进度：{} {}%",
            //                fileInfo.getBasePath(),
            //                fileInfo.getPath(),
            //                fileInfo.getFilename(),
            //                progressSize,
            //                progressSize * 100 / allSize))
            //        .move();
        }
    }

    /**
     * 使用数据库迁移文件
     */
    @Test
    public void transferByPlatform() {
        // 源存储平台
        String fromPlatform = "azure-blob-1";
        // 目标存储平台
        String toPlatform = "huawei-obs-1";
        // 要迁移的路径
        String path = "";
        // 开始迁移
        transfer(fromPlatform, toPlatform, path);
    }

    /**
     * 使用数据库迁移文件
     */
    public void transfer(String fromPlatform, String toPlatform, String path) {

        // 例举出当前路径下所有目录及文件
        ListFilesResult result = fileStorageService
                .listFiles()
                .setPlatform(fromPlatform)
                .setPath(path)
                .listFiles();

        // 递归迁移所有子目录
        for (RemoteDirInfo dir : result.getDirList()) {
            transfer(fromPlatform, toPlatform, path + dir.getName() + "/");
        }

        // 迁移当前路径下所有文件
        for (RemoteFileInfo remoteFileInfo : result.getFileList()) {
            // 转换成 FileInfo，注意 createTime 、metadata 及 userMetadata 可能需要自行处理，详情查看方法源码注释
            // 同时每个存储平台的 ACL 也不一样，也需要自行处理
            FileInfo fileInfo = remoteFileInfo.toFileInfo();

            // 这里仅保留需要的 metadata ，例如：
            Map<String, String> fromMetadata = new KebabCaseInsensitiveMap<>(fileInfo.getMetadata());
            Map<String, String> toMetadata = new HashMap<>();
            if (fromMetadata.containsKey(Constant.Metadata.CONTENT_TYPE)) {
                toMetadata.put(Constant.Metadata.CONTENT_TYPE, fromMetadata.get(Constant.Metadata.CONTENT_TYPE));
            }
            if (fromMetadata.containsKey(Constant.Metadata.CONTENT_LENGTH)) {
                toMetadata.put(Constant.Metadata.CONTENT_LENGTH, fromMetadata.get(Constant.Metadata.CONTENT_LENGTH));
            }
            if (fromMetadata.containsKey(Constant.Metadata.CONTENT_DISPOSITION)) {
                toMetadata.put(
                        Constant.Metadata.CONTENT_DISPOSITION, fromMetadata.get(Constant.Metadata.CONTENT_DISPOSITION));
            }
            fileInfo.setMetadata(toMetadata);

            // 使用复制文件，会保留旧文件，推荐
            fileStorageService
                    .move(fileInfo)
                    .setPlatform(toPlatform)
                    .setNotSupportMetadataThrowException(true) // 不支持元数据时抛出异常
                    .setNotSupportAclThrowException(true) // 不支持 ACL 时抛出异常
                    .setProgressListener((progressSize, allSize) -> log.info(
                            "文件 {}/{} 迁移进度：{} {}%",
                            fileInfo.getPath(), fileInfo.getFilename(), progressSize, progressSize * 100 / allSize))
                    .move();
            // 使用移动文件，会删除旧文件，不推荐
            // fileStorageService
            //        .copy(fileInfo)
            //        .setPlatform(toPlatform)
            //        .setNotSupportMetadataThrowException(true) // 不支持元数据时抛出异常
            //        .setNotSupportAclThrowException(true) // 不支持 ACL 时抛出异常
            //        .setProgressListener((progressSize, allSize) -> log.info(
            //                "文件 {}/{} 迁移进度：{} {}%",
            //                fileInfo.getPath(), fileInfo.getFilename(), progressSize, progressSize * 100 / allSize))
            //        .copy();
        }
    }
}
