package org.dromara.x.file.storage.core.upload;

import cn.hutool.core.lang.Dict;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.coobird.thumbnailator.Thumbnails;
import org.dromara.x.file.storage.core.*;
import org.dromara.x.file.storage.core.aspect.FileStorageAspect;
import org.dromara.x.file.storage.core.file.FileWrapper;
import org.dromara.x.file.storage.core.hash.HashCalculator;
import org.dromara.x.file.storage.core.hash.HashCalculatorManager;
import org.dromara.x.file.storage.core.platform.FileStorage;
import org.dromara.x.file.storage.core.recorder.FileRecorder;

/**
 * 文件上传预处理对象
 */
@NoArgsConstructor
@Getter
@Setter
@Accessors(chain = true)
public class UploadPretreatment extends org.dromara.x.file.storage.core.UploadPretreatment {

    /**
     * 通过旧的 UploadPretreatment 创建新的 UploadPretreatment
     */
    @Deprecated
    public UploadPretreatment(org.dromara.x.file.storage.core.UploadPretreatment pre) {
        setFileStorageService(pre.getFileStorageService());
        setPlatform(pre.getPlatform());
        setFileWrapper(pre.getFileWrapper());
        setThumbnailBytes(pre.getThumbnailBytes());
        setThumbnailSuffix(pre.getThumbnailSuffix());
        setObjectId(pre.getObjectId());
        setObjectType(pre.getObjectType());
        setPath(pre.getPath());
        setSaveFilename(pre.getSaveFilename());
        setSaveThFilename(pre.getSaveThFilename());
        setThContentType(pre.getThContentType());
        setMetadata(pre.getMetadata());
        setUserMetadata(pre.getUserMetadata());
        setThMetadata(pre.getThMetadata());
        setThUserMetadata(pre.getThUserMetadata());
        setNotSupportMetadataThrowException(pre.getNotSupportMetadataThrowException());
        setNotSupportAclThrowException(pre.getNotSupportAclThrowException());
        setAttr(pre.getAttr());
        setProgressListener(pre.getProgressListener());
        setInputStreamPlus(pre.getInputStreamPlusDirect());
        setFileAcl(pre.getFileAcl());
        setThFileAcl(pre.getThFileAcl());
        setHashCalculatorManager(pre.getHashCalculatorManager());
    }

    /**
     * 设置进度监听器，请使用 setProgressListener 代替
     *
     * @param progressMonitor 提供一个参数，表示已传输字节数
     */
    @Override
    public UploadPretreatment setProgressMonitor(boolean flag, Consumer<Long> progressMonitor) {
        return (UploadPretreatment) super.setProgressMonitor(flag, progressMonitor);
    }

    /**
     * 设置进度监听器，请使用 setProgressListener 代替
     *
     * @param progressMonitor 提供一个参数，表示已传输字节数
     */
    @Override
    public UploadPretreatment setProgressMonitor(Consumer<Long> progressMonitor) {
        return (UploadPretreatment) super.setProgressMonitor(progressMonitor);
    }

    /**
     * 设置进度监听器，请使用 setProgressListener 代替
     *
     * @param progressMonitor 提供两个参数，第一个是 progressSize已传输字节数，第二个是 allSize总字节数
     */
    @Override
    public UploadPretreatment setProgressMonitor(boolean flag, BiConsumer<Long, Long> progressMonitor) {
        return (UploadPretreatment) super.setProgressMonitor(flag, progressMonitor);
    }

    /**
     * 设置进度监听器，请使用 setProgressListener 代替
     *
     * @param progressMonitor 提供两个参数，第一个是 progressSize已传输字节数，第二个是 allSize总字节数
     */
    @Override
    public UploadPretreatment setProgressMonitor(BiConsumer<Long, Long> progressMonitor) {
        return (UploadPretreatment) super.setProgressMonitor(progressMonitor);
    }

    /**
     * 设置进度监听器，请使用 setProgressListener 代替
     */
    @Override
    public UploadPretreatment setProgressMonitor(boolean flag, ProgressListener progressMonitor) {
        return (UploadPretreatment) super.setProgressMonitor(flag, progressMonitor);
    }

