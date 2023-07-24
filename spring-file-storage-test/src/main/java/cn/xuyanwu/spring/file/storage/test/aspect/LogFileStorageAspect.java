package cn.xuyanwu.spring.file.storage.test.aspect;

import cn.hutool.core.util.ArrayUtil;
import cn.xuyanwu.spring.file.storage.FileInfo;
import cn.xuyanwu.spring.file.storage.UploadPretreatment;
import cn.xuyanwu.spring.file.storage.aspect.*;
import cn.xuyanwu.spring.file.storage.platform.FileStorage;
import cn.xuyanwu.spring.file.storage.recorder.FileRecorder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Date;
import java.util.function.Consumer;

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
    public FileInfo uploadAround(UploadAspectChain chain,FileInfo fileInfo,UploadPretreatment pre,FileStorage fileStorage,FileRecorder fileRecorder) {
        log.info("上传文件 before -> {}",fileInfo);
        fileInfo = chain.next(fileInfo,pre,fileStorage,fileRecorder);
        log.info("上传文件 after -> {}",fileInfo);
        return fileInfo;
    }

    /**
     * 删除文件，成功返回 true
     */
    @Override
    public boolean deleteAround(DeleteAspectChain chain,FileInfo fileInfo,FileStorage fileStorage,FileRecorder fileRecorder) {
        log.info("删除文件 before -> {}",fileInfo);
        boolean res = chain.next(fileInfo,fileStorage,fileRecorder);
        log.info("删除文件 after -> {}",res);
        return res;
    }

    /**
     * 文件是否存在
     */
    @Override
    public boolean existsAround(ExistsAspectChain chain,FileInfo fileInfo,FileStorage fileStorage) {
        log.info("文件是否存在 before -> {}",fileInfo);
        boolean res = chain.next(fileInfo,fileStorage);
        log.info("文件是否存在 after -> {}",res);
        return res;
    }

    /**
     * 下载文件
     */
    @Override
    public void downloadAround(DownloadAspectChain chain,FileInfo fileInfo,FileStorage fileStorage,Consumer<InputStream> consumer) {
        log.info("下载文件 before -> {}",fileInfo);
        chain.next(fileInfo,fileStorage,consumer);
        log.info("下载文件 after -> {}",fileInfo);
    }

    /**
     * 下载缩略图文件
     */
    @Override
    public void downloadThAround(DownloadThAspectChain chain,FileInfo fileInfo,FileStorage fileStorage,Consumer<InputStream> consumer) {
        log.info("下载缩略图文件 before -> {}",fileInfo);
        chain.next(fileInfo,fileStorage,consumer);
        log.info("下载缩略图文件 after -> {}",fileInfo);
    }

    @Override
    public boolean isSupportPresignedUrlAround(IsSupportPresignedUrlAspectChain chain,FileStorage fileStorage) {
        log.info("是否支持对文件生成可以签名访问的 URL before -> {}",fileStorage.getPlatform());
        boolean res = chain.next(fileStorage);
        log.info("是否支持对文件生成可以签名访问的 URL -> {}",res);
        return res;
    }

    /**
     * 对文件生成可以签名访问的 URL，无法生成则返回 null
     */
    @Override
    public String generatePresignedUrlAround(GeneratePresignedUrlAspectChain chain,FileInfo fileInfo,Date expiration,FileStorage fileStorage) {
        log.info("对文件生成可以签名访问的 URL before -> {}",fileInfo);
        String res = chain.next(fileInfo,expiration,fileStorage);
        log.info("对文件生成可以签名访问的 URL after -> {}",res);
        return res;
    }

    /**
     * 对缩略图文件生成可以签名访问的 URL，无法生成则返回 null
     */
    @Override
    public String generateThPresignedUrlAround(GenerateThPresignedUrlAspectChain chain,FileInfo fileInfo,Date expiration,FileStorage fileStorage) {
        log.info("对缩略图文件生成可以签名访问的 URL before -> {}",fileInfo);
        String res = chain.next(fileInfo,expiration,fileStorage);
        log.info("对缩略图文件生成可以签名访问的 URL after -> {}",res);
        return res;
    }

    @Override
    public boolean isSupportAclAround(IsSupportAclAspectChain chain,FileStorage fileStorage) {
        log.info("是否支持文件的访问控制列表 before -> {}",fileStorage.getPlatform());
        boolean res = chain.next(fileStorage);
        log.info("是否支持文件的访问控制列表 -> {}",res);
        return res;
    }

    /**
     * 设置文件的访问控制列表，一般情况下只有对象存储支持该功能
     */
    @Override
    public boolean setFileAcl(SetFileAclAspectChain chain,FileInfo fileInfo,Object acl,FileStorage fileStorage) {
        log.info("设置文件的访问控制列表 before -> {}",fileInfo);
        boolean res = chain.next(fileInfo,acl,fileStorage);
        log.info("设置文件的访问控制列表 URL after -> {}",res);
        return res;
    }

    /**
     * 设置缩略图文件的访问控制列表，一般情况下只有对象存储支持该功能
     */
    @Override
    public boolean setThFileAcl(SetThFileAclAspectChain chain,FileInfo fileInfo,Object acl,FileStorage fileStorage) {
        log.info("设置缩略图文件的访问控制列表 before -> {}",fileInfo);
        boolean res = chain.next(fileInfo,acl,fileStorage);
        log.info("设置缩略图文件的访问控制列表 URL after -> {}",res);
        return res;
    }

    /**
     * 通过反射调用指定存储平台的方法
     */
    @Override
    public <T> T invoke(InvokeAspectChain chain,FileStorage fileStorage,String method,Object[] args) {
        log.info("通过反射调用指定存储平台的方法 before -> {}.{}({})",fileStorage.getPlatform(),method,ArrayUtil.join(args,", "));
        T res = chain.next(fileStorage,method,args);
        log.info("通过反射调用指定存储平台的方法 before -> {}",res);
        return res;
    }
}
