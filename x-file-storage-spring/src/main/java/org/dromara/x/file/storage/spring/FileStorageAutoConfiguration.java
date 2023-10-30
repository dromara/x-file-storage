package org.dromara.x.file.storage.spring;

import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.FileStorageServiceBuilder;
import org.dromara.x.file.storage.core.aspect.FileStorageAspect;
import org.dromara.x.file.storage.core.file.FileWrapperAdapter;
import org.dromara.x.file.storage.core.platform.FileStorage;
import org.dromara.x.file.storage.core.platform.FileStorageClientFactory;
import org.dromara.x.file.storage.core.recorder.DefaultFileRecorder;
import org.dromara.x.file.storage.core.recorder.FileRecorder;
import org.dromara.x.file.storage.core.tika.ContentTypeDetect;
import org.dromara.x.file.storage.core.tika.DefaultTikaFactory;
import org.dromara.x.file.storage.core.tika.TikaContentTypeDetect;
import org.dromara.x.file.storage.core.tika.TikaFactory;
import org.dromara.x.file.storage.spring.SpringFileStorageProperties.SpringLocalConfig;
import org.dromara.x.file.storage.spring.SpringFileStorageProperties.SpringLocalPlusConfig;
import org.dromara.x.file.storage.spring.file.MultipartFileWrapperAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

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
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        for (SpringLocalConfig local : properties.getLocal()) {
            if (local.getEnableStorage() && local.getEnableAccess()) {
                registry.addResourceHandler(local.getPathPatterns())
                        .addResourceLocations("file:" + local.getBasePath());
            }
        }
        for (SpringLocalPlusConfig local : properties.getLocalPlus()) {
            if (local.getEnableStorage() && local.getEnableAccess()) {
                registry.addResourceHandler(local.getPathPatterns())
                        .addResourceLocations("file:" + local.getStoragePath());
            }
        }
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
     * 文件存储服务
     */
    @Bean(destroyMethod = "destroy")
    public FileStorageService fileStorageService(
            FileRecorder fileRecorder,
            @Autowired(required = false) List<List<? extends FileStorage>> fileStorageLists,
            @Autowired(required = false) List<FileStorageAspect> aspectList,
            @Autowired(required = false) List<FileWrapperAdapter> fileWrapperAdapterList,
            ContentTypeDetect contentTypeDetect,
            @Autowired(required = false) List<List<FileStorageClientFactory<?>>> clientFactoryList) {

        if (fileStorageLists == null) fileStorageLists = new ArrayList<>();
        if (aspectList == null) aspectList = new ArrayList<>();
        if (fileWrapperAdapterList == null) fileWrapperAdapterList = new ArrayList<>();
        if (clientFactoryList == null) clientFactoryList = new ArrayList<>();

        FileStorageServiceBuilder builder = FileStorageServiceBuilder.create(properties.toFileStorageProperties())
                .setFileRecorder(fileRecorder)
                .setAspectList(aspectList)
                .setContentTypeDetect(contentTypeDetect)
                .setFileWrapperAdapterList(fileWrapperAdapterList)
                .setClientFactoryList(clientFactoryList);

        fileStorageLists.forEach(builder::addFileStorage);

        if (properties.getEnableByteFileWrapper()) {
            builder.addByteFileWrapperAdapter();
        }
        if (properties.getEnableUriFileWrapper()) {
            builder.addUriFileWrapperAdapter();
        }
        if (properties.getEnableInputStreamFileWrapper()) {
            builder.addInputStreamFileWrapperAdapter();
        }
        if (properties.getEnableLocalFileWrapper()) {
            builder.addLocalFileWrapperAdapter();
        }
        if (properties.getEnableHttpServletRequestFileWrapper()) {
            builder.addHttpServletRequestFileWrapperAdapter();
        }
        if (properties.getEnableMultipartFileWrapper()) {
            builder.addFileWrapperAdapter(new MultipartFileWrapperAdapter());
        }
        return builder.build();
    }

    /**
     * 对 FileStorageService 注入自己的代理对象，不然会导致针对 FileStorageService 的代理方法不生效
     */
    @EventListener(ContextRefreshedEvent.class)
    public void onContextRefreshedEvent() {
        FileStorageService service = applicationContext.getBean(FileStorageService.class);
        service.setSelf(service);
    }
}