    /**
     * 设置进度监听器，请使用 setProgressListener 代替
     */
    @Override
    public UploadPretreatment setProgressMonitor(ProgressListener progressMonitor) {
        return (UploadPretreatment) super.setProgressMonitor(progressMonitor);
    }

    /**
     * 设置进度监听器
     *
     * @param progressListener 提供一个参数，表示已传输字节数
     */
    @Override
    public UploadPretreatment setProgressListener(boolean flag, Consumer<Long> progressListener) {
        return (UploadPretreatment) super.setProgressListener(flag, progressListener);
    }

    /**
     * 设置进度监听器
     *
     * @param progressListener 提供一个参数，表示已传输字节数
     */
    @Override
    public UploadPretreatment setProgressListener(Consumer<Long> progressListener) {
        return (UploadPretreatment) super.setProgressListener(progressListener);
    }

    /**
     * 设置进度监听器
     *
     * @param progressListener 提供两个参数，第一个是 progressSize已传输字节数，第二个是 allSize总字节数
     */
    @Override
    public UploadPretreatment setProgressListener(boolean flag, BiConsumer<Long, Long> progressListener) {
        return (UploadPretreatment) super.setProgressListener(flag, progressListener);
    }

    /**
     * 设置进度监听器
     *
     * @param progressListener 提供两个参数，第一个是 progressSize已传输字节数，第二个是 allSize总字节数
     */
    @Override
    public UploadPretreatment setProgressListener(BiConsumer<Long, Long> progressListener) {
        return (UploadPretreatment) super.setProgressListener(progressListener);
    }

    /**
     * 设置进度监听器
     */
    @Override
    public UploadPretreatment setProgressListener(boolean flag, ProgressListener progressListener) {
        return (UploadPretreatment) super.setProgressListener(flag, progressListener);
    }

    /**
     * 设置 FileStorageService
     */
    @Override
    public UploadPretreatment setFileStorageService(FileStorageService fileStorageService) {
        return (UploadPretreatment) super.setFileStorageService(fileStorageService);
    }

    /**
     * 要上传到的平台
     */
    @Override
    public UploadPretreatment setPlatform(String platform) {
        return (UploadPretreatment) super.setPlatform(platform);
    }

    /**
     * 要上传的文件包装类
     */
    @Override
    public UploadPretreatment setFileWrapper(FileWrapper fileWrapper) {
        return (UploadPretreatment) super.setFileWrapper(fileWrapper);
    }

    /**
     * 要上传文件的缩略图
     */
    @Override
    public UploadPretreatment setThumbnailBytes(byte[] thumbnailBytes) {
        return (UploadPretreatment) super.setThumbnailBytes(thumbnailBytes);
    }

    /**
     * 缩略图后缀，不是扩展名但包含扩展名，例如【.min.jpg】【.png】。
     * 只能在缩略图生成前进行修改后缀中的扩展名部分。
     * 例如当前是【.min.jpg】那么扩展名就是【jpg】，当缩略图未生成的情况下可以随意修改（扩展名必须是 thumbnailator 支持的图片格式），
     * 一旦缩略图生成后，扩展名之外的部分可以随意改变 ，扩展名部分不能改变，除非你在 {@link org.dromara.x.file.storage.core.upload.UploadPretreatment#thumbnail} 方法中修改了输出格式。
     */
    @Override
    public UploadPretreatment setThumbnailSuffix(String thumbnailSuffix) {
        return (UploadPretreatment) super.setThumbnailSuffix(thumbnailSuffix);
    }

    /**
     * 文件所属对象类型
     */
    @Override
    public UploadPretreatment setObjectType(String objectType) {
        return (UploadPretreatment) super.setObjectType(objectType);
    }

    /**
     * 文件存储路径
     */
    @Override
    public UploadPretreatment setPath(String path) {
        return (UploadPretreatment) super.setPath(path);
    }

    /**
     * 保存文件名，如果不设置则自动生成
     */
    @Override
    public UploadPretreatment setSaveFilename(String saveFilename) {
        return (UploadPretreatment) super.setSaveFilename(saveFilename);
    }

    /**
     * 缩略图的保存文件名，注意此文件名不含后缀，后缀用 {@link org.dromara.x.file.storage.core.upload.UploadPretreatment#thumbnailSuffix} 属性控制
     */
    @Override
    public UploadPretreatment setSaveThFilename(String saveThFilename) {
        return (UploadPretreatment) super.setSaveThFilename(saveThFilename);
    }

