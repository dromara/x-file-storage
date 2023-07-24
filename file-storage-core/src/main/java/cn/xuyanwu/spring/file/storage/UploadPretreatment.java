package cn.xuyanwu.spring.file.storage;

import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.lang.Dict;
import cn.xuyanwu.spring.file.storage.exception.FileStorageRuntimeException;
import cn.xuyanwu.spring.file.storage.file.ByteFileWrapperAdapter;
import cn.xuyanwu.spring.file.storage.file.FileWrapper;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.coobird.thumbnailator.Thumbnails;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 文件上传预处理对象
 */
@Getter
@Setter
@Accessors(chain = true)
public class UploadPretreatment {
    private FileStorageService fileStorageService;
    private ByteFileWrapperAdapter byteFileWrapperAdapter;
    /**
     * 要上传到的平台
     */
    private String platform;
    /**
     * 要上传的文件包装类
     */
    private FileWrapper fileWrapper;
    /**
     * 要上传文件的缩略图
     */
    private byte[] thumbnailBytes;
    /**
     * 缩略图后缀，不是扩展名但包含扩展名，例如【.min.jpg】【.png】。
     * 只能在缩略图生成前进行修改后缀中的扩展名部分。
     * 例如当前是【.min.jpg】那么扩展名就是【jpg】，当缩略图未生成的情况下可以随意修改（扩展名必须是 thumbnailator 支持的图片格式），
     * 一旦缩略图生成后，扩展名之外的部分可以随意改变 ，扩展名部分不能改变，除非你在 {@link UploadPretreatment#thumbnail} 方法中修改了输出格式。
     */
    private String thumbnailSuffix;
    /**
     * 文件所属对象id
     */
    private String objectId;
    /**
     * 文件所属对象类型
     */
    private String objectType;
    /**
     * 文件存储路径
     */
    private String path = "";

    /**
     * 保存文件名，如果不设置则自动生成
     */
    private String saveFilename;

    /**
     * 缩略图的保存文件名，注意此文件名不含后缀，后缀用 {@link UploadPretreatment#thumbnailSuffix} 属性控制
     */
    private String saveThFilename;

    /**
     * 缩略图 MIME 类型，如果不设置则在上传文件根据缩略图文件名自动识别
     */
    private String thContentType;

    /**
     * 附加属性字典
     */
    private Dict attr;

    /**
     * 上传进度监听
     */
    private ProgressListener progressListener;

    /**
     * 是否为分片上传
     */
    private boolean isPart = false;

    /**
     * 分片号
     */
    private Integer partNum;

    /**
     * 文件的访问控制列表，一般情况下只有对象存储支持该功能
     * 详情见{@link cn.xuyanwu.spring.file.storage.FileInfo#setFileAcl}
     */
    private Object fileAcl;

    /**
     * 缩略图的访问控制列表，一般情况下只有对象存储支持该功能
     * 详情见{@link cn.xuyanwu.spring.file.storage.FileInfo#setFileAcl}
     */
    private Object thFileAcl;


    /**
     * 设置要上传到的平台
     */
    public UploadPretreatment setPlatform(boolean flag,String platform) {
        if (flag) setPlatform(platform);
        return this;
    }

    /**
     * 设置要上传的文件包装类
     */
    public UploadPretreatment setFileWrapper(boolean flag,FileWrapper fileWrapper) {
        if (flag) setFileWrapper(fileWrapper);
        return this;
    }

    /**
     * 设置要上传文件的缩略图
     */
    public UploadPretreatment setThumbnailBytes(boolean flag,byte[] thumbnailBytes) {
        if (flag) setThumbnailBytes(thumbnailBytes);
        return this;
    }

