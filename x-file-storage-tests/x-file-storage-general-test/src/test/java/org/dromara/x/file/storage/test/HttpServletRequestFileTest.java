package org.dromara.x.file.storage.test;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.http.HttpUtil;
import java.io.File;
import java.util.LinkedHashMap;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 对支持直接读取 HttpServletRequest 的流进行上传的功能进行测试
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class HttpServletRequestFileTest {
    private final File file;
    private final File thfile;

    public HttpServletRequestFileTest() {
        file = new File(System.getProperty("java.io.tmpdir"), "image.jpg");
        if (!file.exists()) {
            FileUtil.writeFromStream(this.getClass().getClassLoader().getResourceAsStream("image.jpg"), file);
        }
        thfile = new File(System.getProperty("java.io.tmpdir"), "image2.jpg");
        if (!thfile.exists()) {
            FileUtil.writeFromStream(this.getClass().getClassLoader().getResourceAsStream("image2.jpg"), thfile);
        }
    }

    /**
     * 单独对文件上传进行测试
     */
    @Test
    public void upload() {

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("aaa", "111");
        map.put("bbb", "222");
        map.put("ccc", "");
        map.put("ddd", null);
        //        map.put("_fileSize",file.length());
        map.put("_hasTh", "true");
        map.put("thfile", thfile);
        map.put("file", file);
        String res = HttpUtil.post("http://localhost:8030/upload-request", map);
        System.out.println("文件上传结果：" + res);
        Assert.isTrue(res.startsWith("{") && res.contains("url"), "文件上传失败！");
    }
}
