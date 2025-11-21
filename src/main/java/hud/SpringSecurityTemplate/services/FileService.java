package hud.SpringSecurityTemplate.services;

import jakarta.activation.MimetypesFileTypeMap;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import hud.SpringSecurityTemplate.repositories.FileRepository;
import hud.SpringSecurityTemplate.utils.FileUtil;

import java.io.IOException;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.nio.file.Path;
import java.util.Base64;

@Service
public class FileService {

    private final FileUtil fileUtil;
    private final FileRepository fileRepository;

    public FileService(FileUtil fileUtil, FileRepository fileRepository) {
        this.fileUtil = fileUtil;
        this.fileRepository = fileRepository;
    }

    public ResponseEntity<String> getFile (String filename){

        String path = "/file/" + filename;
        var fileUpload = fileRepository.findByPath(path);

        if (fileUpload.isPresent()) {
            return buildFileResponse(Path.of(fileUpload.get().getServerPath()), filename);
        }

        return ResponseEntity.notFound().build();
    }

    public String uploadFile(MultipartFile file) throws IOException {
        return fileUtil.saveFile(file).getPath();
    }

    public String uploadBase64File(String base64Data) throws IOException {
        return fileUtil.saveBase64File(base64Data).getPath();
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
        }
        catch (IOException ex) {
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
