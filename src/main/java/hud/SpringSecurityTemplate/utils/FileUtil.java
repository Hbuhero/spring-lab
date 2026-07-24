package hud.SpringSecurityTemplate.utils;


import hud.SpringSecurityTemplate.models.FileUploadSession;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import hud.SpringSecurityTemplate.models.FileUpload;
import hud.SpringSecurityTemplate.repositories.FileRepository;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class FileUtil {
    private static final Logger logger = LogManager.getLogger(FileUtil.class);
    private static final Set<String> ALLOWED_FILE_EXTENSIONS = Set.of("png", "jpg", "jpeg", "svg", "pdf", "apk");
    private static final Pattern BASE64_PATTERN = Pattern.compile("^data:([^;]+);base64,(.+)$", Pattern.DOTALL);
    private static final String FILE_PATH_PREFIX = "/file/";
    private static final String TEMPORARY_DIRECTORY = "/temp/";

    private static final HashMap<String, String> MIME_TYPE_TO_EXTENSION = new HashMap<>() {{
        put("image/png", "png");
        put("image/jpg", "jpg");
        put("image/jpeg", "jpeg");
        put("image/svg+xml", "svg");
        put("application/pdf", "pdf");
        put("application/vnd.android.package-archive", "apk");
    }};
    private final Map<String, FileUploadSession> sessions = new ConcurrentHashMap<>();

    @Value("${application.fileUploadPath}")
    private Path basePath;

    private final FileRepository fileRepository;

    public FileUtil(FileRepository fileRepository) {
        this.fileRepository = fileRepository;
    }

    public FileUpload saveFile(MultipartFile file) throws IOException {
        String filename = sanitizeFilename(file.getOriginalFilename());

        String fileExtension = validateFileExtension(filename);

        String updatedFilename = getServerFilename(fileExtension);
        Path fileServerPath = resolvePath(updatedFilename);

        copyFile(file, fileServerPath);

        String path = URI.create(FILE_PATH_PREFIX).resolve(updatedFilename).toString();

        return fileRepository.save(
                FileUpload.builder()
                        .fileName(updatedFilename)
                        .path(path)
                        .serverPath(fileServerPath.toString())
                        .originalFileName(file.getOriginalFilename())
                        .fileSize(file.getSize())
                        .build()
        );
    }

    public FileUpload saveBase64File(String filename, String dataUri) throws IOException {

        String sanitizedFilename = sanitizeFilename(filename);
        validateFileExtension(sanitizedFilename);

        Matcher matcher = BASE64_PATTERN.matcher(dataUri);

        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid Data URI format");
        }

        String base64Data = matcher.group(2);
        String mimeType = matcher.group(1);

        if (!MIME_TYPE_TO_EXTENSION.containsKey(mimeType)) {
            throw new IllegalArgumentException("Unsupported MIME type: " + mimeType);
        }

        byte[] decodedBytes = Base64.getDecoder().decode(base64Data);

        String updatedFilename = getServerFilename(MIME_TYPE_TO_EXTENSION.get(mimeType));
        Path fileServerPath = resolvePath(updatedFilename);

        copyBase64File(decodedBytes, fileServerPath);

        String path = URI.create(FILE_PATH_PREFIX).resolve(updatedFilename).toString();

        return fileRepository.save(
                FileUpload.builder()
                        .fileName(filename)
                        .originalFileName(sanitizedFilename)
                        .path(path)
                        .serverPath(fileServerPath.toString())
                        .build()
        );
    }

    public FileUpload saveBase64File(String base64Data) throws IOException {

        if (base64Data.contains(",")) {
            base64Data = base64Data.substring(base64Data.indexOf(",") + 1);
        }

        // Step 2: Decode Base64
        byte[] decodedBytes = Base64.getDecoder().decode(base64Data);

        String updatedFilename = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "_" + UUID.randomUUID() + ".png";
        Path fileServerPath = resolvePath(updatedFilename);

        copyBase64File(decodedBytes, fileServerPath);

        String path = URI.create(FILE_PATH_PREFIX).resolve(updatedFilename).toString();

        return fileRepository.save(
                FileUpload.builder()
                        .fileName(updatedFilename)
                        .path(path)
                        .serverPath(fileServerPath.toString())
                        .build()
        );
    }

    public String sanitizeFilename(String filename) throws FileUploadException {
        if (filename == null) {
            throw new FileUploadException("Filename cannot be null");
        }
        return filename.replaceAll("[^a-zA-Z\\d.\\-]", "_").replaceAll("\\.+", ".");
    }

    public String getServerFilename(String extension) {
        if (extension == null){
            throw new NullPointerException("File extension cant be null");
        }

        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "_" + UUID.randomUUID() + "." + extension;
    }

    public String validateFileExtension(String filename) throws FileUploadException {
        final String fileExtension = FilenameUtils.getExtension(filename);

        if (ALLOWED_FILE_EXTENSIONS.stream().noneMatch(fileExtension::equalsIgnoreCase)){
            throw new FileUploadException("Invalid file extension: " + fileExtension);
        }

        return fileExtension;
    }

    private void copyFile(MultipartFile file, Path filePath) throws IOException {
        try {

            FileUtils.copyInputStreamToFile(file.getInputStream(), filePath.toFile());
        }
        catch (IOException e) {
            throw new IOException("Could not create file");
        }
    }

    private void copyBase64File(byte[] bytes, Path filePath) throws IOException {
        try {

            FileUtils.writeByteArrayToFile(filePath.toFile(), bytes);
        }
        catch (IOException e) {
            throw new IOException("Could not create file");
        }
    }

    public Path resolvePath(String filename) throws IOException {

        if (!Files.exists(basePath)){
            Files.createDirectory(basePath);
        }

        return basePath.resolve(filename);
    }

    public Path resolveTemporaryPath(String filename) throws IOException {
        Path temporaryPath = Path.of(basePath + TEMPORARY_DIRECTORY);
        if (!Files.exists(temporaryPath)){
            Files.createDirectory(temporaryPath);
        }

        return temporaryPath.resolve(filename);
    }
    public byte[] getFileForPath(Path path) throws IOException {
        if (Files.exists(path)) {
            return Files.readAllBytes(path);
        }
        return null;
    }

    public String finalizeUpload(
            FileUploadSession session,
            String uploadId,
            long total
    ) throws IOException {

        // ✅ uploadId prefix namespaces the file — no two uploads can collide
        String serverFileName = getServerFilename(validateFileExtension(session.getSafeFileName()));
        Path permanentPath = resolvePath(serverFileName);

        // ✅ ATOMIC_MOVE — safe on same filesystem, no partial-file window
        FileUtils.moveFile(session.getTempPath().toFile(), permanentPath.toFile(), StandardCopyOption.ATOMIC_MOVE);

        FileUpload fileRecord = new FileUpload();
        fileRecord.setFileName(serverFileName); // ✅ server-side namespaced name
        fileRecord.setOriginalFileName(session.getOriginalFileName()); // ✅ client-provided name
        fileRecord.setPath(FILE_PATH_PREFIX + serverFileName);
        fileRecord.setFileSize(total);
        fileRecord.setCreatedAt(LocalDateTime.now());
        fileRecord.setServerPath(permanentPath.toString());
        FileUpload saved = fileRepository.save(fileRecord);


        logger.info("Upload complete for session [{}] → [{}]", uploadId, permanentPath);

        return saved.getPath();
    }

    /**
     * Stream the request body directly into the target file at {@code offset}
     * using NIO {@link FileChannel} + a 64 KB direct {@link ByteBuffer}.
     * <p>
     * This avoids materialising the entire chunk in the JVM heap
     * (which {@code inputStream.readAllBytes()} would cause).
     *
     * @return number of bytes actually written
     */
    public long writeChunkNio(
            InputStream inputStream,
            Path targetPath,
            long offset
    ) throws IOException {
        long written = 0L;
        try (FileChannel channel = FileChannel.open(targetPath,
                StandardOpenOption.WRITE, StandardOpenOption.CREATE);
             ReadableByteChannel src = Channels.newChannel(inputStream)) {

            channel.position(offset);
            ByteBuffer buf = ByteBuffer.allocateDirect(64 * 1024); // 64 KB direct buffer

            int read;
            while ((read = src.read(buf)) > 0) {
                buf.flip();
                while (buf.hasRemaining()) {
                    channel.write(buf); // handles partial writes
                }
                buf.clear();
                written += read;
            }
        }
        return written;
    }


    public void setFileStatusIsDeleted(FileUpload fileUpload) {
        fileRepository.delete(fileUpload);
    }
}
