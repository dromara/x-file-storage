package org.dromara.x.file.storage.core.hash;

import static org.dromara.x.file.storage.core.constant.Constant.Hash.MessageDigest.*;

import cn.hutool.core.map.CaseInsensitiveLinkedMap;
import java.util.Map;
import lombok.NoArgsConstructor;

/**
 * 哈希信息类，用来存储各种哈希值，例如 MD5、SHA1、SHA256等，详情{@link org.dromara.x.file.storage.core.constant.Constant.Hash}
 */
@NoArgsConstructor
public class HashInfo extends CaseInsensitiveLinkedMap<String, String> {

    /**
     * 构造方法
     */
    public HashInfo(Map<String, String> map) {
        super(map);
    }

    /**
     * 获取 MD2
     */
    public String getMd2() {
        return get(MD2);
    }

    /**
     * 获取 MD5
     */
    public String getMd5() {
        return get(MD5);
    }

    /**
     * 获取 SHA1
     */
    public String getSha1() {
        return get(SHA1);
    }

    /**
     * 获取 SHA256
     */
    public String getSha256() {
        return get(SHA256);
    }

    /**
     * 获取 SHA384
     */
    public String getSha384() {
        return get(SHA384);
    }

    /**
     * 获取 SHA512
     */
    public String getSha512() {
        return get(SHA512);
    }
}