    /**
     * 缩略图 MIME 类型，如果不设置则在上传文件根据缩略图文件名自动识别
     */
    @Override
    public UploadPretreatment setThContentType(String thContentType) {
        return (UploadPretreatment) super.setThContentType(thContentType);
    }

    /**
     * 文件元数据
     */
    @Override
    public UploadPretreatment setMetadata(Map<String, String> metadata) {
        return (UploadPretreatment) super.setMetadata(metadata);
    }

    /**
     * 文件用户元数据
     */
    @Override
    public UploadPretreatment setUserMetadata(Map<String, String> userMetadata) {
        return (UploadPretreatment) super.setUserMetadata(userMetadata);
    }

    /**
     * 缩略图元数据
     */
    @Override
    public UploadPretreatment setThMetadata(Map<String, String> thMetadata) {
        return (UploadPretreatment) super.setThMetadata(thMetadata);
    }

    /**
     * 缩略图用户元数据
     */
    @Override
    public UploadPretreatment setThUserMetadata(Map<String, String> thUserMetadata) {
        return (UploadPretreatment) super.setThUserMetadata(thUserMetadata);
    }

    /**
     * 不支持元数据时抛出异常
     */
    @Override
    public UploadPretreatment setNotSupportMetadataThrowException(Boolean notSupportMetadataThrowException) {
        return (UploadPretreatment) super.setNotSupportMetadataThrowException(notSupportMetadataThrowException);
    }

    /**
     * 不支持 ACL 时抛出异常
     */
    @Override
    public UploadPretreatment setNotSupportAclThrowException(Boolean notSupportAclThrowException) {
        return (UploadPretreatment) super.setNotSupportAclThrowException(notSupportAclThrowException);
    }

    /**
     * 附加属性字典
     */
    @Override
    public UploadPretreatment setAttr(Dict attr) {
        return (UploadPretreatment) super.setAttr(attr);
    }

    /**
     * 上传进度监听
     */
    @Override
    public UploadPretreatment setProgressListener(ProgressListener progressListener) {
        return (UploadPretreatment) super.setProgressListener(progressListener);
    }

    /**
     * 传时用的增强版本的 InputStream ，可以带进度监听、计算哈希等功能，仅内部使用
     */
    @Override
    public UploadPretreatment setInputStreamPlus(InputStreamPlus inputStreamPlus) {
        return (UploadPretreatment) super.setInputStreamPlus(inputStreamPlus);
    }

    /**
     * 文件的访问控制列表，一般情况下只有对象存储支持该功能
     * 详情见{@link FileInfo#setFileAcl}
     */
    @Override
    public UploadPretreatment setFileAcl(Object fileAcl) {
        return (UploadPretreatment) super.setFileAcl(fileAcl);
    }

    /**
     * 缩略图的访问控制列表，一般情况下只有对象存储支持该功能
     * 详情见{@link FileInfo#setFileAcl}
     */
    @Override
    public UploadPretreatment setThFileAcl(Object thFileAcl) {
        return (UploadPretreatment) super.setThFileAcl(thFileAcl);
    }

    /**
     * 哈希计算器管理器
     */
    @Override
    public UploadPretreatment setHashCalculatorManager(HashCalculatorManager hashCalculatorManager) {
        return (UploadPretreatment) super.setHashCalculatorManager(hashCalculatorManager);
    }

    /**
     * 设置要上传到的平台
     */
    @Override
    public UploadPretreatment setPlatform(boolean flag, String platform) {
        return (UploadPretreatment) super.setPlatform(flag, platform);
    }

    /**
     * 设置要上传的文件包装类
     */
    @Override
    public UploadPretreatment setFileWrapper(boolean flag, FileWrapper fileWrapper) {
        return (UploadPretreatment) super.setFileWrapper(flag, fileWrapper);
    }

    /**
     * 设置要上传文件的缩略图
     */
    @Override
    public UploadPretreatment setThumbnailBytes(boolean flag, byte[] thumbnailBytes) {
        return (UploadPretreatment) super.setThumbnailBytes(flag, thumbnailBytes);
    }

