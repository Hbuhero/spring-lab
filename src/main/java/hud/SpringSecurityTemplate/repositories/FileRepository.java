package hud.SpringSecurityTemplate.repositories;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import hud.SpringSecurityTemplate.models.FileUpload;

import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<FileUpload, Long> {

    Optional<FileUpload> findByPath(String path);
}
