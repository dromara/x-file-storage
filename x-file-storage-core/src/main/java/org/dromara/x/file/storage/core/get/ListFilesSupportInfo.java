package org.dromara.x.file.storage.core.get;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 列举文件支持信息
 */
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class ListFilesSupportInfo {
    /**
     * 是否支持列举文件，正常情况下判断此参数就行了
     */
    private Boolean isSupport;

    /**
     * 每次获取的最大文件数，对象存储一般是 1000
     */
    private Integer supportMaxFiles;

    /**
     * 不支持列举文件
     */
    public static ListFilesSupportInfo notSupport() {
        return new ListFilesSupportInfo(false, null);
    }

    /**
     * 支持全部的列举文件功能
     */
    public static ListFilesSupportInfo supportAll() {
        return new ListFilesSupportInfo(true, 1000);
    }
}
