package pl.sk.webdav.template;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentTemplateService {
  private final DocumentTemplateRepository repository;
  private final Path storagePath;

  public DocumentTemplateService(
      DocumentTemplateRepository repository,
      @Value("${app.templates.storage-path}") String storagePath) {
    this.repository = repository;
    this.storagePath = Path.of(storagePath);
  }

  public List<DocumentTemplate> list(String query, String sortField, String sortDir) {
    Sort sort = buildSort(sortField, sortDir);
    if (query == null || query.isBlank()) {
      return repository.findAll(sort);
    }
    String normalized = query.trim();
    return repository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
        normalized, normalized, sort);
  }

  public DocumentTemplate create(String name, String description, MultipartFile file)
      throws IOException {
    UUID id = UUID.randomUUID();
    Files.createDirectories(storagePath);
    Path destination = storagePath.resolve(id + ".docx");
    Files.copy(file.getInputStream(), destination);
    DocumentTemplate template = new DocumentTemplate(id, name, description);
    return repository.save(template);
  }

  public void delete(UUID id) throws IOException {
    repository.deleteById(id);
    Path destination = storagePath.resolve(id + ".docx");
    Files.deleteIfExists(destination);
  }

  public Path resolvePath(UUID id) {
    return storagePath.resolve(id + ".docx");
  }

  public long getLastModified(UUID id) throws IOException {
    return Files.getLastModifiedTime(resolvePath(id)).toMillis();
  }

  private Sort buildSort(String sortField, String sortDir) {
    String safeField = "name";
    if (sortField != null) {
      String normalized = sortField.toLowerCase(Locale.ROOT);
      if (normalized.equals("description")) {
        safeField = "description";
      } else if (normalized.equals("name")) {
        safeField = "name";
      }
    }
    Sort.Direction direction =
        "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
    return Sort.by(direction, safeField);
  }
}
