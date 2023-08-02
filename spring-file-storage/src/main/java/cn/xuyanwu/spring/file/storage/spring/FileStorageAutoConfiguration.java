package cn.xuyanwu.spring.file.storage.spring;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.extra.ftp.Ftp;
import cn.hutool.extra.ssh.Sftp;
import cn.xuyanwu.spring.file.storage.FileStorageService;
import cn.xuyanwu.spring.file.storage.aspect.FileStorageAspect;
import cn.xuyanwu.spring.file.storage.exception.FileStorageRuntimeException;
import cn.xuyanwu.spring.file.storage.file.*;
import cn.xuyanwu.spring.file.storage.platform.*;
import cn.xuyanwu.spring.file.storage.platform.QiniuKodoFileStorageClientFactory.QiniuKodoClient;
import cn.xuyanwu.spring.file.storage.recorder.DefaultFileRecorder;
import cn.xuyanwu.spring.file.storage.recorder.FileRecorder;
import cn.xuyanwu.spring.file.storage.spring.file.MultipartFileWrapperAdapter;
import cn.xuyanwu.spring.file.storage.tika.ContentTypeDetect;
import cn.xuyanwu.spring.file.storage.tika.DefaultTikaFactory;
import cn.xuyanwu.spring.file.storage.tika.TikaContentTypeDetect;
import cn.xuyanwu.spring.file.storage.tika.TikaFactory;
import cn.xuyanwu.spring.file.storage.util.Tools;
import com.aliyun.oss.OSS;
import com.amazonaws.services.s3.AmazonS3;
import com.baidubce.services.bos.BosClient;
import com.github.sardine.Sardine;
import com.google.cloud.storage.Storage;
import com.obs.services.ObsClient;
import com.qcloud.cos.COSClient;
import com.upyun.RestManager;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@Configuration
@ConditionalOnMissingBean(FileStorageService.class)
public class FileStorageAutoConfiguration implements WebMvcConfigurer {

    @Autowired
    private SpringFileStorageProperties properties;
    @Autowired
    private ApplicationContext applicationContext;


    /**
     * 配置本地存储的访问地址
     */
    @Override
    public void addResourceHandlers(@NotNull ResourceHandlerRegistry registry) {
        for (SpringFileStorageProperties.SpringLocalConfig local : properties.getLocal()) {
            if (local.getEnableAccess()) {
                registry.addResourceHandler(local.getPathPatterns()).addResourceLocations("file:" + local.getBasePath());
            }
        }
        for (SpringFileStorageProperties.SpringLocalPlusConfigConfig local : properties.getLocalPlus()) {
            if (local.getEnableAccess()) {
                registry.addResourceHandler(local.getPathPatterns()).addResourceLocations("file:" + local.getStoragePath());
            }
        }
    }

