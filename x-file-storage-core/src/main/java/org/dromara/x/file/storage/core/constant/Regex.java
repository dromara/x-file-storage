package org.dromara.x.file.storage.core.constant;

/**
 * There is no description.
 *
 * @author XS <wanghaiqi@beeplay123.com>
 * @version 1.0
 * @date 2023/10/24 15:05
 */
public interface Regex {

    /**
     * IP:PORT
     */
    String IP_COLON_PORT =
            "^.*:(?:[1-9]\\d{0,3}|[1-5]\\d{4}|[1-5][0-9]{0,3}\\d{0,3}|6[0-4]\\d{0,3}|65[0-4]\\d{0,2}|655[0-2]\\d?)$";

    /**
     * IP1:PORT1,IP2:PORT2
     */
    String IP_COLON_PORT_COMMA = "^(.*?):\\d+(?:,(.*?):\\d+)*$";
}
