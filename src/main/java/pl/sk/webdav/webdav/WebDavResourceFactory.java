package pl.sk.webdav.webdav;

import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.ResourceFactory;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import java.nio.file.Path;
import org.springframework.core.env.Environment;
import pl.sk.webdav.template.DocumentTemplateRepository;

public class WebDavResourceFactory implements ResourceFactory {
  @Override
  public Resource getResource(String host, String path)
      throws NotAuthorizedException, BadRequestException {
    DocumentTemplateRepository repository =
        SpringContextProvider.getBean(DocumentTemplateRepository.class);
    Environment environment = SpringContextProvider.getBean(Environment.class);
    String storage = environment.getProperty("app.templates.storage-path", "./data/templates");
    Path storagePath = java.nio.file.Paths.get(storage);

    String normalized = path == null ? "" : path.trim();
    if (normalized.startsWith("/")) {
      normalized = normalized.substring(1);
    }
    if (normalized.startsWith("webdav/")) {
      normalized = normalized.substring("webdav/".length());
    } else if ("webdav".equals(normalized)) {
      normalized = "";
    }
    if (normalized.trim().isEmpty()) {
      return new RootCollectionResource(repository, storagePath);
    }

    String[] parts = normalized.split("/");
    if (parts.length == 1 && "templates".equals(parts[0])) {
      return new TemplatesCollectionResource(repository, storagePath);
    }
    if (parts.length == 2 && "templates".equals(parts[0])) {
      return TemplateFileResource.fromId(repository, storagePath, parts[1]);
    }

    return null;
  }
}
