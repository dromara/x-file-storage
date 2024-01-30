package org.dromara.x.file.storage.test.aspect;

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
import org.dromara.x.file.storage.core.recorder.FileRecorder;
import org.dromara.x.file.storage.core.tika.ContentTypeDetect;
import org.dromara.x.file.storage.core.upload.*;
import org.dromara.x.file.storage.core.upload.MultipartUploadSupportInfo;
import org.springframework.stereotype.Component;

/**
 * 使用切面打印文件上传和删除的日志
 */
@Slf4j
@Component
public class LogFileStorageAspect implements FileStorageAspect {

    /**
     * 上传，成功返回文件信息，失败返回 null
     */
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

    /**
     * 是否支持手动分片上传
     */
    @Override
    public MultipartUploadSupportInfo isSupportMultipartUpload(
            IsSupportMultipartUploadAspectChain chain, FileStorage fileStorage) {
        log.info("是否支持手动分片上传 before -> {}", fileStorage.getPlatform());
        MultipartUploadSupportInfo res = chain.next(fileStorage);
        log.info("是否支持手动分片上传 -> {}", res);
        return res;
    }

    /**
     * 手动分片上传-初始化，成功返回文件信息，失败返回 null
     */
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

    /**
     * 手动分片上传-上传分片，成功返回文件信息
     */
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

    /**
     * 手动分片上传-完成
     */
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

    /**
     * 手动分片上传-取消
     */
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

    /**
     * 手动分片上传-列举已上传的分片
     */
    @Override
    public FilePartInfoList listParts(ListPartsAspectChain chain, ListPartsPretreatment pre, FileStorage fileStorage) {
        log.info("手动分片上传-列举已上传的分片 before -> {}", pre.getFileInfo());
        FilePartInfoList list = chain.next(pre, fileStorage);
        log.info("手动分片上传-列举已上传的分片 after -> {}", list);
        return list;
    }

    /**
     * 获取文件
     */
    @Override
    public RemoteFileInfo getFile(GetFileAspectChain chain, GetFilePretreatment pre, FileStorage fileStorage) {
        log.info("获取文件 before -> {}", BeanUtil.beanToMap(pre, "fileStorageService"));
        RemoteFileInfo result = chain.next(pre, fileStorage);
        log.info("获取文件 after -> {}", result);
        return result;
    }

    /**
     * 删除文件，成功返回 true
     */
    @Override
    public boolean deleteAround(
            DeleteAspectChain chain, FileInfo fileInfo, FileStorage fileStorage, FileRecorder fileRecorder) {
        log.info("删除文件 before -> {}", fileInfo);
        boolean res = chain.next(fileInfo, fileStorage, fileRecorder);
        log.info("删除文件 after -> {}", res);
        return res;
    }

    /**
     * 文件是否存在
     */
    @Override
    public boolean existsAround(ExistsAspectChain chain, FileInfo fileInfo, FileStorage fileStorage) {
        log.info("文件是否存在 before -> {}", fileInfo);
        boolean res = chain.next(fileInfo, fileStorage);
        log.info("文件是否存在 after -> {}", res);
        return res;
    }

    /**
     * 下载文件
     */
    @Override
    public void downloadAround(
            DownloadAspectChain chain, FileInfo fileInfo, FileStorage fileStorage, Consumer<InputStream> consumer) {
        log.info("下载文件 before -> {}", fileInfo);
        chain.next(fileInfo, fileStorage, consumer);
        log.info("下载文件 after -> {}", fileInfo);
    }

    /**
     * 下载缩略图文件
     */
    @Override
    public void downloadThAround(
            DownloadThAspectChain chain, FileInfo fileInfo, FileStorage fileStorage, Consumer<InputStream> consumer) {
        log.info("下载缩略图文件 before -> {}", fileInfo);
        chain.next(fileInfo, fileStorage, consumer);
        log.info("下载缩略图文件 after -> {}", fileInfo);
    }

    /**
     * 是否支持对文件生成可以签名访问的 URL
     */
    @Override
    public boolean isSupportPresignedUrlAround(IsSupportPresignedUrlAspectChain chain, FileStorage fileStorage) {
        log.info("是否支持对文件生成可以签名访问的 URL before -> {}", fileStorage.getPlatform());
        boolean res = chain.next(fileStorage);
        log.info("是否支持对文件生成可以签名访问的 URL -> {}", res);
        return res;
    }

    /**
     * 对文件生成可以签名访问的 URL，无法生成则返回 null
     */
    @Override
    public String generatePresignedUrlAround(
            GeneratePresignedUrlAspectChain chain, FileInfo fileInfo, Date expiration, FileStorage fileStorage) {
        log.info("对文件生成可以签名访问的 URL before -> {}", fileInfo);
        String res = chain.next(fileInfo, expiration, fileStorage);
        log.info("对文件生成可以签名访问的 URL after -> {}", res);
        return res;
    }