    /**
     * 设置缩略图后缀，不是扩展名但包含扩展名，例如【.min.jpg】【.png】。
     * 只能在缩略图生成前进行修改后缀中的扩展名部分。
     * 例如当前是【.min.jpg】那么扩展名就是【jpg】，当缩略图未生成的情况下可以随意修改（扩展名必须是 thumbnailator 支持的图片格式），
     * 一旦缩略图生成后，扩展名之外的部分可以随意改变 ，扩展名部分不能改变，除非你在 {@link org.dromara.x.file.storage.core.upload.UploadPretreatment#thumbnail} 方法中修改了输出格式。
     */
    @Override
    public UploadPretreatment setThumbnailSuffix(boolean flag, String thumbnailSuffix) {
        return (UploadPretreatment) super.setThumbnailSuffix(flag, thumbnailSuffix);
    }

    /**
     * 设置文件所属对象id
     *
     * @param objectId 如果不是 String 类型会自动调用 toString() 方法
     */
    @Override
    public UploadPretreatment setObjectId(boolean flag, Object objectId) {
        return (UploadPretreatment) super.setObjectId(flag, objectId);
    }

    /**
     * 设置文件所属对象id
     *
     * @param objectId 如果不是 String 类型会自动调用 toString() 方法
     */
    @Override
    public UploadPretreatment setObjectId(Object objectId) {
        return (UploadPretreatment) super.setObjectId(objectId);
    }

    /**
     * 设置文件所属对象类型
     */
    @Override
    public UploadPretreatment setObjectType(boolean flag, String objectType) {
        return (UploadPretreatment) super.setObjectType(flag, objectType);
    }

    /**
     * 设置文文件存储路径
     */
    @Override
    public UploadPretreatment setPath(boolean flag, String path) {
        return (UploadPretreatment) super.setPath(flag, path);
    }

    /**
     * 设置保存文件名，如果不设置则自动生成
     */
    @Override
    public UploadPretreatment setSaveFilename(boolean flag, String saveFilename) {
        return (UploadPretreatment) super.setSaveFilename(flag, saveFilename);
    }

    /**
     * 设置缩略图的保存文件名，注意此文件名不含后缀，后缀用 {@link org.dromara.x.file.storage.core.upload.UploadPretreatment#thumbnailSuffix} 属性控制
     */
    @Override
    public UploadPretreatment setSaveThFilename(boolean flag, String saveThFilename) {
        return (UploadPretreatment) super.setSaveThFilename(flag, saveThFilename);
    }

    /**
     * 缩略图 MIME 类型，如果不设置则在上传文件根据缩略图文件名自动识别
     */
    @Override
    public UploadPretreatment setThContentType(boolean flag, String thContentType) {
        return (UploadPretreatment) super.setThContentType(flag, thContentType);
    }

    /**
     * 设置文件名
     */
    @Override
    public UploadPretreatment setName(boolean flag, String name) {
        return (UploadPretreatment) super.setName(flag, name);
    }

    /**
     * 设置文件名
     */
    @Override
    public UploadPretreatment setName(String name) {
        return (UploadPretreatment) super.setName(name);
    }

    /**
     * 设置文件的 MIME 类型
     */
    @Override
    public UploadPretreatment setContentType(boolean flag, String contentType) {
        return (UploadPretreatment) super.setContentType(flag, contentType);
    }

    /**
     * 设置文件的 MIME 类型
     */
    @Override
    public UploadPretreatment setContentType(String contentType) {
        return (UploadPretreatment) super.setContentType(contentType);
    }

    /**
     * 设置原始文件名
     */
    @Override
    public UploadPretreatment setOriginalFilename(boolean flag, String originalFilename) {
        return (UploadPretreatment) super.setOriginalFilename(flag, originalFilename);
    }

    /**
     * 设置原始文件名
     */
    @Override
    public UploadPretreatment setOriginalFilename(String originalFilename) {
        return (UploadPretreatment) super.setOriginalFilename(originalFilename);
    }

    /**
     * 设置文件元数据
     */
    @Override
    public UploadPretreatment putMetadata(boolean flag, String key, String value) {
        return (UploadPretreatment) super.putMetadata(flag, key, value);
    }

    /**
     * 设置文件元数据
     */
    @Override
    public UploadPretreatment putMetadata(String key, String value) {
        return (UploadPretreatment) super.putMetadata(key, value);
    }

