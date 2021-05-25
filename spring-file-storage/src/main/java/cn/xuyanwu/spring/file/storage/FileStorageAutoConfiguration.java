package cn.xuyanwu.spring.file.storage;

import cn.hutool.core.collection.CollUtil;
import cn.xuyanwu.spring.file.storage.aspect.FileStorageAspect;
import cn.xuyanwu.spring.file.storage.platform.*;
import cn.xuyanwu.spring.file.storage.recorder.DefaultFileRecorder;
import cn.xuyanwu.spring.file.storage.recorder.FileRecorder;
import lombok.extern.slf4j.Slf4j;
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
import java.util.stream.Collectors;

@Slf4j
@Configuration
@ConditionalOnMissingBean(FileStorageService.class)
public class FileStorageAutoConfiguration implements WebMvcConfigurer {

    @Autowired
    private FileStorageProperties properties;
    @Autowired
    private ApplicationContext applicationContext;


    /**
     * 配置本地存储的访问地址
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        for (FileStorageProperties.Local local : properties.getLocal()) {
            if (local.getEnableAccess()) {
                registry.addResourceHandler(local.getPathPatterns()).addResourceLocations("file:" + local.getBasePath());
            }
        }
    }

    /**
     * 本地存储 Bean
     */
    @Bean
    public List<LocalFileStorage> localFileStorageList() {
        return properties.getLocal().stream().map(local -> {
            if (!local.getEnableStorage()) return null;
            log.info("加载存储平台：{}",local.getPlatform());
            LocalFileStorage localFileStorage = new LocalFileStorage();
            localFileStorage.setPlatform(local.getPlatform());
            localFileStorage.setBasePath(local.getBasePath());
            localFileStorage.setDomain(local.getDomain());
            return localFileStorage;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * 华为云 OBS 存储 Bean
     */
    @Bean
    @ConditionalOnClass(name = "com.obs.services.ObsClient")
    public List<HuaweiObsFileStorage> huaweiObsFileStorageList() {
        return properties.getHuaweiObs().stream().map(obs -> {
            if (!obs.getEnableStorage()) return null;
            log.info("加载存储平台：{}",obs.getPlatform());
            HuaweiObsFileStorage storage = new HuaweiObsFileStorage();
            storage.setPlatform(obs.getPlatform());
            storage.setAccessKey(obs.getAccessKey());
            storage.setSecretKey(obs.getSecretKey());
            storage.setEndPoint(obs.getEndPoint());
            storage.setBucketName(obs.getBucketName());
            storage.setDomain(obs.getDomain());
            storage.setBasePath(obs.getBasePath());
            return storage;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * 阿里云 OSS 存储 Bean
     */
    @Bean
    @ConditionalOnClass(name = "com.aliyun.oss.OSS")
    public List<AliyunOssFileStorage> aliyunOssFileStorageList() {
        return properties.getAliyunOss().stream().map(oss -> {
            if (!oss.getEnableStorage()) return null;
            log.info("加载存储平台：{}",oss.getPlatform());
            AliyunOssFileStorage storage = new AliyunOssFileStorage();
            storage.setPlatform(oss.getPlatform());
            storage.setAccessKey(oss.getAccessKey());
            storage.setSecretKey(oss.getSecretKey());
            storage.setEndPoint(oss.getEndPoint());
            storage.setBucketName(oss.getBucketName());
            storage.setDomain(oss.getDomain());
            storage.setBasePath(oss.getBasePath());
            return storage;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * 七牛云 Kodo 存储 Bean
     */
    @Bean
    @ConditionalOnClass(name = "com.qiniu.storage.UploadManager")
    public List<QiniuKodoFileStorage> qiniuKodoFileStorageList() {
        return properties.getQiniuKodo().stream().map(kodo -> {
            if (!kodo.getEnableStorage()) return null;
            log.info("加载存储平台：{}",kodo.getPlatform());
            QiniuKodoFileStorage storage = new QiniuKodoFileStorage();
            storage.setPlatform(kodo.getPlatform());
            storage.setAccessKey(kodo.getAccessKey());
            storage.setSecretKey(kodo.getSecretKey());
            storage.setBucketName(kodo.getBucketName());
            storage.setDomain(kodo.getDomain());
            storage.setBasePath(kodo.getBasePath());
            return storage;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * 腾讯云 COS 存储 Bean
     */
    @Bean
    @ConditionalOnClass(name = "com.qcloud.cos.COSClient")
    public List<TencentCosFileStorage> tencentCosFileStorageList() {
        return properties.getTencentCos().stream().map(cos -> {
            if (!cos.getEnableStorage()) return null;
            log.info("加载存储平台：{}",cos.getPlatform());
            TencentCosFileStorage storage = new TencentCosFileStorage();
            storage.setPlatform(cos.getPlatform());
            storage.setSecretId(cos.getSecretId());
            storage.setSecretKey(cos.getSecretKey());
            storage.setRegion(cos.getRegion());
            storage.setBucketName(cos.getBucketName());
            storage.setDomain(cos.getDomain());
            storage.setBasePath(cos.getBasePath());
            return storage;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * 百度云 BOS 存储 Bean
     */
    @Bean
    @ConditionalOnClass(name = "com.baidubce.services.bos.BosClient")
    public List<BaiduBosFileStorage> baiduBosFileStorageList() {
        return properties.getBaiduBos().stream().map(bos -> {
            if (!bos.getEnableStorage()) return null;
            log.info("加载存储平台：{}",bos.getPlatform());
            BaiduBosFileStorage storage = new BaiduBosFileStorage();
            storage.setPlatform(bos.getPlatform());
            storage.setAccessKey(bos.getAccessKey());
            storage.setSecretKey(bos.getSecretKey());
            storage.setEndPoint(bos.getEndPoint());
            storage.setBucketName(bos.getBucketName());
            storage.setDomain(bos.getDomain());
            storage.setBasePath(bos.getBasePath());
            return storage;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * 又拍云 USS 存储 Bean
     */
    @Bean
    @ConditionalOnClass(name = "com.upyun.RestManager")
    public List<UpyunUssFileStorage> upyunUssFileStorageList() {
        return properties.getUpyunUSS().stream().map(uss -> {
            if (!uss.getEnableStorage()) return null;
            log.info("加载存储平台：{}",uss.getPlatform());
            UpyunUssFileStorage storage = new UpyunUssFileStorage();
            storage.setPlatform(uss.getPlatform());
            storage.setUsername(uss.getUsername());
            storage.setPassword(uss.getPassword());
            storage.setBucketName(uss.getBucketName());
            storage.setDomain(uss.getDomain());
            storage.setBasePath(uss.getBasePath());
            return storage;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * MinIO 存储 Bean
     */
    @Bean
    @ConditionalOnClass(name = "io.minio.MinioClient")
    public List<MinIOFileStorage> minioFileStorageList() {
        return properties.getMinio().stream().map(minio -> {
            if (!minio.getEnableStorage()) return null;
            log.info("加载存储平台：{}",minio.getPlatform());
            MinIOFileStorage storage = new MinIOFileStorage();
            storage.setPlatform(minio.getPlatform());
            storage.setAccessKey(minio.getAccessKey());
            storage.setSecretKey(minio.getSecretKey());
            storage.setEndPoint(minio.getEndPoint());
            storage.setBucketName(minio.getBucketName());
            storage.setDomain(minio.getDomain());
            storage.setBasePath(minio.getBasePath());
            return storage;
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
     * 文件存储服务
     */
    @Bean
    public FileStorageService fileStorageService(FileRecorder fileRecorder,
                                                 List<List<? extends FileStorage>> fileStorageList,
                                                 List<FileStorageAspect> aspectList) {
        this.initDetect();
        FileStorageService service = new FileStorageService();

        service.setFileRecorder(fileRecorder);
        service.setFileStorageList(fileStorageList);
        service.setProperties(properties);
        service.setAspectList(aspectList);
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
        String template = "检测到{}配置，但是没有找到对应的依赖库，所以无法加载此存储平台！配置参考地址：https://gitee.com/XYW1171736840/spring-file-storage";
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
        if (CollUtil.isNotEmpty(properties.getUpyunUSS()) && doesNotExistClass("com.upyun.RestManager")) {
            log.warn(template,"又拍云 USS ");
        }
        if (CollUtil.isNotEmpty(properties.getMinio()) && doesNotExistClass("io.minio.MinioClient")) {
            log.warn(template," MinIO ");
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

}
