package cn.xuyanwu.spring.file.storage;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.lang.Dict;
import cn.xuyanwu.spring.file.storage.exception.FileStorageRuntimeException;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.function.Consumer;

/**
 * 文件上传预处理对象
 */
@Getter
@Setter
@Accessors(chain = true)
public class UploadPretreatment {
    private FileStorageService fileStorageService;
    /**
     * 要上传到的平台
     */
    private String platform;
    /**
     * 要上传的文件包装类
     */
    private MultipartFileWrapper fileWrapper;
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
     * MIME 类型，如果不设置则在上传文件根据 {@link MultipartFileWrapper#getContentType()} 和文件名自动识别
     */
    private String contentType;

    /**
     * 缩略图 MIME 类型，如果不设置则在上传文件根据缩略图文件名自动识别
     */
    private String thContentType;

    /**
     * 附加属性字典
     */
    private Dict attr;

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
     * 获取文件名
     */
    public String getName() {
        return fileWrapper.getName();
    }

    /**
     * 设置文件名
     */
    public UploadPretreatment setName(String name) {
        fileWrapper.setName(name);
        return this;
    }

    /**
     * 获取原始文件名
     */
    public String getOriginalFilename() {
        return fileWrapper.getOriginalFilename();
    }

    /**
     * 设置原始文件名
     */
    public UploadPretreatment setOriginalFilename(String originalFilename) {
        fileWrapper.setOriginalFilename(originalFilename);
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
    public UploadPretreatment putAttr(String key,Object value) {
        getAttr().put(key,value);
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
            MultipartFile mf = fileWrapper.getMultipartFile();
            fileWrapper.setMultipartFile(new MockMultipartFile(mf.getName(),mf.getOriginalFilename(),null,out.toByteArray()));
            return this;
        } catch (IOException e) {
            throw new FileStorageRuntimeException("图片处理失败！",e);
        }
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
    public UploadPretreatment image() {
        return image(th -> th.size(200,200));
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
    public UploadPretreatment thumbnail(Consumer<Thumbnails.Builder<? extends InputStream>> consumer) {
        try {
            return thumbnail(consumer,thumbnailBytes != null ? new ByteArrayInputStream(thumbnailBytes) : fileWrapper.getInputStream());
        } catch (IOException e) {
            throw new FileStorageRuntimeException("生成缩略图失败！",e);
        }
    }

    /**
     * 通过指定 MultipartFile 生成缩略图并进行图片处理，
     * 可以进行裁剪、旋转、缩放、水印等操作，默认输出图片格式通过 thumbnailSuffix 获取，
     */
    public UploadPretreatment thumbnail(Consumer<Thumbnails.Builder<? extends InputStream>> consumer,MultipartFile file) {
        try {
            return thumbnail(consumer,file.getInputStream());
        } catch (IOException e) {
            throw new FileStorageRuntimeException("生成缩略图失败！",e);
        }
    }

    /**
     * 通过指定 InputStream 生成缩略图并进行图片处理，
     * 可以进行裁剪、旋转、缩放、水印等操作，默认输出图片格式通过 thumbnailSuffix 获取，
     * 操作完成后会自动关闭 InputStream
     */
    public UploadPretreatment thumbnail(Consumer<Thumbnails.Builder<? extends InputStream>> consumer,InputStream in) {
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
        } finally {
            IoUtil.close(in);
        }
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
    public UploadPretreatment thumbnail() {
        return thumbnail(200,200);
    }

    /**
     * 上传文件，成功返回文件信息，失败返回null
     */
    public FileInfo upload() {
        return fileStorageService.upload(this);
    }
}