    /**
     * 设置文件元数据
     */
    @Override
    public UploadPretreatment putMetadataAll(boolean flag, Map<String, String> metadata) {
        return (UploadPretreatment) super.putMetadataAll(flag, metadata);
    }

    /**
     * 设置文件元数据
     */
    @Override
    public UploadPretreatment putMetadataAll(Map<String, String> metadata) {
        return (UploadPretreatment) super.putMetadataAll(metadata);
    }

    /**
     * 设置文件用户元数据
     */
    @Override
    public UploadPretreatment putUserMetadata(boolean flag, String key, String value) {
        return (UploadPretreatment) super.putUserMetadata(flag, key, value);
    }

    /**
     * 设置文件用户元数据
     */
    @Override
    public UploadPretreatment putUserMetadata(String key, String value) {
        return (UploadPretreatment) super.putUserMetadata(key, value);
    }

    /**
     * 设置文件用户元数据
     */
    @Override
    public UploadPretreatment putUserMetadataAll(boolean flag, Map<String, String> metadata) {
        return (UploadPretreatment) super.putUserMetadataAll(flag, metadata);
    }

    /**
     * 设置文件用户元数据
     */
    @Override
    public UploadPretreatment putUserMetadataAll(Map<String, String> metadata) {
        return (UploadPretreatment) super.putUserMetadataAll(metadata);
    }

    /**
     * 设置缩略图元数据
     */
    @Override
    public UploadPretreatment putThMetadata(boolean flag, String key, String value) {
        return (UploadPretreatment) super.putThMetadata(flag, key, value);
    }

    /**
     * 设置缩略图元数据
     */
    @Override
    public UploadPretreatment putThMetadata(String key, String value) {
        return (UploadPretreatment) super.putThMetadata(key, value);
    }

    /**
     * 设置缩略图元数据
     */
    @Override
    public UploadPretreatment putThMetadataAll(boolean flag, Map<String, String> metadata) {
        return (UploadPretreatment) super.putThMetadataAll(flag, metadata);
    }

    /**
     * 设置缩略图元数据
     */
    @Override
    public UploadPretreatment putThMetadataAll(Map<String, String> metadata) {
        return (UploadPretreatment) super.putThMetadataAll(metadata);
    }

    /**
     * 设置缩略图用户元数据
     */
    @Override
    public UploadPretreatment putThUserMetadata(boolean flag, String key, String value) {
        return (UploadPretreatment) super.putThUserMetadata(flag, key, value);
    }

    /**
     * 设置缩略图用户元数据
     */
    @Override
    public UploadPretreatment putThUserMetadata(String key, String value) {
        return (UploadPretreatment) super.putThUserMetadata(key, value);
    }

    /**
     * 设置缩略图用户元数据
     */
    @Override
    public UploadPretreatment putThUserMetadataAll(boolean flag, Map<String, String> metadata) {
        return (UploadPretreatment) super.putThUserMetadataAll(flag, metadata);
    }

    /**
     * 设置缩略图用户元数据
     */
    @Override
    public UploadPretreatment putThUserMetadataAll(Map<String, String> metadata) {
        return (UploadPretreatment) super.putThUserMetadataAll(metadata);
    }

    /**
     * 设置不支持元数据时抛出异常
     */
    @Override
    public UploadPretreatment setNotSupportMetadataThrowException(
            boolean flag, Boolean notSupportMetadataThrowException) {
        return (UploadPretreatment) super.setNotSupportMetadataThrowException(flag, notSupportMetadataThrowException);
    }

    /**
     * 设置不支持 ACL 时抛出异常
     */
    @Override
    public UploadPretreatment setNotSupportAclThrowException(boolean flag, Boolean notSupportAclThrowException) {
        return (UploadPretreatment) super.setNotSupportAclThrowException(flag, notSupportAclThrowException);
    }

    /**
     * 设置附加属性
     */
    @Override
    public UploadPretreatment putAttr(boolean flag, String key, Object value) {
        return (UploadPretreatment) super.putAttr(flag, key, value);
    }

    /**
     * 设置附加属性
     */
    @Override
    public UploadPretreatment putAttr(String key, Object value) {
        return (UploadPretreatment) super.putAttr(key, value);
    }

