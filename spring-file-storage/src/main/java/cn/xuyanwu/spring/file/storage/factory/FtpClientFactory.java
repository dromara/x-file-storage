package cn.xuyanwu.spring.file.storage.factory;

import cn.xuyanwu.spring.file.storage.FileStorageProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.tika.utils.StringUtils;

import java.io.IOException;

@Slf4j
public class FtpClientFactory extends BasePooledObjectFactory<FTPClient> {
    private final FileStorageProperties.FTP config;
    private String LOCAL_CHARSET= "UTF-8";

    @Override
    public FTPClient create() throws Exception {
        FTPClient ftpClient = new FTPClient();
        ftpClient.setConnectTimeout(config.getConnectionTimeout());
        log.info("连接ftp服务器:{}:{}", config.getHost(), config.getPort());
        ftpClient.connect(config.getHost(), config.getPort());
        ftpClient.setCharset(config.getCharset());
        int reply = ftpClient.getReplyCode();
        if (!FTPReply.isPositiveCompletion(reply)) {
            log.info("连接失败ftp服务器:{}:{}", config.getHost(), config.getPort());
            ftpClient.disconnect();
            return null;
        }
        ftpClient.setSoTimeout(config.getSoTimeout());

        if (!FTPReply.isPositiveCompletion(ftpClient.sendCommand(
                "OPTS UTF8", "ON"))){
            LOCAL_CHARSET = config.getCharset().name();
        }
        ftpClient.setControlEncoding(LOCAL_CHARSET);
        if (config.getIsActive()) {
            ftpClient.enterLocalPassiveMode();
        }

        boolean success;
        if (StringUtils.isBlank(config.getUser())) {
            success = ftpClient.login("anonymous", "anonymous");
        } else {
            success = ftpClient.login(config.getUser(), config.getPassword());
        }
        if (!success) {
            log.info("登录ftp服务器失败:{}:{}", config.getUser(), config.getPassword());
            return null;
        }
        log.info("FTP服务器 >>>>>>> 连接成功");
        ftpClient.setFileType(config.getTransferFileType());
        ftpClient.setBufferSize(1024);
        log.debug("创建ftp连接");
        return ftpClient;
    }

    @Override
    public PooledObject<FTPClient> wrap(FTPClient ftpClient) {
        return new DefaultPooledObject<>(ftpClient);
    }

    public FtpClientFactory(FileStorageProperties.FTP config) {
        this.config = config;
    }

    @Override
    public void destroyObject(PooledObject<FTPClient> pool) {
        FTPClient ftpClient = pool.getObject();
        if (ftpClient != null) {
            try {
                ftpClient.disconnect();
                log.debug("销毁ftp连接");
            } catch (Exception e) {
                log.error("销毁ftpClient异常，error：{}", e.getMessage());
            }
        }
    }

    @Override
    public boolean validateObject(PooledObject<FTPClient> pool) {
        FTPClient ftpClient = pool.getObject();
        try {
            return ftpClient != null && ftpClient.sendNoOp();
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public void activateObject(PooledObject<FTPClient> pool) throws Exception {
        FTPClient client = pool.getObject();
        if (!client.isConnected()) {
            boolean success;
            if (StringUtils.isBlank(config.getUser())) {
                success = client.login("anonymous", "anonymous");
            } else {
                success = client.login(config.getUser(), config.getPassword());
            }
            if (!success) {
                log.info("登录ftp服务器失败:{}:{}", config.getUser(), config.getPassword());
            }
        }
    }
}

