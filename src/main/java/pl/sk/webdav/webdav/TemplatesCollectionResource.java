package pl.sk.webdav.webdav;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.CollectionResource;
import com.bradmcevoy.http.PropFindableResource;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import pl.sk.webdav.template.DocumentTemplateRepository;

public class TemplatesCollectionResource implements CollectionResource, PropFindableResource {
  private final DocumentTemplateRepository repository;
  private final Path storagePath;
  private final Date createdAt;

  public TemplatesCollectionResource(DocumentTemplateRepository repository, Path storagePath) {
    this.repository = repository;
    this.storagePath = storagePath;
    this.createdAt = new Date();
  }

  @Override
  public Resource child(String name) throws NotAuthorizedException, BadRequestException {
    return TemplateFileResource.fromId(repository, storagePath, name);
  }

  @Override
  public List<? extends Resource> getChildren() throws NotAuthorizedException, BadRequestException {
    return repository.findAll().stream()
        .map(template -> new TemplateFileResource(template.getId(), storagePath))
        .collect(Collectors.toList());
  }

  @Override
  public String getUniqueId() {
    return "templates-collection";
  }

  @Override
  public String getName() {
    return "templates";
  }

  @Override
  public Object authenticate(String user, String password) {
    return "anonymous";
  }

  @Override
  public boolean authorise(Request request, Request.Method method, Auth auth) {
    return true;
  }

  @Override
  public String getRealm() {
    return "webdav";
  }

  @Override
  public Date getModifiedDate() {
    return createdAt;
  }

  @Override
  public String checkRedirect(Request request) {
    return null;
  }

  @Override
  public Date getCreateDate() {
    return createdAt;
  }
}
