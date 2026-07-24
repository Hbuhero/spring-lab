package hud.SpringSecurityTemplate.services;

import hud.SpringSecurityTemplate.models.FileUploadSession;
import hud.SpringSecurityTemplate.payloads.responses.Message;
import jakarta.activation.MimetypesFileTypeMap;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.multipart.MultipartFile;
import hud.SpringSecurityTemplate.repositories.FileRepository;
import hud.SpringSecurityTemplate.utils.FileUtil;
import hud.SpringSecurityTemplate.payloads.requests.FileUploadDto;

import java.io.IOException;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class FileService {
    private static final HashMap<String, String> EXTENSION_TO_MIME_TYPE = new HashMap<>() {{
        put("png", "image/png");
        put("jpg", "image/jpg");
        put("jpeg", "image/jpeg");
        put("svg", "image/svg+xml");
        put("pdf", "application/pdf");
        put("apk", "application/vnd.android.package-archive");
    }};
    private final Logger logger = LogManager.getLogger(this.getClass());
    private final Map<String, FileUploadSession> sessions = new ConcurrentHashMap<>();
    private final FileUtil fileUtil;
    private final FileRepository fileRepository;

    public FileService(FileUtil fileUtil, FileRepository fileRepository) {
        this.fileUtil = fileUtil;
        this.fileRepository = fileRepository;
    }

    public ResponseEntity<String> getFile(String filename) {

        String path = "/files?filename=" + filename;
        var fileUpload = fileRepository.findByPath(path);

        if (fileUpload.isPresent()) {
            return buildFileResponse(Path.of(fileUpload.get().getServerPath()), filename);
        }

        return ResponseEntity.notFound().build();
    }

    public ResponseEntity<?> getFileMultipart(String filename) throws IOException {

        String path = "/file/" + filename;
        var fileUpload = fileRepository.findByPath(path);

        if (fileUpload.isPresent()) {
            InputStreamResource resource = new InputStreamResource(
                    Files.newInputStream(
                            Paths.get(fileUpload.get().getServerPath())
                    )
            );

            return ResponseEntity.ok()
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + fileUpload.get().getOriginalFileName() + "\""
                    )
                    .contentType(
                            MediaType.parseMediaType(
                                    EXTENSION_TO_MIME_TYPE.get(FilenameUtils.getExtension(fileUpload.get().getFileName()))
                            )
                    )
                    .contentLength(fileUpload.get().getFileSize())
                    .body(resource);
        }

        return ResponseEntity.notFound().build();
    }

    public String uploadFile(MultipartFile file) throws IOException {
        return fileUtil.saveFile(file).getPath();
    }

    public String uploadBase64File(String base64Data) throws IOException {
        return fileUtil.saveBase64File(base64Data).getPath();
    }

    public String uploadBase64File(FileUploadDto.FileUploadRequest request) throws IOException {
        return fileUtil.saveBase64File(request.getFileName(), request.getBase64Data()).getPath();
    }

    public ResponseEntity<?> initialiseChunkUpload(FileUploadDto.InitUploadRequest request) {
        try {
            fileUtil.validateFileExtension(request.getFileName());

            String uploadId = UUID.randomUUID().toString();

            // Sanitize: strip path separators, replace unsafe chars
            String safeFileName = fileUtil.sanitizeFilename(request.getFileName());
            Path tempPath = fileUtil.resolveTemporaryPath(uploadId + "_" + safeFileName);

            sessions.put(uploadId, new FileUploadSession(
                    uploadId,
                    request.getFileName(), // ✅ keep original client name separately
                    safeFileName, // ✅ server-safe name
                    request.getTotalSize(),
                    tempPath));

            logger.info("Initialized upload session [{}] for file [{}] (safe: [{}])",
                    uploadId, request.getFileName(), safeFileName);
            return ResponseEntity.ok(new FileUploadDto.UploadInitResponse(uploadId));

        } catch (IOException e) {
            logger.error("Failed to initialize upload session: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(new Message("Failed to initialize upload session with root cause: " + e.getMessage()));
        }
    }

    public ResponseEntity<?> uploadChunk(String uploadId, String contentRange, HttpServletRequest request) {
        logger.info("Receiving chunk for session [{}]", uploadId);

        FileUploadSession session = sessions.get(uploadId);
        if (session == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new Message("Upload session not found"));
        }

        try {
            // Parse Content-Range: bytes {start}-{end}/{total}
            String[] parts = contentRange.replace("bytes ", "").split("[/-]");
            long start = Long.parseLong(parts[0]);
            // parts[1] is the range end — implicit from actualWritten; not stored
            // separately
            long total = Long.parseLong(parts[2]);

            // ✅ NIO streaming — no readAllBytes() heap copy
            long actualWritten = fileUtil.writeChunkNio(
                    request.getInputStream(), session.getTempPath(), start);

            // ✅ Count actual bytes written, not the client-supplied Content-Length
            session.addBytesReceived(actualWritten);
            long totalReceived = session.getBytesReceived();

            logger.info("Written {} bytes at offset {} for session [{}]", actualWritten, start, uploadId);

            // All bytes received → finalize
            if (totalReceived >= total) {
                String filePath = fileUtil.finalizeUpload(session, uploadId, total);
                sessions.remove(uploadId);
                return new ResponseEntity<>(filePath, HttpStatus.CREATED);
            }

            // ✅ 202 Accepted — not 308 Permanent Redirect
            // ✅ X-Bytes-Received header — not the Range header
            return ResponseEntity.accepted()
                    .header("X-Bytes-Received", String.valueOf(totalReceived))
                    .body(new Message("Chunk received"));

        } catch (Exception e) {
            logger.error("Failed to write chunk for session [{}]: {}", uploadId, e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(new Message("Failed to write chunk"));
        }
    }

    public ResponseEntity<?> cancelUpload(String uploadId) {
        FileUploadSession session = sessions.remove(uploadId);
        if (session == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new Message("Upload session not found"));
        }
        try {
            Files.deleteIfExists(session.getTempPath());
            logger.info("Cancelled upload session [{}]", uploadId);
            return ResponseEntity.noContent().build();
        } catch (IOException e) {
            logger.error("Failed to delete temp file for session [{}]: {}", uploadId, e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(new Message("Failed to cancel upload"));
        }
    }

    public ResponseEntity<String> buildFileResponse(Path path, String filename) {
        try {
            byte[] fileBytes = fileUtil.getFileForPath(path);

            if (fileBytes == null) {
                return ResponseEntity.notFound().build();
            }

            String file = Base64.getEncoder().encodeToString(fileBytes);

            HttpHeaders headers = new HttpHeaders();

            // attachment will force the user to download the file
            String lowerCaseFilename = filename.toLowerCase();
            String contentType = lowerCaseFilename.endsWith("htm") || lowerCaseFilename.endsWith("html") || lowerCaseFilename.endsWith("svg") || lowerCaseFilename.endsWith("svgz")
                    ? "attachment"
                    : "inline";
            headers.setContentDisposition(ContentDisposition.builder(contentType).build());


            var response = ResponseEntity.ok().headers(headers).contentType(getMediaTypeFromFilename(filename)).header("filename", filename);

            return response.body(file);
        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    public MediaType getMediaTypeFromFilename(String filename) {
        FileNameMap fileNameMap = URLConnection.getFileNameMap();
        String mimeType = fileNameMap.getContentTypeFor(filename);
        if (mimeType != null) {
            return MediaType.parseMediaType(mimeType);
        }
        MimetypesFileTypeMap fileTypeMap = new MimetypesFileTypeMap();

        return MediaType.parseMediaType(fileTypeMap.getContentType(filename));
    }
}
