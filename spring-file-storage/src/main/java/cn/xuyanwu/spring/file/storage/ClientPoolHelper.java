package cn.xuyanwu.spring.file.storage;

import com.google.common.cache.LoadingCache;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPool;

import java.util.function.Consumer;
import java.util.function.Function;

@Setter
@Slf4j
public class ClientPoolHelper<T,R> {
    private final LoadingCache<String,GenericObjectPool<T>> caches;
    public ClientPoolHelper(LoadingCache<String, GenericObjectPool<T>> caches) {
        this.caches = caches;
    }

    public R runOnce(String platform,Function<T,R> action) {
        T client = null;
        GenericObjectPool<T> pool = null;
        R r = null;
        try {
            pool = caches.get(platform);
            if(pool != null) {
                client = pool.borrowObject(pool.getMaxBorrowWaitTimeMillis());
                r = action.apply(client);
            }
        } catch (Exception e) {
            log.error("连接池执行任务异常",e);
        }finally {
            if(pool != null) {
                pool.returnObject(client);
            }
        }
        return r;
    }
    public R runOnce(Function<T,R> action,String platform) {
        return runOnce(platform,action);
    }
    public void runOnce(String platform,Consumer<T> action) {
        T client = null;
        GenericObjectPool<T> pool = null;
        try {
            pool = caches.get(platform);
            if(pool != null) {
                client = pool.borrowObject(pool.getMaxBorrowWaitTimeMillis());
                action.accept(client);
            }
        } catch (Exception e) {
            log.error("连接池执行任务异常",e);
        }finally {
            if(pool != null) {
                pool.returnObject(client);
            }
        }
    }

    public void runOnce(Consumer<T> action,String platform) {
        runOnce(platform,action);
    }
}
