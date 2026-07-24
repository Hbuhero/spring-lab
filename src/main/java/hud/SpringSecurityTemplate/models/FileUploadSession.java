package hud.SpringSecurityTemplate.models;

import lombok.Data;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;

@Data
public class FileUploadSession {

    private final String uploadId;

    private final String originalFileName;

    private final String safeFileName;

    private final long totalSize;
    private final Path tempPath;

    /**
     * Running total of bytes actually written to disk.
     * Uses AtomicLong so concurrent PATCH requests are safe.
     */
    private final AtomicLong bytesReceived = new AtomicLong(0);

    public FileUploadSession(String uploadId, String originalFileName,
                             String safeFileName, long totalSize, Path tempPath) {
        this.uploadId = uploadId;
        this.originalFileName = originalFileName;
        this.safeFileName = safeFileName;
        this.totalSize = totalSize;
        this.tempPath = tempPath;
    }

    public void addBytesReceived(long bytes) {
        bytesReceived.addAndGet(bytes);
    }

    public long getBytesReceived() {
        return bytesReceived.get();
    }
}
