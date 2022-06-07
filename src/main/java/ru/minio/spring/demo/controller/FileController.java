package ru.minio.spring.demo.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.minio.spring.demo.config.ApiConstants;
import ru.minio.spring.demo.service.MinioService;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = ApiConstants.ROOT)
public class FileController {

    private final MinioService minioService;

    @GetMapping
    public ResponseEntity<Object> getFiles() {
        return ResponseEntity.ok(minioService.getListObjects());
    }

    @PostMapping(value = ApiConstants.FILE_UPLOAD)
    public ResponseEntity<Object> upload(@RequestParam(value = "file") MultipartFile file) {
        return ResponseEntity.ok().body(minioService.uploadFile(file));
    }

    @GetMapping(path = ApiConstants.FILE_DOWNLOAD)
    public ResponseEntity<ByteArrayResource> downloadFile(@RequestParam(value = "fileName") String fileName,
                                                          @RequestParam(value = "versionId") String versionId) {
        var data = minioService.getFile(fileName, versionId);
        var resource = new ByteArrayResource(data);

        return ResponseEntity
                .ok()
                .contentLength(data.length)
                .header("Content-type", "application/octet-stream")
                .header("Content-disposition", "attachment; filename=\"" + fileName + "\"")
                .body(resource);
    }
}
