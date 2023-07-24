package cn.xuyanwu.spring.file.storage.spring;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.ssh.Sftp;
import cn.xuyanwu.spring.file.storage.FileStorageService;
import cn.xuyanwu.spring.file.storage.aspect.FileStorageAspect;
import cn.xuyanwu.spring.file.storage.file.*;
import cn.xuyanwu.spring.file.storage.platform.*;
import cn.xuyanwu.spring.file.storage.recorder.DefaultFileRecorder;
import cn.xuyanwu.spring.file.storage.recorder.FileRecorder;
import cn.xuyanwu.spring.file.storage.spring.file.MultipartFileWrapperAdapter;
import cn.xuyanwu.spring.file.storage.tika.ContentTypeDetect;
import cn.xuyanwu.spring.file.storage.tika.DefaultTikaFactory;
import cn.xuyanwu.spring.file.storage.tika.TikaContentTypeDetect;
import cn.xuyanwu.spring.file.storage.tika.TikaFactory;
import cn.xuyanwu.spring.file.storage.util.Tools;
import com.aliyun.oss.ClientBuilderConfiguration;
import com.amazonaws.ClientConfiguration;
import com.baidubce.services.bos.BosClientConfiguration;
import com.obs.services.ObsConfiguration;
import com.qcloud.cos.ClientConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
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
        for (FileStorageProperties.LocalPlus local : properties.getLocalPlus()) {
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
        return properties.getLocal().stream().map(local -> {
            if (!local.getEnableStorage()) return null;
            log.info("加载存储平台：{}，此存储平台已不推荐使用，新项目请使用 LocalPlusFileStorage",local.getPlatform());
            LocalFileStorage localFileStorage = new LocalFileStorage();
            localFileStorage.setPlatform(local.getPlatform());
            localFileStorage.setBasePath(local.getBasePath());
            localFileStorage.setDomain(local.getDomain());
            return localFileStorage;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * 本地存储升级版 Bean
     */
    @Bean
    public List<LocalPlusFileStorage> localPlusFileStorageList() {
        return properties.getLocalPlus().stream().map(local -> {
            if (!local.getEnableStorage()) return null;
            log.info("加载存储平台：{}",local.getPlatform());
            LocalPlusFileStorage localFileStorage = new LocalPlusFileStorage();
            localFileStorage.setPlatform(local.getPlatform());
            localFileStorage.setBasePath(local.getBasePath());
            localFileStorage.setDomain(local.getDomain());
            localFileStorage.setStoragePath(local.getStoragePath());
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
            storage.setDefaultAcl(obs.getDefaultAcl());
            storage.setMultipartThreshold(obs.getMultipartThreshold());
            storage.setMultipartPartSize(obs.getMultipartPartSize());
            storage.setClientConfigurationSupplier(getClientConfigurationSupplier(obs::getClientConfiguration,ObsConfiguration.class));

            if (obs.getClientConfiguration() != null) {
                if (obs.getClientConfiguration() instanceof ObsConfiguration) {
                    storage.setClientConfiguration((ObsConfiguration) obs.getClientConfiguration());
                } else {
                    storage.setClientConfiguration(BeanUtil.copyProperties(obs.getClientConfiguration(),ObsConfiguration.class));
                }
            }
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
            storage.setDefaultAcl(oss.getDefaultAcl());
            storage.setMultipartThreshold(oss.getMultipartThreshold());
            storage.setMultipartPartSize(oss.getMultipartPartSize());
            storage.setClientConfigurationSupplier(getClientConfigurationSupplier(oss::getClientConfiguration,ClientBuilderConfiguration.class));
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
            storage.setDefaultAcl(cos.getDefaultAcl());
            storage.setMultipartThreshold(cos.getMultipartThreshold());
            storage.setMultipartPartSize(cos.getMultipartPartSize());
            storage.setClientConfigurationSupplier(getClientConfigurationSupplier(cos::getClientConfiguration,ClientConfig.class));
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
            storage.setDefaultAcl(bos.getDefaultAcl());
            storage.setMultipartThreshold(bos.getMultipartThreshold());
            storage.setMultipartPartSize(bos.getMultipartPartSize());
            storage.setClientConfigurationSupplier(getClientConfigurationSupplier(bos::getClientConfiguration,BosClientConfiguration.class));
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
     * AWS 存储 Bean
     */
    @Bean
    @ConditionalOnClass(name = "com.amazonaws.services.s3.AmazonS3")
    public List<AwsS3FileStorage> amazonS3FileStorageList() {
        return properties.getAwsS3().stream().map(s3 -> {
            if (!s3.getEnableStorage()) return null;
            log.info("加载存储平台：{}",s3.getPlatform());
            AwsS3FileStorage storage = new AwsS3FileStorage();
            storage.setPlatform(s3.getPlatform());
            storage.setAccessKey(s3.getAccessKey());
            storage.setSecretKey(s3.getSecretKey());
            storage.setRegion(s3.getRegion());
            storage.setEndPoint(s3.getEndPoint());
            storage.setBucketName(s3.getBucketName());
            storage.setDomain(s3.getDomain());
            storage.setBasePath(s3.getBasePath());
            storage.setDefaultAcl(s3.getDefaultAcl());
            storage.setMultipartThreshold(s3.getMultipartThreshold());
            storage.setMultipartPartSize(s3.getMultipartPartSize());
            storage.setClientConfigurationSupplier(getClientConfigurationSupplier(s3::getClientConfiguration,ClientConfiguration.class));
            return storage;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * FTP 存储 Bean
     */
    @Bean
    @ConditionalOnClass(name = {"org.apache.commons.net.ftp.FTPClient","cn.hutool.extra.ftp.Ftp"})
    public List<FtpFileStorage> ftpFileStorageList() {
        return properties.getFtp().stream().map(ftp -> {
            if (!ftp.getEnableStorage()) return null;
            log.info("加载存储平台：{}",ftp.getPlatform());
            FtpFileStorage storage = new FtpFileStorage();
            storage.setPlatform(ftp.getPlatform());
            storage.setHost(ftp.getHost());
            storage.setPort(ftp.getPort());
            storage.setUser(ftp.getUser());
            storage.setPassword(ftp.getPassword());
            storage.setCharset(ftp.getCharset());
            storage.setConnectionTimeout(ftp.getConnectionTimeout());
            storage.setSoTimeout(ftp.getSoTimeout());
            storage.setServerLanguageCode(ftp.getServerLanguageCode());
            storage.setSystemKey(ftp.getSystemKey());
            storage.setIsActive(ftp.getIsActive());
            storage.setDomain(ftp.getDomain());
            storage.setBasePath(ftp.getBasePath());
            storage.setStoragePath(ftp.getStoragePath());
            return storage;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * SFTP 存储 Bean
     */
    @Bean
    @ConditionalOnClass(name = {"com.jcraft.jsch.ChannelSftp","cn.hutool.extra.ftp.Ftp"})
    public List<SftpFileStorage> sftpFileStorageList(List<FileStorageClientFactory<Sftp>> factoryList) {
        Map<String,FileStorageClientFactory<Sftp>> factoryMap = factoryList.stream().collect(Collectors.toMap(FileStorageClientFactory::getPlatform,v -> v));
        return properties.getSftp().stream().map(sftp -> {
            if (!sftp.getEnableStorage()) return null;
            log.info("加载存储平台：{}",sftp.getPlatform());
            SftpFileStorage storage = new SftpFileStorage();
            storage.setPlatform(sftp.getPlatform());
            storage.setDomain(sftp.getDomain());
            storage.setBasePath(sftp.getBasePath());
            storage.setStoragePath(sftp.getStoragePath());
            FileStorageClientFactory<Sftp> clientFactory = factoryMap.get(sftp.getPlatform());
            if (clientFactory == null) clientFactory = new SftpFileStorageClientFactory(sftp);
            storage.setClientFactory(clientFactory);
            return storage;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * WebDAV 存储 Bean
     */
    @Bean
    @ConditionalOnClass(name = "com.github.sardine.Sardine")
    public List<WebDavFileStorage> webDavFileStorageList() {
        return properties.getWebDav().stream().map(sftp -> {
            if (!sftp.getEnableStorage()) return null;
            log.info("加载存储平台：{}",sftp.getPlatform());
            WebDavFileStorage storage = new WebDavFileStorage();
            storage.setPlatform(sftp.getPlatform());
            storage.setServer(sftp.getServer());
            storage.setUser(sftp.getUser());
            storage.setPassword(sftp.getPassword());
            storage.setDomain(sftp.getDomain());
            storage.setBasePath(sftp.getBasePath());
            storage.setStoragePath(sftp.getStoragePath());
            return storage;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Bean
    @ConditionalOnClass(name = "com.google.cloud.storage.Storage")
    public List<GoogleCloudStorage> googleCloudStorageList() {
        return properties.getGoogleCloud().stream().map(googleCloud -> {
            if (!googleCloud.getEnableStorage()) return null;
            log.info("加载存储平台：{}",googleCloud.getPlatform());
            GoogleCloudStorage storage = new GoogleCloudStorage();
            storage.setPlatform(googleCloud.getPlatform());
            storage.setProjectId(googleCloud.getProjectId());
            storage.setBucketName(googleCloud.getBucketName());
            storage.setCredentialsPath(googleCloud.getCredentialsPath());
            storage.setDomain(googleCloud.getDomain());
            storage.setBasePath(googleCloud.getBasePath());
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
        service.setProperties(properties);
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
        if (CollUtil.isNotEmpty(properties.getUpyunUSS()) && doesNotExistClass("com.upyun.RestManager")) {
            log.warn(template,"又拍云 USS ");
        }
        if (CollUtil.isNotEmpty(properties.getMinio()) && doesNotExistClass("io.minio.MinioClient")) {
            log.warn(template," MinIO ");
        }
        if (CollUtil.isNotEmpty(properties.getAwsS3()) && doesNotExistClass("com.amazonaws.services.s3.AmazonS3")) {
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
        if (CollUtil.isNotEmpty(properties.getGoogleCloud()) && doesNotExistClass("com.google.cloud.storage.Storage")) {
            log.warn(template,"谷歌云存储 ");
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
     * 尝试获取 ClientConfiguration 对象，支持以下三种方式：
     * 1、本身就是 ClientConfiguration 及其子类，直接返回
     * 2、通过 SpEL 表达式获取，例如 @fileStorageClientConfigurationSupplier.getBosClientConfiguration()
     * 3、通过 copy 对象属性创建，可以支持将 Map 或自定义对象转换为 ClientConfiguration 对象
     */
    public <T> Supplier<T> getClientConfigurationSupplier(Supplier<Object> objectSupplier,Class<T> clazz) {
        return () -> {
            Object obj = objectSupplier.get();
            if (obj == null) return null;

            if (obj instanceof String) {//尝试通过 SpEL 表达式获取
                String expressionString = (String) obj;
                if (StrUtil.isBlank(expressionString)) return null;
                ExpressionParser parser = new SpelExpressionParser();
                StandardEvaluationContext context = new StandardEvaluationContext();
                context.setBeanResolver((evaluationContext,beanName) -> applicationContext.getBean(beanName));
                obj = parser.parseExpression((String) obj).getValue(context);
            }

            if (clazz.isInstance(obj)) {//本身就是 ClientConfiguration 及其子类
                return Tools.cast(obj);
            } else {//尝试通过 copy 对象方式获取
                return BeanUtil.copyProperties(obj,clazz);
            }
        };
    }

}