    /**
     * 设置缩略图后缀，不是扩展名但包含扩展名，例如【.min.jpg】【.png】。
     * 只能在缩略图生成前进行修改后缀中的扩展名部分。
     * 例如当前是【.min.jpg】那么扩展名就是【jpg】，当缩略图未生成的情况下可以随意修改（扩展名必须是 thumbnailator 支持的图片格式），
     * 一旦缩略图生成后，扩展名之外的部分可以随意改变 ，扩展名部分不能改变，除非你在 {@link UploadPretreatment#thumbnail} 方法中修改了输出格式。
     */
    public UploadPretreatment setThumbnailSuffix(boolean flag,String thumbnailSuffix) {
        if (flag) setThumbnailSuffix(thumbnailSuffix);
        return this;
    }


    /**
     * 设置文件所属对象id
     *
     * @param objectId 如果不是 String 类型会自动调用 toString() 方法
     */
    public UploadPretreatment setObjectId(boolean flag,Object objectId) {
        if (flag) setObjectId(objectId);
        return this;
    }

    /**
     * 设置文件所属对象id
     *
     * @param objectId 如果不是 String 类型会自动调用 toString() 方法
     */
    public UploadPretreatment setObjectId(Object objectId) {
        this.objectId = objectId == null ? null : objectId.toString();
        return this;
    }

    /**
     * 设置文件所属对象类型
     */
    public UploadPretreatment setObjectType(boolean flag,String objectType) {
        if (flag) setObjectType(objectType);
        return this;
    }

    /**
     * 设置文文件存储路径
     */
    public UploadPretreatment setPath(boolean flag,String path) {
        if (flag) setPath(path);
        return this;
    }

    /**
     * 设置保存文件名，如果不设置则自动生成
     */
    public UploadPretreatment setSaveFilename(boolean flag,String saveFilename) {
        if (flag) setSaveFilename(saveFilename);
        return this;
    }

    /**
     * 设置缩略图的保存文件名，注意此文件名不含后缀，后缀用 {@link UploadPretreatment#thumbnailSuffix} 属性控制
     */
    public UploadPretreatment setSaveThFilename(boolean flag,String saveThFilename) {
        if (flag) setSaveThFilename(saveFilename);
        return this;
    }

    /**
     * 缩略图 MIME 类型，如果不设置则在上传文件根据缩略图文件名自动识别
     */
    public UploadPretreatment setThContentType(boolean flag,String thContentType) {
        if (flag) setThContentType(thContentType);
        return this;
    }

    /**
     * 获取文件名
     */
    public String getName() {
        return fileWrapper.getName();
    }

    /**
     * 设置文件名
     */
    public UploadPretreatment setName(boolean flag,String name) {
        if (flag) setName(name);
        return this;
    }

    /**
     * 设置文件名
     */
    public UploadPretreatment setName(String name) {
        fileWrapper.setName(name);
        return this;
    }

    /**
     * 获取文件的 MIME 类型
     */
    public String getContentType() {
        return fileWrapper.getContentType();
    }

    /**
     * 设置文件的 MIME 类型
     */
    public UploadPretreatment setContentType(boolean flag,String contentType) {
        if (flag) setContentType(contentType);
        return this;
    }

    /**
     * 设置文件的 MIME 类型
     */
    public UploadPretreatment setContentType(String contentType) {
        fileWrapper.setContentType(contentType);
        return this;
    }

    /**
     * 获取原始文件名
     */
    public String getOriginalFilename() {
        return fileWrapper.getName();
    }

    /**
     * 设置原始文件名
     */
    public UploadPretreatment setOriginalFilename(boolean flag,String originalFilename) {
        if (flag) setOriginalFilename(originalFilename);
        return this;
    }

    /**
     * 设置原始文件名
     */
    public UploadPretreatment setOriginalFilename(String originalFilename) {
        fileWrapper.setName(originalFilename);
        return this;
    }

    /**
     * 获取附加属性字典
     */
    public Dict getAttr() {
        if (attr == null) attr = new Dict();
        return attr;
    }

    /**
     * 设置附加属性
     */
    public UploadPretreatment putAttr(boolean flag,String key,Object value) {
        if (flag) putAttr(key,value);
        return this;
    }

    /**
     * 设置附加属性
     */
    public UploadPretreatment putAttr(String key,Object value) {
        getAttr().put(key,value);
        return this;
    }

