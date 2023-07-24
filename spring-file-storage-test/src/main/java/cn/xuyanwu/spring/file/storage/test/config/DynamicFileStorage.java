package cn.xuyanwu.spring.file.storage.test.config;

import cn.xuyanwu.spring.file.storage.platform.LocalFileStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 动态存储平台设置
 */
@Component
public class DynamicFileStorage {
    @Autowired
    private List<LocalFileStorage> list;

    public void add(){
        //TODO 读取数据库配置
        LocalFileStorage localFileStorage = new LocalFileStorage();
        localFileStorage.setPlatform("my-local-1");//平台名称
        localFileStorage.setBasePath("");
        localFileStorage.setDomain("");
        list.add(localFileStorage);
    }

    public void remove(String platform){
        for (LocalFileStorage localFileStorage : list) {

        }
    }
}
