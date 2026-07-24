package hud.SpringSecurityTemplate.controllers;

import hud.SpringSecurityTemplate.payloads.requests.FileUploadDto;
import hud.SpringSecurityTemplate.services.FileService;
import hud.SpringSecurityTemplate.utils.Constants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;



@Controller
@RequestMapping(Constants.API_V1 + "/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @GetMapping("/file/{filename}")
    public ResponseEntity<?> getFile(@PathVariable("filename") String filename) throws IOException {
        return fileService.getFileMultipart(filename);
    }

    @GetMapping("/base64/{filename}")
    public ResponseEntity<String> getFileBase64(@PathVariable("filename") String filename) {
        return fileService.getFile(filename);
    }

    @PostMapping(name = "/upload/multipart", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadMultipartFile(@RequestParam("file") MultipartFile file) throws Exception {
        return ResponseEntity.ok(fileService.uploadFile(file));
    }

    @PostMapping("/upload/base64")
    public ResponseEntity<?> uploadBase64File(@Valid @RequestBody FileUploadDto.FileUploadRequest request) throws Exception {
        return ResponseEntity.ok(fileService.uploadBase64File(request));
    }

    @PostMapping("/chunk-upload/init")
    public ResponseEntity<?> initialiseChunkUpload(@RequestBody FileUploadDto.InitUploadRequest request) {
       return fileService.initialiseChunkUpload(request);
    }

    @PatchMapping("/upload/chunk/{uploadId}")
    public ResponseEntity<?> uploadChunk(
            @PathVariable String uploadId,
            @RequestHeader("Content-Range") String contentRange,
            HttpServletRequest request
    ) {
        return fileService.uploadChunk(uploadId, contentRange, request);
    }

    @DeleteMapping("/upload/{uploadId}")
    public ResponseEntity<?> cancelUpload(@PathVariable String uploadId) {
        return fileService.cancelUpload(uploadId);
    }
}