    /**
     * 对缩略图文件生成可以签名访问的 URL，无法生成则返回 null
     */
    @Override
    public String generateThPresignedUrlAround(
            GenerateThPresignedUrlAspectChain chain, FileInfo fileInfo, Date expiration, FileStorage fileStorage) {
        log.info("对缩略图文件生成可以签名访问的 URL before -> {}", fileInfo);
        String res = chain.next(fileInfo, expiration, fileStorage);
        log.info("对缩略图文件生成可以签名访问的 URL after -> {}", res);
        return res;
    }

    /**
     * 是否支持文件的访问控制列表，一般情况下只有对象存储支持该功能
     */
    @Override
    public boolean isSupportAclAround(IsSupportAclAspectChain chain, FileStorage fileStorage) {
        log.info("是否支持文件的访问控制列表 before -> {}", fileStorage.getPlatform());
        boolean res = chain.next(fileStorage);
        log.info("是否支持文件的访问控制列表 -> {}", res);
        return res;
    }

    /**
     * 设置文件的访问控制列表，一般情况下只有对象存储支持该功能
     */
    @Override
    public boolean setFileAcl(SetFileAclAspectChain chain, FileInfo fileInfo, Object acl, FileStorage fileStorage) {
        log.info("设置文件的访问控制列表 before -> {}", fileInfo);
        boolean res = chain.next(fileInfo, acl, fileStorage);
        log.info("设置文件的访问控制列表 URL after -> {}", res);
        return res;
    }

    /**
     * 设置缩略图文件的访问控制列表，一般情况下只有对象存储支持该功能
     */
    @Override
    public boolean setThFileAcl(SetThFileAclAspectChain chain, FileInfo fileInfo, Object acl, FileStorage fileStorage) {
        log.info("设置缩略图文件的访问控制列表 before -> {}", fileInfo);
        boolean res = chain.next(fileInfo, acl, fileStorage);
        log.info("设置缩略图文件的访问控制列表 URL after -> {}", res);
        return res;
    }

    /**
     * 是否支持 Metadata
     */
    @Override
    public boolean isSupportMetadataAround(IsSupportMetadataAspectChain chain, FileStorage fileStorage) {
        log.info("是否支持 Metadata before -> {}", fileStorage.getPlatform());
        boolean res = chain.next(fileStorage);
        log.info("是否支持 Metadata -> {}", res);
        return res;
    }

    /**
     * 是否支持同存储平台复制
     */
    @Override
    public boolean isSupportSameCopyAround(IsSupportSameCopyAspectChain chain, FileStorage fileStorage) {
        log.info("是否支持同存储平台复制 before -> {}", fileStorage.getPlatform());
        boolean res = chain.next(fileStorage);
        log.info("是否支持同存储平台复制 -> {}", res);
        return res;
    }

    /**
     * 同存储平台复制，成功返回文件信息
     */
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

    /**
     * 复制，成功返回文件信息
     */
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

    /**
     * 是否支持同存储平台移动
     */
    @Override
    public boolean isSupportSameMoveAround(IsSupportSameMoveAspectChain chain, FileStorage fileStorage) {
        log.info("是否支持同存储平台移动 before -> {}", fileStorage.getPlatform());
        boolean res = chain.next(fileStorage);
        log.info("是否支持同存储平台移动 -> {}", res);
        return res;
    }

    /**
     * 同存储平台移动，成功返回文件信息
     */
    @Override
    public FileInfo sameMoveAround(
            SameMoveAspectChain chain,
            FileInfo srcFileInfo,
            FileInfo destFileInfo,
            MovePretreatment pre,
            FileStorage fileStorage,
            FileRecorder fileRecorder) {
        log.info("同存储平台复制文件 before -> srcFileInfo：{}，destFileInfo：{}", srcFileInfo, pre.getFileInfo());

        destFileInfo = chain.next(srcFileInfo, destFileInfo, pre, fileStorage, fileRecorder);
        log.info("同存储平台复制文件 after -> srcFileInfo：{}，destFileInfo：{}", srcFileInfo, destFileInfo);
        return destFileInfo;
    }

    /**
     * 移动，成功返回文件信息
     */
    @Override
    public FileInfo moveAround(
            MoveAspectChain chain,
            FileInfo srcFileInfo,
            MovePretreatment pre,
            FileStorage fileStorage,
            FileRecorder fileRecorder) {
        log.info("复制文件 before -> {}", srcFileInfo);
        srcFileInfo = chain.next(srcFileInfo, pre, fileStorage, fileRecorder);
        log.info("复制文件 after -> {}", srcFileInfo);
        return srcFileInfo;
    }

    /**
     * 通过反射调用指定存储平台的方法
     */
    @Override
    public <T> T invoke(InvokeAspectChain chain, FileStorage fileStorage, String method, Object[] args) {
        log.info("通过反射调用指定存储平台的方法 before -> {}.{}({})", fileStorage.getPlatform(), method, ArrayUtil.join(args, ", "));
        T res = chain.next(fileStorage, method, args);
        log.info("通过反射调用指定存储平台的方法 before -> {}", res);
        return res;
    }
}
