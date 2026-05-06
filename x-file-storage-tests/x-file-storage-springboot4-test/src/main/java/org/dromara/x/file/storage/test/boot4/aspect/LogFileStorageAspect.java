package org.dromara.x.file.storage.test.boot4.aspect;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ArrayUtil;
import java.io.InputStream;
import java.util.Date;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.UploadPretreatment;
import org.dromara.x.file.storage.core.aspect.*;
import org.dromara.x.file.storage.core.copy.CopyPretreatment;
import org.dromara.x.file.storage.core.get.*;
import org.dromara.x.file.storage.core.move.MovePretreatment;
import org.dromara.x.file.storage.core.platform.FileStorage;
import org.dromara.x.file.storage.core.presigned.GeneratePresignedUrlPretreatment;
import org.dromara.x.file.storage.core.presigned.GeneratePresignedUrlResult;
import org.dromara.x.file.storage.core.recorder.FileRecorder;
import org.dromara.x.file.storage.core.tika.ContentTypeDetect;
import org.dromara.x.file.storage.core.upload.*;
import org.springframework.stereotype.Component;

/**
 * 使用切面打印文件上传和删除的日志
 */
@Slf4j
@Component
public class LogFileStorageAspect implements FileStorageAspect {

    @Override
    public FileInfo uploadAround(
            UploadAspectChain chain,
            FileInfo fileInfo,
            UploadPretreatment pre,
            FileStorage fileStorage,
            FileRecorder fileRecorder) {
        log.info("上传文件 before -> {}", fileInfo);
        fileInfo = chain.next(fileInfo, pre, fileStorage, fileRecorder);
        log.info("上传文件 after -> {}", fileInfo);
        return fileInfo;
    }

    @Override
    public MultipartUploadSupportInfo isSupportMultipartUpload(
            IsSupportMultipartUploadAspectChain chain, FileStorage fileStorage) {
        log.info("是否支持手动分片上传 before -> {}", fileStorage.getPlatform());
        MultipartUploadSupportInfo res = chain.next(fileStorage);
        log.info("是否支持手动分片上传 -> {}", res);
        return res;
    }

    @Override
    public FileInfo initiateMultipartUploadAround(
            InitiateMultipartUploadAspectChain chain,
            FileInfo fileInfo,
            InitiateMultipartUploadPretreatment pre,
            FileStorage fileStorage,
            FileRecorder fileRecorder) {
        log.info("手动分片上传-初始化 before -> {}", fileInfo);
        fileInfo = chain.next(fileInfo, pre, fileStorage, fileRecorder);
        log.info("手动分片上传-初始化 after -> {}", fileInfo);
        return fileInfo;
    }

    @Override
    public FilePartInfo uploadPart(
            UploadPartAspectChain chain,
            UploadPartPretreatment pre,
            FileStorage fileStorage,
            FileRecorder fileRecorder) {
        log.info("手动分片上传-上传分片 before -> {}", pre.getFileInfo());
        FilePartInfo filePartInfo = chain.next(pre, fileStorage, fileRecorder);
        log.info("手动分片上传-上传分片 after -> {}", filePartInfo);
        return filePartInfo;
    }

    @Override
    public FileInfo completeMultipartUploadAround(
            CompleteMultipartUploadAspectChain chain,
            CompleteMultipartUploadPretreatment pre,
            FileStorage fileStorage,
            FileRecorder fileRecorder,
            ContentTypeDetect contentTypeDetect) {
        log.info("手动分片上传-完成 before -> {}", pre.getFileInfo());
        FileInfo fileInfo = chain.next(pre, fileStorage, fileRecorder, contentTypeDetect);
        log.info("手动分片上传-完成 after -> {}", fileInfo);
        return fileInfo;
    }

    @Override
    public FileInfo abortMultipartUploadAround(
            AbortMultipartUploadAspectChain chain,
            AbortMultipartUploadPretreatment pre,
            FileStorage fileStorage,
            FileRecorder fileRecorder) {
        log.info("手动分片上传-取消 before -> {}", pre.getFileInfo());
        FileInfo fileInfo = chain.next(pre, fileStorage, fileRecorder);
        log.info("手动分片上传-取消 after -> {}", fileInfo);
        return fileInfo;
    }

    @Override
    public FilePartInfoList listParts(ListPartsAspectChain chain, ListPartsPretreatment pre, FileStorage fileStorage) {
        log.info("手动分片上传-列举已上传的分片 before -> {}", pre.getFileInfo());
        FilePartInfoList list = chain.next(pre, fileStorage);
        log.info("手动分片上传-列举已上传的分片 after -> {}", list);
        return list;
    }

