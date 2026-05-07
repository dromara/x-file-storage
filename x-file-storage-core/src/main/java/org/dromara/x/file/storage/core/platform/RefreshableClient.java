package org.dromara.x.file.storage.core.platform;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * 可自愈的 SDK 客户端包装：DCL 缓存 + JDK 动态代理拦截 + 失效自动重建
 *
 * <p>当代理客户端任意方法调用的异常链中出现 {@code isUnrecoverable} 判定为不可恢复的异常时，
 * 自动 shutdown 当前真实实例并失效缓存代理；下一次 {@link #get()} 通过 DCL 自动重建。
 * 用对象引用 {@code ==} 比对避免误伤已被其他线程重建的新实例。
 *
 * <p>典型场景：基于 Apache HttpClient 的 SDK 在进程触发过 OOM 后，内部
 * {@code PoolingHttpClientConnectionManager} 被永久 shutdown，可通过本包装类自动恢复。
 *
 * @param <Client> SDK 客户端类型，必须是 interface（JDK 动态代理前提）
 */
@Slf4j
@Getter
@Setter
@NoArgsConstructor
public class RefreshableClient<Client> implements AutoCloseable {
    /**
     * Client 类
     */
    private Class<Client> clientClass;
    /**
     * Client 创建接口
     */
    private Supplier<Client> clientSupplier;
    /**
     * Client 关闭接口，如 {@code OSS::shutdown}
     */
    private Consumer<Client> closeConsumer;
    /**
     * Client 是否已无法恢复的判断接口，遍历 cause 链是否命中"客户端已不可用"信号
     */
    private Predicate<Throwable> isUnrecoverablePredicate;
    /**
     * 失效日志注释获取接口
     */
    private Supplier<String> descriptionSupplier;
    /**
     * Client 的原始对象
     */
    private volatile Client raw;
    /**
     * Client 的代理对象
     */
    private volatile Client proxy;

    /**
     * @param clientClass              Client 类
     * @param clientSupplier           Client 创建接口
     * @param closeConsumer            Client 关闭接口，如 {@code OSS::shutdown}
     * @param isUnrecoverablePredicate Client 是否已无法恢复的判断接口，遍历 cause 链是否命中"客户端已不可用"信号
     * @param descriptionSupplier      失效日志注释获取接口
     */
    public RefreshableClient(
            Class<Client> clientClass,
            Supplier<Client> clientSupplier,
            Consumer<Client> closeConsumer,
            Predicate<Throwable> isUnrecoverablePredicate,
            Supplier<String> descriptionSupplier) {
        this.clientClass = clientClass;
        this.clientSupplier = clientSupplier;
        this.closeConsumer = closeConsumer;
        this.isUnrecoverablePredicate = isUnrecoverablePredicate;
        this.descriptionSupplier = descriptionSupplier;
    }

    /**
     * 获取代理客户端：首次调用时构建真实客户端并包代理（DCL 缓存）
     */
    public Client get() {
        if (proxy == null) {
            synchronized (this) {
                if (proxy == null) {
                    Client newRaw = clientSupplier.get();
                    raw = newRaw;
                    proxy = wrap(newRaw);
                }
            }
        }
        return proxy;
    }

    /**
     * 关闭真实客户端并清空缓存。再次调用 {@link #get()} 会重新构建一个新的实例
     */
    @Override
    public void close() {
        synchronized (this) {
            if (raw != null) {
                closeConsumer.accept(raw);
                raw = null;
                proxy = null;
            }
        }
    }

    /**
     * 仅当当前缓存的真实客户端仍是 suspected 实例时才 shutdown 并置 null，
     * 避免误伤已被其他线程重建的新实例
     */
    private void invalidate(Client suspected) {
        synchronized (this) {
            if (raw == suspected) {
                try {
                    closeConsumer.accept(suspected);
                } catch (Exception ignored) {
                    // 已失效的客户端重复 shutdown 可能再次抛异常，忽略
                }
                raw = null;
                proxy = null;
                log.warn("{} 已失效，已重置客户端，下次调用将自动重建", descriptionSupplier.get());
            }
        }
    }

    /**
     * 用 JDK 动态代理包装真实实例：
     * 每次方法调用 try-catch，命中 {@link #isUnrecoverablePredicate} 则触发 invalidate(target)
     * 然后原样抛出原异常（不吞、不包装、不重试）
     */
    private Client wrap(Client target) {
        return clientClass.cast(Proxy.newProxyInstance(
                clientClass.getClassLoader(), new Class<?>[] {clientClass}, (object, method, args) -> {
                    try {
                        return method.invoke(target, args);
                    } catch (InvocationTargetException ite) {
                        Throwable cause = ite.getTargetException();
                        if (isUnrecoverablePredicate.test(cause)) {
                            invalidate(target);
                        }
                        throw cause;
                    }
                }));
    }

    /**
     * 通用的 Apache HttpClient 4.x "Connection pool shut down" 信号识别器：
     * 遍历 cause 链查找 {@link IllegalStateException} 且消息包含 "Connection pool shut down"。
     *
     * <p>适用于所有基于 Apache HttpClient 的 SDK，例如阿里云 OSS、华为云 OBS、腾讯云 COS、
     * AWS S3 v1/v2 同步客户端、百度云 BOS、WebDAV(sardine) 等。
     */
    public static boolean isApacheHttpClientPoolShutdown(Throwable th) {
        for (Throwable cur = th; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalStateException
                    && cur.getMessage() != null
                    && cur.getMessage().contains("Connection pool shut down")) {
                return true;
            }
        }
        return false;
    }
}
