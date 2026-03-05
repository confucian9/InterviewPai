package com.interviewpai.service;

import io.minio.*;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class MinioService {
    
    private final MinioClient minioClient;
    
    @Value("${minio.bucket}")
    private String bucket;
    
    @Value("${minio.presignExpirySeconds}")
    private Integer presignExpirySeconds;
    
    public MinioService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }
    
    public void createBucketIfNotExists() {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(bucket)
                    .build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(bucket)
                        .build());
            }
        } catch (Exception e) {
            throw new RuntimeException("创建存储桶失败: " + e.getMessage());
        }
    }
    
    public String uploadFile(MultipartFile file, String folder) {
        try {
            createBucketIfNotExists();
            
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String objectName = folder + "/" + datePath + "/" + UUID.randomUUID() + extension;
            
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
            
            return objectName;
        } catch (Exception e) {
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }
    
    public String uploadText(String content, String folder, String filename) {
        try {
            createBucketIfNotExists();
            
            String objectName = folder + "/" + filename;
            
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(new java.io.ByteArrayInputStream(content.getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                            content.getBytes().length, -1)
                    .contentType("text/plain")
                    .build());
            
            return objectName;
        } catch (Exception e) {
            throw new RuntimeException("文本上传失败: " + e.getMessage());
        }
    }
    
    public String uploadJson(String content, String folder, String filename) {
        try {
            createBucketIfNotExists();
            
            String objectName = folder + "/" + filename;
            
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(new java.io.ByteArrayInputStream(content.getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                            content.getBytes().length, -1)
                    .contentType("application/json")
                    .build());
            
            return objectName;
        } catch (Exception e) {
            throw new RuntimeException("JSON上传失败: " + e.getMessage());
        }
    }
    
    public String getPresignedUrl(String objectName) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucket)
                    .object(objectName)
                    .expiry(presignExpirySeconds, TimeUnit.SECONDS)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("获取预签名URL失败: " + e.getMessage());
        }
    }
    
    public InputStream getFile(String objectName) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("获取文件失败: " + e.getMessage());
        }
    }
    
    public String getFileContent(String objectName) {
        try (InputStream inputStream = getFile(objectName)) {
            return new String(inputStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("读取文件内容失败: " + e.getMessage());
        }
    }
    
    public void deleteFile(String objectName) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("删除文件失败: " + e.getMessage());
        }
    }
}