    /**
     * 本地存储 Bean
     */
    @Bean
    public List<LocalFileStorage> localFileStorageList() {
        return properties.getLocal().stream().map(config -> {
            if (!config.getEnableStorage()) return null;
            log.info("加载存储平台：{}，此存储平台已不推荐使用，新项目请使用 LocalPlusFileStorage",config.getPlatform());
            return new LocalFileStorage(config);
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * 本地存储升级版 Bean
     */
    @Bean
    public List<LocalPlusFileStorage> localPlusFileStorageList() {
        return properties.getLocalPlus().stream().map(config -> {
            if (!config.getEnableStorage()) return null;
            log.info("加载存储平台：{}",config.getPlatform());
            return new LocalPlusFileStorage(config);
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * 华为云 OBS 存储 Bean
     */
    @Bean
    @ConditionalOnClass(name = "com.obs.services.ObsClient")
    public List<HuaweiObsFileStorage> huaweiObsFileStorageList(List<List<FileStorageClientFactory<?>>> clientFactoryList) {
        return properties.getHuaweiObs().stream().map(config -> {
            if (!config.getEnableStorage()) return null;
            log.info("加载存储平台：{}",config.getPlatform());
            FileStorageClientFactory<ObsClient> clientFactory = getFactory(config.getPlatform(),clientFactoryList,() -> new HuaweiObsFileStorageClientFactory(config));
            return new HuaweiObsFileStorage(config,clientFactory);
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * 阿里云 OSS 存储 Bean
     */
    @Bean
    @ConditionalOnClass(name = "com.aliyun.oss.OSS")
    public List<AliyunOssFileStorage> aliyunOssFileStorageList(List<List<FileStorageClientFactory<?>>> clientFactoryList) {
        return properties.getAliyunOss().stream().map(config -> {
            if (!config.getEnableStorage()) return null;
            log.info("加载存储平台：{}",config.getPlatform());
            FileStorageClientFactory<OSS> clientFactory = getFactory(config.getPlatform(),clientFactoryList,() -> new AliyunOssFileStorageClientFactory(config));
            return new AliyunOssFileStorage(config,clientFactory);
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * 七牛云 Kodo 存储 Bean
     */
    @Bean
    @ConditionalOnClass(name = "com.qiniu.storage.UploadManager")
    public List<QiniuKodoFileStorage> qiniuKodoFileStorageList(List<List<FileStorageClientFactory<?>>> clientFactoryList) {
        return properties.getQiniuKodo().stream().map(config -> {
            if (!config.getEnableStorage()) return null;
            log.info("加载存储平台：{}",config.getPlatform());
            FileStorageClientFactory<QiniuKodoClient> clientFactory = getFactory(config.getPlatform(),clientFactoryList,() -> new QiniuKodoFileStorageClientFactory(config));
            return new QiniuKodoFileStorage(config,clientFactory);
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * 腾讯云 COS 存储 Bean
     */
    @Bean
    @ConditionalOnClass(name = "com.qcloud.cos.COSClient")
    public List<TencentCosFileStorage> tencentCosFileStorageList(List<List<FileStorageClientFactory<?>>> clientFactoryList) {
        return properties.getTencentCos().stream().map(config -> {
            if (!config.getEnableStorage()) return null;
            log.info("加载存储平台：{}",config.getPlatform());
            FileStorageClientFactory<COSClient> clientFactory = getFactory(config.getPlatform(),clientFactoryList,() -> new TencentCosFileStorageClientFactory(config));
            return new TencentCosFileStorage(config,clientFactory);
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * 百度云 BOS 存储 Bean
     */
    @Bean
    @ConditionalOnClass(name = "com.baidubce.services.bos.BosClient")
    public List<BaiduBosFileStorage> baiduBosFileStorageList(List<List<FileStorageClientFactory<?>>> clientFactoryList) {
        return properties.getBaiduBos().stream().map(config -> {
            if (!config.getEnableStorage()) return null;
            log.info("加载存储平台：{}",config.getPlatform());
            FileStorageClientFactory<BosClient> clientFactory = getFactory(config.getPlatform(),clientFactoryList,() -> new BaiduBosFileStorageClientFactory(config));
            return new BaiduBosFileStorage(config,clientFactory);
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * 又拍云 USS 存储 Bean
     */
    @Bean
    @ConditionalOnClass(name = "com.upyun.RestManager")
    public List<UpyunUssFileStorage> upyunUssFileStorageList(List<List<FileStorageClientFactory<?>>> clientFactoryList) {
        return properties.getUpyunUss().stream().map(config -> {
            if (!config.getEnableStorage()) return null;
            log.info("加载存储平台：{}",config.getPlatform());
            FileStorageClientFactory<RestManager> clientFactory = getFactory(config.getPlatform(),clientFactoryList,() -> new UpyunUssFileStorageClientFactory(config));
            return new UpyunUssFileStorage(config,clientFactory);
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * MinIO 存储 Bean
     */
    @Bean
    @ConditionalOnClass(name = "io.minio.MinioClient")
    public List<MinioFileStorage> minioFileStorageList(List<List<FileStorageClientFactory<?>>> clientFactoryList) {
        return properties.getMinio().stream().map(config -> {
            if (!config.getEnableStorage()) return null;
            log.info("加载存储平台：{}",config.getPlatform());
            FileStorageClientFactory<MinioClient> clientFactory = getFactory(config.getPlatform(),clientFactoryList,() -> new MinioFileStorageClientFactory(config));
            return new MinioFileStorage(config,clientFactory);
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * AWS 存储 Bean
     */
    @Bean
    @ConditionalOnClass(name = "com.amazonaws.services.s3.AmazonS3")
    public List<AmazonS3FileStorage> amazonS3FileStorageList(List<List<FileStorageClientFactory<?>>> clientFactoryList) {
        return properties.getAmazonS3().stream().map(config -> {
            if (!config.getEnableStorage()) return null;
            log.info("加载存储平台：{}",config.getPlatform());
            FileStorageClientFactory<AmazonS3> clientFactory = getFactory(config.getPlatform(),clientFactoryList,() -> new AmazonS3FileStorageClientFactory(config));
            return new AmazonS3FileStorage(config,clientFactory);
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * FTP 存储 Bean
     */
    @Bean
    @ConditionalOnClass(name = {"org.apache.commons.net.ftp.FTPClient","cn.hutool.extra.ftp.Ftp"})
    public List<FtpFileStorage> ftpFileStorageList(List<List<FileStorageClientFactory<?>>> clientFactoryList) {
        return properties.getFtp().stream().map(config -> {
            if (!config.getEnableStorage()) return null;
            log.info("加载存储平台：{}",config.getPlatform());
            FileStorageClientFactory<Ftp> clientFactory = getFactory(config.getPlatform(),clientFactoryList,() -> new FtpFileStorageClientFactory(config));
            return new FtpFileStorage(config,clientFactory);
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * SFTP 存储 Bean
     */
    @Bean
    @ConditionalOnClass(name = {"com.jcraft.jsch.ChannelSftp","cn.hutool.extra.ftp.Ftp"})
    public List<SftpFileStorage> sftpFileStorageList(List<List<FileStorageClientFactory<?>>> clientFactoryList) {
        return properties.getSftp().stream().map(config -> {
            if (!config.getEnableStorage()) return null;
            log.info("加载存储平台：{}",config.getPlatform());
            FileStorageClientFactory<Sftp> clientFactory = getFactory(config.getPlatform(),clientFactoryList,() -> new SftpFileStorageClientFactory(config));
            return new SftpFileStorage(config,clientFactory);
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * WebDAV 存储 Bean
     */
    @Bean
    @ConditionalOnClass(name = "com.github.sardine.Sardine")
    public List<WebDavFileStorage> webDavFileStorageList(List<List<FileStorageClientFactory<?>>> clientFactoryList) {
        return properties.getWebDav().stream().map(config -> {
            if (!config.getEnableStorage()) return null;
            log.info("加载存储平台：{}",config.getPlatform());
            FileStorageClientFactory<Sardine> clientFactory = getFactory(config.getPlatform(),clientFactoryList,() -> new WebDavFileStorageClientFactory(config));
            return new WebDavFileStorage(config,clientFactory);
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * Google Cloud Storage 存储 Bean
     */
    @Bean
    @ConditionalOnClass(name = "com.google.cloud.storage.Storage")
    public List<GoogleCloudStorageFileStorage> googleCloudStorageFileStorageList(List<List<FileStorageClientFactory<?>>> clientFactoryList) {
        return properties.getGoogleCloudStorage().stream().map(config -> {
            if (!config.getEnableStorage()) return null;
            log.info("加载存储平台：{}",config.getPlatform());
            FileStorageClientFactory<Storage> clientFactory = getFactory(config.getPlatform(),clientFactoryList,() -> new GoogleCloudStorageFileStorageClientFactory(config));
            return new GoogleCloudStorageFileStorage(config,clientFactory);
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * 当没有找到 FileRecorder 时使用默认的 FileRecorder
     */
    @Bean
    @ConditionalOnMissingBean(FileRecorder.class)
    public FileRecorder fileRecorder() {
        log.warn("没有找到 FileRecorder 的实现类，文件上传之外的部分功能无法正常使用，必须实现该接口才能使用完整功能！");
        return new DefaultFileRecorder();
    }

    /**
     * Tika 工厂类型，用于识别上传的文件的 MINE
     */
    @Bean
    @ConditionalOnMissingBean(TikaFactory.class)
    public TikaFactory tikaFactory() {
        return new DefaultTikaFactory();
    }

    /**
     * 识别文件的 MIME 类型
     */
    @Bean
    @ConditionalOnMissingBean(ContentTypeDetect.class)
    public ContentTypeDetect contentTypeDetect(TikaFactory tikaFactory) {
        return new TikaContentTypeDetect(tikaFactory);
    }


    /**
     * byte[] 文件包装适配器
     */
    @Bean
    @ConditionalOnMissingBean(ByteFileWrapperAdapter.class)
    public ByteFileWrapperAdapter byteFileWrapperAdapter(ContentTypeDetect contentTypeDetect) {
        return new ByteFileWrapperAdapter(contentTypeDetect);
    }

    /**
     * URL 文件包装适配器，兼容Spring的ClassPath路径、文件路径、HTTP路径等
     */
    @Bean
    @ConditionalOnMissingBean(URLFileWrapperAdapter.class)
    public URLFileWrapperAdapter httpFileWrapperAdapter(ContentTypeDetect contentTypeDetect) {
        return new URLFileWrapperAdapter(contentTypeDetect);
    }

    /**
     * InputStream 文件包装适配器
     */
    @Bean
    @ConditionalOnMissingBean(InputStreamFileWrapperAdapter.class)
    public InputStreamFileWrapperAdapter inputStreamFileWrapperAdapter(ContentTypeDetect contentTypeDetect) {
        return new InputStreamFileWrapperAdapter(contentTypeDetect);
    }

    /**
     * 本地文件包装适配器
     */
    @Bean
    @ConditionalOnMissingBean(LocalFileWrapperAdapter.class)
    public LocalFileWrapperAdapter localFileWrapperAdapter(ContentTypeDetect contentTypeDetect) {
        return new LocalFileWrapperAdapter(contentTypeDetect);
    }

    /**
     * Multipart 文件包装适配器
     */
    @Bean
    @ConditionalOnMissingBean(MultipartFileWrapperAdapter.class)
    public MultipartFileWrapperAdapter multipartFileWrapperAdapter() {
        return new MultipartFileWrapperAdapter();
    }


    /**
     * 文件存储服务
     */
    @Bean(destroyMethod = "destroy")
    public FileStorageService fileStorageService(FileRecorder fileRecorder,
                                                 List<List<? extends FileStorage>> fileStorageLists,
                                                 List<FileStorageAspect> aspectList,
                                                 List<FileWrapperAdapter> fileWrapperAdapterList,
                                                 ContentTypeDetect contentTypeDetect) {
        this.initDetect();
        FileStorageService service = new FileStorageService();
        service.setFileStorageList(new CopyOnWriteArrayList<>());
        fileStorageLists.forEach(service.getFileStorageList()::addAll);
        service.setFileRecorder(fileRecorder);
        service.setDefaultPlatform(properties.getDefaultPlatform());
        service.setThumbnailSuffix(properties.getThumbnailSuffix());
        service.setAspectList(new CopyOnWriteArrayList<>(aspectList));
        service.setFileWrapperAdapterList(new CopyOnWriteArrayList<>(fileWrapperAdapterList));
        service.setContentTypeDetect(contentTypeDetect);
        return service;
    }

    /**
     * 对 FileStorageService 注入自己的代理对象，不然会导致针对 FileStorageService 的代理方法不生效
     */
    @EventListener(ContextRefreshedEvent.class)
    public void onContextRefreshedEvent() {
        FileStorageService service = applicationContext.getBean(FileStorageService.class);
        service.setSelf(service);
    }

    public void initDetect() {
        String template = "检测到{}配置，但是没有找到对应的依赖库，所以无法加载此存储平台！配置参考地址：https://spring-file-storage.xuyanwu.cn/#/%E5%BF%AB%E9%80%9F%E5%85%A5%E9%97%A8";
        if (CollUtil.isNotEmpty(properties.getHuaweiObs()) && doesNotExistClass("com.obs.services.ObsClient")) {
            log.warn(template,"华为云 OBS ");
        }
        if (CollUtil.isNotEmpty(properties.getAliyunOss()) && doesNotExistClass("com.aliyun.oss.OSS")) {
            log.warn(template,"阿里云 OSS ");
        }
        if (CollUtil.isNotEmpty(properties.getQiniuKodo()) && doesNotExistClass("com.qiniu.storage.UploadManager")) {
            log.warn(template,"七牛云 Kodo ");
        }
        if (CollUtil.isNotEmpty(properties.getTencentCos()) && doesNotExistClass("com.qcloud.cos.COSClient")) {
            log.warn(template,"腾讯云 COS ");
        }
        if (CollUtil.isNotEmpty(properties.getBaiduBos()) && doesNotExistClass("com.baidubce.services.bos.BosClient")) {
            log.warn(template,"百度云 BOS ");
        }
        if (CollUtil.isNotEmpty(properties.getUpyunUss()) && doesNotExistClass("com.upyun.RestManager")) {
            log.warn(template,"又拍云 USS ");
        }
        if (CollUtil.isNotEmpty(properties.getMinio()) && doesNotExistClass("io.minio.MinioClient")) {
            log.warn(template," MinIO ");
        }
        if (CollUtil.isNotEmpty(properties.getAmazonS3()) && doesNotExistClass("com.amazonaws.services.s3.AmazonS3")) {
            log.warn(template," AmazonS3 ");
        }
        if (CollUtil.isNotEmpty(properties.getFtp()) && (doesNotExistClass("org.apache.commons.net.ftp.FTPClient") || doesNotExistClass("cn.hutool.extra.ftp.Ftp"))) {
            log.warn(template," FTP ");
        }
        if (CollUtil.isNotEmpty(properties.getFtp()) && (doesNotExistClass("com.jcraft.jsch.ChannelSftp") || doesNotExistClass("cn.hutool.extra.ftp.Ftp"))) {
            log.warn(template," SFTP ");
        }
        if (CollUtil.isNotEmpty(properties.getWebDav()) && doesNotExistClass("com.github.sardine.Sardine")) {
            log.warn(template," WebDAV ");
        }
        if (CollUtil.isNotEmpty(properties.getGoogleCloudStorage()) && doesNotExistClass("com.google.cloud.storage.Storage")) {
            log.warn(template,"Google Cloud Storage ");
        }
    }

    /**
     * 判断是否没有引入指定 Class
     */
    public static boolean doesNotExistClass(String name) {
        try {
            Class.forName(name);
            return false;
        } catch (ClassNotFoundException e) {
            return true;
        }
    }


    /**
     * 获取或创建指定存储平台的 Client 工厂对象
     */
    public static <Client> FileStorageClientFactory<Client> getFactory(String platform,List<List<FileStorageClientFactory<?>>> list,Supplier<FileStorageClientFactory<Client>> defaultSupplier) {
        for (List<FileStorageClientFactory<?>> factoryList : list) {
            for (FileStorageClientFactory<?> factory : factoryList) {
                if (Objects.equals(platform,factory.getPlatform())) {
                    try {
                        return Tools.cast(factory);
                    } catch (Exception e) {
                        throw new FileStorageRuntimeException("获取 FileStorageClientFactory 失败，类型不匹配，platform：" + platform,e);
                    }
                }
            }
        }
        return defaultSupplier.get();
    }

}
