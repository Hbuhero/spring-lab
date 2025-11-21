package hud.SpringSecurityTemplate.controllers;

import hud.SpringSecurityTemplate.services.FileService;
import hud.SpringSecurityTemplate.utils.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
@RequestMapping(Constants.API_V1 + "/file")
public class FileController {
    @Autowired
    private FileService fileService;

    @GetMapping("/{filename}")
    public ResponseEntity<String> getFile(@PathVariable String filename) {
        return fileService.getFile(filename);
    }
}
