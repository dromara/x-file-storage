package org.dromara.x.file.storage.solon;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import lombok.extern.slf4j.Slf4j;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.noear.solon.annotation.Import;
import org.noear.solon.annotation.Inject;
import org.noear.solon.test.SolonJUnit5Extension;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


@Slf4j
@Import(profiles = "classpath:app.yml", scanPackages = "org.dromara")
@ExtendWith(SolonJUnit5Extension.class)
public class FileStorageAutoConfigurationTest {

  @Inject
  private SolonFileStorageProperties solonFileStorageProperties;

  @Inject
  private FileStorageService fileStorageService;

  /** 测试读取配置文件 */
  @Test
  public void testPropLoad() {

    // 断言配置文件读取成功
    assertNotNull(solonFileStorageProperties, "配置文件读取失败");


    // 断言属性为 minio-user 否则抛出异常 读取默认平台出错
    assertEquals("minio-user", solonFileStorageProperties.getDefaultPlatform(), "读取默认平台出错");

    // 断言 thumbnail-suffix 为 .min.jpg 否则抛出异常
    assertEquals(".min.jpg", solonFileStorageProperties.getThumbnailSuffix(), "读取缩略图后缀出错");

    // 断言 minio 配置有2个
    assertEquals(2, solonFileStorageProperties.getMinio().size(), "读取平台配置出错");

    // 断言 minio 的两个配置的值分别是 minio-user 和 minio-dept
    assertEquals("minio-user", solonFileStorageProperties.getMinio().get(0).getPlatform(), "读取平台配置出错");
    assertEquals("minio-dept", solonFileStorageProperties.getMinio().get(1).getPlatform(), "读取平台配置出错");

  }


  /**
   * 测试本地文件上传
   */
  @Test
  public void testLocalFileUpload() {

    // 生成一个本地文件用于测试上传
    File testFile = new File(FileUtil.getTmpDir(), RandomUtil.randomString(10) + ".txt");
    FileUtil.writeUtf8String("Hello World " + RandomUtil.randomString(100), testFile);

    FileInfo uploaded = null;
    try {


      uploaded = fileStorageService.of(testFile).setPlatform("local-plus-1").upload();

      assertEquals(FileUtil.size(testFile), uploaded.getSize(), "文件大小不一致");

    } finally {
      fileStorageService.delete(uploaded);
      // 测试完成后删除文件
      FileUtil.del(testFile);

    }


  }


  /**
   * 测试minio 文件上传
   */
  @Test
  public void testMinioFileUpload() {

    // 生成一个本地文件用于测试上传
    File testFile = new File(FileUtil.getTmpDir(), RandomUtil.randomString(10) + ".txt");
    FileUtil.writeUtf8String("Hello World " + RandomUtil.randomString(100), testFile);

    FileInfo uploaded = null;
    try {

      uploaded = fileStorageService.of(testFile).setPlatform("minio-user")
        .setSaveFilename(testFile.getName())
        .setPath("/user/user-id-1/目录1/目录2/目录3/")
        .putUserMetadata("userId_2", "u-123")
        .upload();

      assertEquals(FileUtil.size(testFile), uploaded.getSize(), "文件大小不一致");

    } finally {
      // 测试完成后删除文件
      fileStorageService.delete(uploaded);
      FileUtil.del(testFile);
    }

  }

}