    /**
     * 设置附加属性
     */
    @Override
    public UploadPretreatment putAttrAll(boolean flag, Map<String, Object> attr) {
        return (UploadPretreatment) super.putAttrAll(flag, attr);
    }

    /**
     * 设置附加属性
     */
    @Override
    public UploadPretreatment putAttrAll(Map<String, Object> attr) {
        return (UploadPretreatment) super.putAttrAll(attr);
    }

    /**
     * 进行图片处理，可以进行裁剪、旋转、缩放、水印等操作
     */
    @Override
    public UploadPretreatment image(boolean flag, Consumer<Thumbnails.Builder<? extends InputStream>> consumer) {
        return (UploadPretreatment) super.image(flag, consumer);
    }

    /**
     * 进行图片处理，可以进行裁剪、旋转、缩放、水印等操作
     */
    @Override
    public UploadPretreatment image(Consumer<Thumbnails.Builder<? extends InputStream>> consumer) {
        return (UploadPretreatment) super.image(consumer);
    }

    /**
     * 缩放到指定大小
     */
    @Override
    public UploadPretreatment image(boolean flag, int width, int height) {
        return (UploadPretreatment) super.image(flag, width, height);
    }

    /**
     * 缩放到指定大小
     */
    @Override
    public UploadPretreatment image(int width, int height) {
        return (UploadPretreatment) super.image(width, height);
    }

    /**
     * 缩放到 200*200 大小
     */
    @Override
    public UploadPretreatment image(boolean flag) {
        return (UploadPretreatment) super.image(flag);
    }

    /**
     * 缩放到 200*200 大小
     */
    @Override
    public UploadPretreatment image() {
        return (UploadPretreatment) super.image();
    }

    /**
     * 清空缩略图
     */
    @Override
    public UploadPretreatment clearThumbnail(boolean flag) {
        return (UploadPretreatment) super.clearThumbnail(flag);
    }

    /**
     * 清空缩略图
     */
    @Override
    public UploadPretreatment clearThumbnail() {
        return (UploadPretreatment) super.clearThumbnail();
    }

    /**
     * 通过指定 file 生成缩略图，
     * 如果 file 是 InputStream、FileWrapper 等可以自动关闭的对象，操作完成后会自动关闭
     */
    @Override
    public UploadPretreatment thumbnailOf(boolean flag, Object file) {
        return (UploadPretreatment) super.thumbnailOf(flag, file);
    }

    /**
     * 通过指定 file 生成缩略图，
     * 如果 file 是 InputStream、FileWrapper 等可以自动关闭的对象，操作完成后会自动关闭
     */
    @Override
    public UploadPretreatment thumbnailOf(Object file) {
        return (UploadPretreatment) super.thumbnailOf(file);
    }

    /**
     * 通过指定 file 生成缩略图并进行图片处理，
     * 可以进行裁剪、旋转、缩放、水印等操作，默认输出图片格式通过 thumbnailSuffix 获取，
     * 如果 file 是 InputStream、FileWrapper 等可以自动关闭的对象，操作完成后会自动关闭
     */
    @Override
    public UploadPretreatment thumbnailOf(
            boolean flag, Object file, Consumer<Thumbnails.Builder<? extends InputStream>> consumer) {
        return (UploadPretreatment) super.thumbnailOf(flag, file, consumer);
    }

    /**
     * 通过指定 file 生成缩略图并进行图片处理，
     * 可以进行裁剪、旋转、缩放、水印等操作，默认输出图片格式通过 thumbnailSuffix 获取，
     * 如果 file 是 InputStream、FileWrapper 等可以自动关闭的对象，操作完成后会自动关闭
     */
    @Override
    public UploadPretreatment thumbnailOf(Object file, Consumer<Thumbnails.Builder<? extends InputStream>> consumer) {
        return (UploadPretreatment) super.thumbnailOf(file, consumer);
    }

    /**
     * 生成缩略图并进行图片处理，如果缩略图已存在则使用已有的缩略图进行处理，
     * 可以进行裁剪、旋转、缩放、水印等操作，默认输出图片格式通过 thumbnailSuffix 获取
     */
    @Override
    public UploadPretreatment thumbnail(boolean flag, Consumer<Thumbnails.Builder<? extends InputStream>> consumer) {
        return (UploadPretreatment) super.thumbnail(flag, consumer);
    }

