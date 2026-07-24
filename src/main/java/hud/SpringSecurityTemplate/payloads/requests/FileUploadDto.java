package hud.SpringSecurityTemplate.payloads.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;

public class FileUploadDto {

    // ── Requests ──────────────────────────────────────────────────────────────

    @Data
    public static class InitUploadRequest {
        private String fileName;
        private long totalSize;
    }

    // ── Responses ─────────────────────────────────────────────────────────────

    @Data
    @AllArgsConstructor
    public static class UploadInitResponse {
        private String uploadId;
    }

    @Data
    public static class FileUploadRequest {
        @NotBlank
        private String fileName;

        @NotBlank
        private String base64Data;

    }
}