    /**
     * 进行图片处理，可以进行裁剪、旋转、缩放、水印等操作
     */
    public UploadPretreatment image(boolean flag,Consumer<Thumbnails.Builder<? extends InputStream>> consumer) {
        if (flag) image(consumer);
        return this;
    }

    /**
     * 进行图片处理，可以进行裁剪、旋转、缩放、水印等操作
     */
    public UploadPretreatment image(Consumer<Thumbnails.Builder<? extends InputStream>> consumer) {
        try (InputStream in = fileWrapper.getInputStream()) {
            Thumbnails.Builder<? extends InputStream> builder = Thumbnails.of(in);
            consumer.accept(builder);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            builder.toOutputStream(out);
            fileWrapper = byteFileWrapperAdapter.getFileWrapper(out.toByteArray(),fileWrapper.getName(),null,null);
            return this;
        } catch (IOException e) {
            throw new FileStorageRuntimeException("图片处理失败！",e);
        }
    }

    /**
     * 缩放到指定大小
     */
    public UploadPretreatment image(boolean flag,int width,int height) {
        if (flag) image(width,height);
        return this;
    }

    /**
     * 缩放到指定大小
     */
    public UploadPretreatment image(int width,int height) {
        return image(th -> th.size(width,height));
    }

    /**
     * 缩放到 200*200 大小
     */
    public UploadPretreatment image(boolean flag) {
        if (flag) image();
        return this;
    }

    /**
     * 缩放到 200*200 大小
     */
    public UploadPretreatment image() {
        return image(th -> th.size(200,200));
    }

    /**
     * 清空缩略图
     */
    public UploadPretreatment clearThumbnail(boolean flag) {
        if (flag) clearThumbnail();
        return this;
    }

    /**
     * 清空缩略图
     */
    public UploadPretreatment clearThumbnail() {
        thumbnailBytes = null;
        return this;
    }

    /**
     * 生成缩略图并进行图片处理，如果缩略图已存在则使用已有的缩略图进行处理，
     * 可以进行裁剪、旋转、缩放、水印等操作，默认输出图片格式通过 thumbnailSuffix 获取
     */
    public UploadPretreatment thumbnail(boolean flag,Consumer<Thumbnails.Builder<? extends InputStream>> consumer) {
        if (flag) thumbnail(consumer);
        return this;
    }

    /**
     * 生成缩略图并进行图片处理，如果缩略图已存在则使用已有的缩略图进行处理，
     * 可以进行裁剪、旋转、缩放、水印等操作，默认输出图片格式通过 thumbnailSuffix 获取
     */
    public UploadPretreatment thumbnail(Consumer<Thumbnails.Builder<? extends InputStream>> consumer) {
        try {
            if (thumbnailBytes == null) {
                return fileWrapper.getInputStreamMaskResetReturn(in -> thumbnail(consumer,in));
            } else {
                return thumbnail(consumer,new ByteArrayInputStream(thumbnailBytes));
            }
        } catch (IOException e) {
            throw new FileStorageRuntimeException("生成缩略图失败！",e);
        }
    }

    /**
     * 通过指定 InputStream 生成缩略图并进行图片处理，
     * 可以进行裁剪、旋转、缩放、水印等操作，默认输出图片格式通过 thumbnailSuffix 获取，
     * 操作完成后不会自动关闭 InputStream
     */
    private UploadPretreatment thumbnail(Consumer<Thumbnails.Builder<? extends InputStream>> consumer,InputStream in) {
        try {
            Thumbnails.Builder<? extends InputStream> builder = Thumbnails.of(in);
            builder.outputFormat(FileNameUtil.extName(thumbnailSuffix));
            consumer.accept(builder);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            builder.toOutputStream(out);
            thumbnailBytes = out.toByteArray();
            return this;
        } catch (IOException e) {
            throw new FileStorageRuntimeException("生成缩略图失败！",e);
        }
    }