    @Override
    public ListFilesSupportInfo isSupportListFiles(IsSupportListFilesAspectChain chain, FileStorage fileStorage) {
        log.info("是否支持列举文件 before -> {}", fileStorage.getPlatform());
        ListFilesSupportInfo res = chain.next(fileStorage);
        log.info("是否支持列举文件 -> {}", res);
        return res;
    }

    @Override
    public ListFilesResult listFiles(ListFilesAspectChain chain, ListFilesPretreatment pre, FileStorage fileStorage) {
        log.info("列举文件 before -> {}", BeanUtil.beanToMap(pre, "fileStorageService"));
        ListFilesResult result = chain.next(pre, fileStorage);
        log.info("列举文件 after -> {}", result);
        return result;
    }

    @Override
    public RemoteFileInfo getFile(GetFileAspectChain chain, GetFilePretreatment pre, FileStorage fileStorage) {
        log.info("获取文件 before -> {}", BeanUtil.beanToMap(pre, "fileStorageService"));
        RemoteFileInfo result = chain.next(pre, fileStorage);
        log.info("获取文件 after -> {}", result);
        return result;
    }

    @Override
    public boolean deleteAround(
            DeleteAspectChain chain, FileInfo fileInfo, FileStorage fileStorage, FileRecorder fileRecorder) {
        log.info("删除文件 before -> {}", fileInfo);
        boolean res = chain.next(fileInfo, fileStorage, fileRecorder);
        log.info("删除文件 after -> {}", res);
        return res;
    }

    @Override
    public boolean existsAround(ExistsAspectChain chain, FileInfo fileInfo, FileStorage fileStorage) {
        log.info("文件是否存在 before -> {}", fileInfo);
        boolean res = chain.next(fileInfo, fileStorage);
        log.info("文件是否存在 after -> {}", res);
        return res;
    }

    @Override
    public void downloadAround(
            DownloadAspectChain chain, FileInfo fileInfo, FileStorage fileStorage, Consumer<InputStream> consumer) {
        log.info("下载文件 before -> {}", fileInfo);
        chain.next(fileInfo, fileStorage, consumer);
        log.info("下载文件 after -> {}", fileInfo);
    }

    @Override
    public void downloadThAround(
            DownloadThAspectChain chain, FileInfo fileInfo, FileStorage fileStorage, Consumer<InputStream> consumer) {
        log.info("下载缩略图文件 before -> {}", fileInfo);
        chain.next(fileInfo, fileStorage, consumer);
        log.info("下载缩略图文件 after -> {}", fileInfo);
    }

    @Override
    public boolean isSupportPresignedUrlAround(IsSupportPresignedUrlAspectChain chain, FileStorage fileStorage) {
        log.info("是否支持对文件生成可以签名访问的 URL before -> {}", fileStorage.getPlatform());
        boolean res = chain.next(fileStorage);
        log.info("是否支持对文件生成可以签名访问的 URL -> {}", res);
        return res;
    }

    @Override
    public GeneratePresignedUrlResult generatePresignedUrlAround(
            GeneratePresignedUrlAspectChain chain, GeneratePresignedUrlPretreatment pre, FileStorage fileStorage) {
        log.info("对文件生成可以签名访问的 URL before -> {}", BeanUtil.beanToMap(pre, "fileStorageService"));
        GeneratePresignedUrlResult res = chain.next(pre, fileStorage);
        log.info("对文件生成可以签名访问的 URL after -> {}", res);
        return res;
    }

    @Override
    public String generateThPresignedUrlAround(
            GenerateThPresignedUrlAspectChain chain, FileInfo fileInfo, Date expiration, FileStorage fileStorage) {
        log.info("对缩略图文件生成可以签名访问的 URL before -> {}", fileInfo);
        String res = chain.next(fileInfo, expiration, fileStorage);
        log.info("对缩略图文件生成可以签名访问的 URL after -> {}", res);
        return res;
    }

    @Override
    public boolean isSupportAclAround(IsSupportAclAspectChain chain, FileStorage fileStorage) {
        log.info("是否支持文件的访问控制列表 before -> {}", fileStorage.getPlatform());
        boolean res = chain.next(fileStorage);
        log.info("是否支持文件的访问控制列表 -> {}", res);
        return res;
    }