    /**
     * 生成缩略图并进行图片处理，如果缩略图已存在则使用已有的缩略图进行处理，
     * 可以进行裁剪、旋转、缩放、水印等操作，默认输出图片格式通过 thumbnailSuffix 获取
     */
    @Override
    public UploadPretreatment thumbnail(Consumer<Thumbnails.Builder<? extends InputStream>> consumer) {
        return (UploadPretreatment) super.thumbnail(consumer);
    }

    /**
     * 生成缩略图并缩放到指定大小，默认输出图片格式通过 thumbnailSuffix 获取
     */
    @Override
    public UploadPretreatment thumbnail(boolean flag, int width, int height) {
        return (UploadPretreatment) super.thumbnail(flag, width, height);
    }

    /**
     * 生成缩略图并缩放到指定大小，默认输出图片格式通过 thumbnailSuffix 获取
     */
    @Override
    public UploadPretreatment thumbnail(int width, int height) {
        return (UploadPretreatment) super.thumbnail(width, height);
    }

    /**
     * 生成缩略图并缩放到 200*200 大小，默认输出图片格式通过 thumbnailSuffix 获取
     */
    @Override
    public UploadPretreatment thumbnail(boolean flag) {
        return (UploadPretreatment) super.thumbnail(flag);
    }

    /**
     * 生成缩略图并缩放到 200*200 大小，默认输出图片格式通过 thumbnailSuffix 获取
     */
    @Override
    public UploadPretreatment thumbnail() {
        return (UploadPretreatment) super.thumbnail();
    }

    /**
     * 设置文件的访问控制列表，一般情况下只有对象存储支持该功能
     */
    @Override
    public UploadPretreatment setFileAcl(boolean flag, Object acl) {
        return (UploadPretreatment) super.setFileAcl(flag, acl);
    }

    /**
     * 设置文件的访问控制列表，一般情况下只有对象存储支持该功能
     */
    @Override
    public UploadPretreatment setThFileAcl(boolean flag, Object acl) {
        return (UploadPretreatment) super.setThFileAcl(flag, acl);
    }

    /**
     * 同时设置 fileAcl 和 thFileAcl 两个属性
     * 详情见{@link FileInfo#setFileAcl}
     */
    @Override
    public UploadPretreatment setAcl(boolean flag, Object acl) {
        return (UploadPretreatment) super.setAcl(flag, acl);
    }

    /**
     * 同时设置 fileAcl 和 thFileAcl 两个属性
     * 详情见{@link FileInfo#setFileAcl}
     */
    @Override
    public UploadPretreatment setAcl(Object acl) {
        return (UploadPretreatment) super.setAcl(acl);
    }

    /**
     * 添加一个哈希计算器
     * @param hashCalculator 哈希计算器
     */
    @Override
    public UploadPretreatment setHashCalculator(HashCalculator hashCalculator) {
        return (UploadPretreatment) super.setHashCalculator(hashCalculator);
    }

    /**
     * 设置哈希计算器管理器（如果条件为 true）
     * @param flag 条件
     * @param hashCalculatorManager 哈希计算器管理器
     */
    @Override
    public UploadPretreatment setHashCalculatorManager(boolean flag, HashCalculatorManager hashCalculatorManager) {
        return (UploadPretreatment) super.setHashCalculatorManager(flag, hashCalculatorManager);
    }

    /**
     * 添加 MD2 哈希计算器（如果条件为 true）
     * @param flag 条件
     */
    @Override
    public UploadPretreatment setHashCalculatorMd2(boolean flag) {
        return (UploadPretreatment) super.setHashCalculatorMd2(flag);
    }

    /**
     * 添加 MD2 哈希计算器
     */
    @Override
    public UploadPretreatment setHashCalculatorMd2() {
        return (UploadPretreatment) super.setHashCalculatorMd2();
    }

    /**
     * 添加 MD5 哈希计算器（如果条件为 true）
     * @param flag 条件
     */
    @Override
    public UploadPretreatment setHashCalculatorMd5(boolean flag) {
        return (UploadPretreatment) super.setHashCalculatorMd5(flag);
    }

    /**
     * 添加 MD5 哈希计算器
     */
    @Override
    public UploadPretreatment setHashCalculatorMd5() {
        return (UploadPretreatment) super.setHashCalculatorMd5();
    }

