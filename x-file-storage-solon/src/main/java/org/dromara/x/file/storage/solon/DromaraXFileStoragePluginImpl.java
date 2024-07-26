package org.dromara.x.file.storage.solon;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.core.AppContext;
import org.noear.solon.core.Plugin;
import org.noear.solon.web.staticfiles.StaticMappings;
import org.noear.solon.web.staticfiles.repository.FileStaticRepository;

/**
 * 启动时 注册本地存储的访问地址
 * @author link2fun
 */
@Slf4j
public class DromaraXFileStoragePluginImpl implements Plugin {
    /**
     * 启动（保留，为兼容性过度）
     *
     * @param context 应用上下文
     */
    @Override
    public void start(final AppContext context) {

        // 配置包扫描, 扫描当前插件包下的所有类
        context.beanScan(FileStorageAutoConfiguration.class);

        // 通过插件 配置本地存储的访问地址
        context.getBeanAsync(SolonFileStorageProperties.class, cfg -> {

            // local 配置
            //noinspection deprecation
            final List<? extends SolonFileStorageProperties.SolonLocalConfig> localList = cfg.getLocal();
            //noinspection deprecation
            for (final SolonFileStorageProperties.SolonLocalConfig localConfig : localList) {

                if (!localConfig.getEnableStorage() || !localConfig.getEnableAccess()) {
                    // 没有启用存储或 不允许访问, 则不配置映射
                    continue;
                }

                // 添加本地绝对目录（例：/img/logo.jpg 映射地址为：/data/sss/app/img/logo.jpg）
                StaticMappings.add(localConfig.getPathPatterns(), new FileStaticRepository(localConfig.getBasePath()));
                log.info(
                        "[x-file-storage] [local] 添加本地存储映射: {} -> {}",
                        localConfig.getPathPatterns(),
                        localConfig.getBasePath());
            }

            // localPlus 配置
            final List<? extends SolonFileStorageProperties.SolonLocalPlusConfig> localPlusList = cfg.getLocalPlus();
            for (final SolonFileStorageProperties.SolonLocalPlusConfig localPlusConfig : localPlusList) {

                if (!localPlusConfig.getEnableStorage() || !localPlusConfig.getEnableAccess()) {
                    // 没有启用存储或 不允许访问, 则不配置映射
                    continue;
                }

                // 添加本地绝对目录（例：/img/logo.jpg 映射地址为：/data/sss/app/img/logo.jpg）
                StaticMappings.add(
                        localPlusConfig.getPathPatterns(), new FileStaticRepository(localPlusConfig.getStoragePath()));
                log.info(
                        "[x-file-storage] [localPlus] 添加本地存储映射: {} -> {}",
                        localPlusConfig.getPathPatterns(),
                        localPlusConfig.getStoragePath());
            }
        });
    }
}
