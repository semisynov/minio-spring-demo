package ru.minio.spring.demo.service;

import io.minio.*;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.DefaultUriBuilderFactory;
import ru.minio.spring.demo.config.ApiConstants;
import ru.minio.spring.demo.dto.FileDto;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class MinioService {
    private final MinioClient minioClient;
    private final String bucketName;
    private final Environment environment;

    public MinioService(MinioClient minioClient,
                        Environment environment,
                        @Value("${minio.bucket.name}") String bucketName) {
        this.minioClient = minioClient;
        this.bucketName = bucketName;
        this.environment = environment;
    }

    public List<FileDto> getListObjects() {
        var objects = new ArrayList<FileDto>();
        try {
            var result = minioClient.listObjects(ListObjectsArgs.builder()
                    .bucket(bucketName)
                    .recursive(true)
                    .includeVersions(true)
                    .build());
            for (Result<Item> item : result) {
                objects.add(FileDto.builder()
                        .filename(item.get().objectName())
                        .size(item.get().size())
                        .url(getFileUrl(item.get().objectName(), item.get().versionId()))
                        .versionId(item.get().versionId())
                        .build());
            }
            return objects;
        } catch (Exception e) {
            log.error("Happened error when get list objects from minio: ", e);
        }
        return objects;
    }

    public FileDto uploadFile(MultipartFile file) {
        try {
            var response = minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(file.getOriginalFilename())
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .build());
            return FileDto.builder()
                    .size(file.getSize())
                    .url(getFileUrl(file.getOriginalFilename(), response.versionId()))
                    .filename(file.getOriginalFilename())
                    .build();
        } catch (Exception e) {
            log.error("Error while upload file: ", e);
            throw new RuntimeException(e);
        }
    }

    public byte[] getFile(String key, String versionId) {
        var request = GetObjectArgs.builder()
                .bucket(bucketName)
                .object(key)
                .versionId(versionId)
                .build();
        try (var response = minioClient.getObject(request)) {
            return response.readAllBytes();
        } catch (Exception e) {
            log.error("Error while get file: ", e);
            throw new RuntimeException(e);
        }
    }

    private String getFileUrl(String fileName, String versionId) {
        try {
            var port = Optional.ofNullable(environment.getProperty("server.port"))
                    .map(Integer::parseInt)
                    .orElse(-1);
            var hostAddress = InetAddress.getLocalHost().getHostAddress();

            var uri = new DefaultUriBuilderFactory().builder()
                    .scheme("http")
                    .host(hostAddress)
                    .port(port)
                    .pathSegment(ApiConstants.ROOT, ApiConstants.FILE_DOWNLOAD)
                    .queryParam("fileName", fileName)
                    .queryParam("versionId", versionId)
                    .build();
            return uri.toString();
        } catch (UnknownHostException e) {
            log.error("Error while get file url: ", e);
            throw new RuntimeException(e);
        }
    }
}
