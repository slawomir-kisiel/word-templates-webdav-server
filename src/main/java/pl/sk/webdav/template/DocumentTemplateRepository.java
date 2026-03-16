package pl.sk.webdav.template;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentTemplateRepository extends JpaRepository<DocumentTemplate, UUID> {
  List<DocumentTemplate> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
      String name, String description, Sort sort);
}