    @Override
    public boolean setFileAcl(SetFileAclAspectChain chain, FileInfo fileInfo, Object acl, FileStorage fileStorage) {
        log.info("设置文件的访问控制列表 before -> {}", fileInfo);
        boolean res = chain.next(fileInfo, acl, fileStorage);
        log.info("设置文件的访问控制列表 URL after -> {}", res);
        return res;
    }

    @Override
    public boolean setThFileAcl(SetThFileAclAspectChain chain, FileInfo fileInfo, Object acl, FileStorage fileStorage) {
        log.info("设置缩略图文件的访问控制列表 before -> {}", fileInfo);
        boolean res = chain.next(fileInfo, acl, fileStorage);
        log.info("设置缩略图文件的访问控制列表 URL after -> {}", res);
        return res;
    }

    @Override
    public boolean isSupportMetadataAround(IsSupportMetadataAspectChain chain, FileStorage fileStorage) {
        log.info("是否支持 Metadata before -> {}", fileStorage.getPlatform());
        boolean res = chain.next(fileStorage);
        log.info("是否支持 Metadata -> {}", res);
        return res;
    }

    @Override
    public boolean isSupportSameCopyAround(IsSupportSameCopyAspectChain chain, FileStorage fileStorage) {
        log.info("是否支持同存储平台复制 before -> {}", fileStorage.getPlatform());
        boolean res = chain.next(fileStorage);
        log.info("是否支持同存储平台复制 -> {}", res);
        return res;
    }

    @Override
    public FileInfo sameCopyAround(
            SameCopyAspectChain chain,
            FileInfo srcFileInfo,
            FileInfo destFileInfo,
            CopyPretreatment pre,
            FileStorage fileStorage,
            FileRecorder fileRecorder) {
        log.info("同存储平台复制文件 before -> srcFileInfo：{}，destFileInfo：{}", srcFileInfo, destFileInfo);
        destFileInfo = chain.next(srcFileInfo, destFileInfo, pre, fileStorage, fileRecorder);
        log.info("同存储平台复制文件 after -> srcFileInfo：{}，destFileInfo：{}", srcFileInfo, destFileInfo);
        return destFileInfo;
    }

    @Override
    public FileInfo copyAround(
            CopyAspectChain chain,
            FileInfo srcFileInfo,
            CopyPretreatment pre,
            FileStorage fileStorage,
            FileRecorder fileRecorder) {
        log.info("复制文件 before -> {}", srcFileInfo);
        srcFileInfo = chain.next(srcFileInfo, pre, fileStorage, fileRecorder);
        log.info("复制文件 after -> {}", srcFileInfo);
        return srcFileInfo;
    }

    @Override
    public boolean isSupportSameMoveAround(IsSupportSameMoveAspectChain chain, FileStorage fileStorage) {
        log.info("是否支持同存储平台移动 before -> {}", fileStorage.getPlatform());
        boolean res = chain.next(fileStorage);
        log.info("是否支持同存储平台移动 -> {}", res);
        return res;
    }

    @Override
    public FileInfo sameMoveAround(
            SameMoveAspectChain chain,
            FileInfo srcFileInfo,
            FileInfo destFileInfo,
            MovePretreatment pre,
            FileStorage fileStorage,
            FileRecorder fileRecorder) {
        log.info("同存储平台移动文件 before -> srcFileInfo：{}，destFileInfo：{}", srcFileInfo, pre.getFileInfo());
        destFileInfo = chain.next(srcFileInfo, destFileInfo, pre, fileStorage, fileRecorder);
        log.info("同存储平台移动文件 after -> srcFileInfo：{}，destFileInfo：{}", srcFileInfo, destFileInfo);
        return destFileInfo;
    }

    @Override
    public FileInfo moveAround(
            MoveAspectChain chain,
            FileInfo srcFileInfo,
            MovePretreatment pre,
            FileStorage fileStorage,
            FileRecorder fileRecorder) {
        log.info("移动文件 before -> {}", srcFileInfo);
        srcFileInfo = chain.next(srcFileInfo, pre, fileStorage, fileRecorder);
        log.info("移动文件 after -> {}", srcFileInfo);
        return srcFileInfo;
    }

    @Override
    public <T> T invoke(InvokeAspectChain chain, FileStorage fileStorage, String method, Object[] args) {
        log.info("通过反射调用指定存储平台的方法 before -> {}.{}({})", fileStorage.getPlatform(), method, ArrayUtil.join(args, ", "));
        T res = chain.next(fileStorage, method, args);
        log.info("通过反射调用指定存储平台的方法 after -> {}", res);
        return res;
    }
}
