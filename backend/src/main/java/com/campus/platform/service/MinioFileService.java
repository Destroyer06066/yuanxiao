package com.campus.platform.service;

import com.campus.platform.config.MinioConfig;
import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * MinIO 文件存储服务：
 * 用于存储导出文件（Excel 等）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinioFileService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    /**
     * 上传文件
     * @param directory 目录，如 "exports/2026/"
     * @param filename 文件名，如 "students.xlsx"
     * @param inputStream 文件流
     * @param contentType MIME 类型
     * @return 文件对象名（MinIO key）
     */
    public String upload(String directory, String filename, InputStream inputStream, String contentType) {
        try {
            String objectName = directory + UUID.randomUUID() + "_" + filename;
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucket())
                            .object(objectName)
                            .stream(inputStream, -1, 10485760) // 10MB 分片
                            .contentType(contentType)
                            .build()
            );
            log.info("文件上传成功: bucket={}, objectName={}", minioConfig.getBucket(), objectName);
            return objectName;
        } catch (Exception e) {
            log.error("MinIO 上传失败", e);
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成预签名下载链接
     */
    public String getPresignedDownloadUrl(String objectName) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(minioConfig.getBucket())
                            .object(objectName)
                            .expiry(1, TimeUnit.HOURS)
                            .build()
            );
        } catch (Exception e) {
            log.error("MinIO 生成下载链接失败", e);
            throw new RuntimeException("生成下载链接失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成预签名上传链接
     */
    public String getPresignedUploadUrl(String objectName) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(minioConfig.getBucket())
                            .object(objectName)
                            .expiry(30, TimeUnit.MINUTES)
                            .build()
            );
        } catch (Exception e) {
            log.error("MinIO 生成上传链接失败", e);
            throw new RuntimeException("生成上传链接失败: " + e.getMessage(), e);
        }
    }

    /**
     * 删除文件
     */
    public void delete(String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioConfig.getBucket())
                            .object(objectName)
                            .build()
            );
            log.info("文件删除成功: {}", objectName);
        } catch (Exception e) {
            log.error("MinIO 删除失败: {}", objectName, e);
        }
    }
}
