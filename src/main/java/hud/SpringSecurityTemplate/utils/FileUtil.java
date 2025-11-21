package hud.SpringSecurityTemplate.utils;


import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import hud.SpringSecurityTemplate.models.FileUpload;
import hud.SpringSecurityTemplate.repositories.FileRepository;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;

@Service
public class FileUtil {

    @Value("${application.fileUploadPath}")
    private Path basePath;

    private final FileRepository fileRepository;
    private static final Set<String> allowedFileExtensions = Set.of("png", "jpg", "jpeg", "svg");

    public FileUtil(FileRepository fileRepository) {
        this.fileRepository = fileRepository;
    }

    public FileUpload saveFile( MultipartFile file) throws IOException {
        String filename = sanitizeFilename(file.getOriginalFilename());

        validateFileExtension(filename);

        String updatedFilename = LocalDateTime.now() + "_" + UUID.randomUUID();
        Path fileServerPath = resolvePath(updatedFilename);

        copyFile(file, fileServerPath);

        String path = URI.create("/file/" + "/").resolve(updatedFilename).toString();

        return fileRepository.save(
                FileUpload.builder()
                        .fileName(updatedFilename)
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

        String path = URI.create("/file/" + "/").resolve(updatedFilename).toString();

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

    public void validateFileExtension(String filename) throws FileUploadException {
        final String fileExtension = FilenameUtils.getExtension(filename);

        if (allowedFileExtensions.stream().noneMatch(fileExtension::equalsIgnoreCase)){
            throw new FileUploadException("Invalid file extension: " + fileExtension);
        }

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

    public byte[] getFileForPath(Path path) throws IOException {
        if (Files.exists(path)) {
            return Files.readAllBytes(path);
        }
        return null;
    }

    public void setFileStatusIsDeleted(FileUpload fileUpload) {
        fileRepository.delete(fileUpload);
    }
}
