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
import pl.sk.webdav.template.DocumentTemplateRepository;

public class RootCollectionResource implements CollectionResource, PropFindableResource {
  private final DocumentTemplateRepository repository;
  private final Path storagePath;
  private final Date createdAt;

  public RootCollectionResource(DocumentTemplateRepository repository, Path storagePath) {
    this.repository = repository;
    this.storagePath = storagePath;
    this.createdAt = new Date();
  }

  @Override
  public Resource child(String name) throws NotAuthorizedException, BadRequestException {
    if ("templates".equals(name)) {
      return new TemplatesCollectionResource(repository, storagePath);
    }
    return null;
  }

  @Override
  public List<? extends Resource> getChildren() throws NotAuthorizedException, BadRequestException {
    return List.of(new TemplatesCollectionResource(repository, storagePath));
  }

  @Override
  public String getUniqueId() {
    return "webdav-root";
  }

  @Override
  public String getName() {
    return "webdav";
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