    /**
     * 生成缩略图并缩放到指定大小，默认输出图片格式通过 thumbnailSuffix 获取
     */
    public UploadPretreatment thumbnail(boolean flag,int width,int height) {
        if (flag) thumbnail(width,height);
        return this;
    }

    /**
     * 生成缩略图并缩放到指定大小，默认输出图片格式通过 thumbnailSuffix 获取
     */
    public UploadPretreatment thumbnail(int width,int height) {
        return thumbnail(th -> th.size(width,height));
    }

    /**
     * 生成缩略图并缩放到 200*200 大小，默认输出图片格式通过 thumbnailSuffix 获取
     */
    public UploadPretreatment thumbnail(boolean flag) {
        if (flag) thumbnail();
        return this;
    }

    /**
     * 生成缩略图并缩放到 200*200 大小，默认输出图片格式通过 thumbnailSuffix 获取
     */
    public UploadPretreatment thumbnail() {
        return thumbnail(200,200);
    }

    /**
     * 转换为分片上传
     */
    public UploadPretreatment part(boolean flag,Integer partNum) {
        if (flag) part(partNum);
        return this;
    }

    /**
     * 转换为分片上传
     */
    public UploadPretreatment part(Integer partNum) {
        isPart = true;
        this.partNum = partNum;
        return this;
    }


    /**
     * 设置上传进度监听器
     *
     * @param progressListener 提供一个参数，表示已传输字节数
     */
    public UploadPretreatment setProgressMonitor(boolean flag,Consumer<Long> progressListener) {
        if (flag) setProgressMonitor(progressListener);
        return this;
    }

    /**
     * 设置上传进度监听器
     *
     * @param progressListener 提供一个参数，表示已传输字节数
     */
    public UploadPretreatment setProgressMonitor(Consumer<Long> progressListener) {
        return setProgressMonitor((progressSize,allSize) -> progressListener.accept(progressSize));
    }

    /**
     * 设置上传进度监听器
     *
     * @param progressListener 提供两个参数，第一个是 progressSize已传输字节数，第二个是 allSize总字节数
     */
    public UploadPretreatment setProgressMonitor(boolean flag,BiConsumer<Long,Long> progressListener) {
        if (flag) setProgressMonitor(progressListener);
        return this;
    }

    /**
     * 设置上传进度监听器
     *
     * @param progressListener 提供两个参数，第一个是 progressSize已传输字节数，第二个是 allSize总字节数
     */
    public UploadPretreatment setProgressMonitor(BiConsumer<Long,Long> progressListener) {
        return setProgressMonitor(new ProgressListener() {
            @Override
            public void start() {
            }

            @Override
            public void progress(long progressSize,long allSize) {
                progressListener.accept(progressSize,allSize);
            }

            @Override
            public void finish() {
            }
        });
    }

    /**
     * 设置上传进度监听器
     */
    public UploadPretreatment setProgressMonitor(boolean flag,ProgressListener progressListener) {
        if (flag) setProgressMonitor(progressListener);
        return this;
    }

    /**
     * 设置上传进度监听器
     */
    public UploadPretreatment setProgressMonitor(ProgressListener progressListener) {
        this.progressListener = progressListener;
        return this;
    }

    /**
     * 同时设置 fileAcl 和 thFileAcl 两个属性
     * 详情见{@link cn.xuyanwu.spring.file.storage.FileInfo#setFileAcl}
     */
    public UploadPretreatment setAcl(boolean flag,Object acl) {
        if (flag) setAcl(acl);
        return this;
    }

    /**
     * 同时设置 fileAcl 和 thFileAcl 两个属性
     * 详情见{@link cn.xuyanwu.spring.file.storage.FileInfo#setFileAcl}
     */
    public UploadPretreatment setAcl(Object acl) {
        this.fileAcl = acl;
        this.thFileAcl = acl;
        return this;
    }

    /**
     * 上传文件，成功返回文件信息，失败返回null
     */
    public FileInfo upload() {
        return fileStorageService.upload(this);
    }
}