    /**
     * 添加 SHA1 哈希计算器（如果条件为 true）
     * @param flag 条件
     */
    @Override
    public UploadPretreatment setHashCalculatorSha1(boolean flag) {
        return (UploadPretreatment) super.setHashCalculatorSha1(flag);
    }

    /**
     * 添加 SHA1 哈希计算器
     */
    @Override
    public UploadPretreatment setHashCalculatorSha1() {
        return (UploadPretreatment) super.setHashCalculatorSha1();
    }

    /**
     * 添加 SHA256 哈希计算器（如果条件为 true）
     * @param flag 条件
     */
    @Override
    public UploadPretreatment setHashCalculatorSha256(boolean flag) {
        return (UploadPretreatment) super.setHashCalculatorSha256(flag);
    }

    /**
     * 添加 SHA256 哈希计算器
     */
    @Override
    public UploadPretreatment setHashCalculatorSha256() {
        return (UploadPretreatment) super.setHashCalculatorSha256();
    }

    /**
     * 添加 SHA384 哈希计算器（如果条件为 true）
     * @param flag 条件
     */
    @Override
    public UploadPretreatment setHashCalculatorSha384(boolean flag) {
        return (UploadPretreatment) super.setHashCalculatorSha384(flag);
    }

    /**
     * 添加 SHA384 哈希计算器
     */
    @Override
    public UploadPretreatment setHashCalculatorSha384() {
        return (UploadPretreatment) super.setHashCalculatorSha384();
    }

    /**
     * 添加 SHA512 哈希计算器（如果条件为 true）
     * @param flag 条件
     */
    @Override
    public UploadPretreatment setHashCalculatorSha512(boolean flag) {
        return (UploadPretreatment) super.setHashCalculatorSha512(flag);
    }

    /**
     * 添加 SHA512 哈希计算器
     */
    @Override
    public UploadPretreatment setHashCalculatorSha512() {
        return (UploadPretreatment) super.setHashCalculatorSha512();
    }

    /**
     * 添加哈希计算器（如果条件为 true）
     * @param flag 条件
     * @param name 哈希名称，例如 MD5、SHA1、SHA256等，详情{@link org.dromara.x.file.storage.core.constant.Constant.Hash.MessageDigest}
     */
    @Override
    public UploadPretreatment setHashCalculator(boolean flag, String name) {
        return (UploadPretreatment) super.setHashCalculator(flag, name);
    }

    /**
     * 添加哈希计算器
     * @param name 哈希名称，例如 MD5、SHA1、SHA256等，详情{@link org.dromara.x.file.storage.core.constant.Constant.Hash.MessageDigest}
     */
    @Override
    public UploadPretreatment setHashCalculator(String name) {
        return (UploadPretreatment) super.setHashCalculator(name);
    }

    /**
     * 添加哈希计算器
     * @param flag 条件
     * @param messageDigest 消息摘要算法
     */
    @Override
    public UploadPretreatment setHashCalculator(boolean flag, MessageDigest messageDigest) {
        return (UploadPretreatment) super.setHashCalculator(flag, messageDigest);
    }

    /**
     * 添加哈希计算器
     * @param messageDigest 消息摘要算法
     */
    @Override
    public UploadPretreatment setHashCalculator(MessageDigest messageDigest) {
        return (UploadPretreatment) super.setHashCalculator(messageDigest);
    }

    /**
     * 添加哈希计算器（如果条件为 true）
     * @param flag 条件
     * @param hashCalculator 哈希计算器
     */
    @Override
    public UploadPretreatment setHashCalculator(boolean flag, HashCalculator hashCalculator) {
        return (UploadPretreatment) super.setHashCalculator(flag, hashCalculator);
    }

    /**
     * 上传文件，成功返回文件信息，失败返回null
     */
    @Override
    public FileInfo upload() {
        return new UploadActuator(this).execute();
    }

    /**
     * 上传文件，成功返回文件信息，失败返回null。此方法仅限内部使用
     */
    @Override
    public FileInfo upload(FileStorage fileStorage, FileRecorder fileRecorder, List<FileStorageAspect> aspectList) {
        return new UploadActuator(this).execute(fileStorage, fileRecorder, aspectList);
    }
}
