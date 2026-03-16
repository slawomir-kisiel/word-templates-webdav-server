package pl.sk.webdav.webdav;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.GetableResource;
import com.bradmcevoy.http.LockInfo;
import com.bradmcevoy.http.LockResult;
import com.bradmcevoy.http.LockTimeout;
import com.bradmcevoy.http.LockToken;
import com.bradmcevoy.http.LockableResource;
import com.bradmcevoy.http.PropFindableResource;
import com.bradmcevoy.http.Range;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.ReplaceableResource;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.LockedException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import com.bradmcevoy.http.exceptions.NotFoundException;
import com.bradmcevoy.http.exceptions.PreConditionFailedException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import pl.sk.webdav.template.DocumentTemplate;
import pl.sk.webdav.template.DocumentTemplateRepository;

public class TemplateFileResource
    implements GetableResource, ReplaceableResource, PropFindableResource, LockableResource {
  private static final String DEFAULT_CONTENT_TYPE =
      "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
  private static final Map<UUID, LockToken> LOCKS = new ConcurrentHashMap<>();

  private final UUID id;
  private final Path storagePath;

  public TemplateFileResource(UUID id, Path storagePath) {
    this.id = id;
    this.storagePath = storagePath;
  }

  public static Resource fromId(
      DocumentTemplateRepository repository, Path storagePath, String idValue) {
    String normalized = idValue;
    if (normalized != null && normalized.toLowerCase().endsWith(".docx")) {
      normalized = normalized.substring(0, normalized.length() - 5);
    }
    UUID id;
    try {
      id = UUID.fromString(normalized);
    } catch (IllegalArgumentException ex) {
      return null;
    }
    Optional<DocumentTemplate> template = repository.findById(id);
    return template.isPresent() ? new TemplateFileResource(id, storagePath) : null;
  }

  @Override
  public void sendContent(
      OutputStream out,
      Range range,
      Map<String, String> params,
      String contentType)
      throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
    Path filePath = storagePath.resolve(id + ".docx");
    if (!Files.exists(filePath)) {
      throw new NotFoundException("Template file missing");
    }
    try (InputStream in = Files.newInputStream(filePath)) {
      byte[] buffer = new byte[8192];
      int read;
      while ((read = in.read(buffer)) != -1) {
        out.write(buffer, 0, read);
      }
    }
  }

  @Override
  public Long getMaxAgeSeconds(Auth auth) {
    return 0L;
  }

  @Override
  public String getContentType(String accepts) {
    Path filePath = storagePath.resolve(id + ".docx");
    try {
      String detected = Files.probeContentType(filePath);
      return detected == null ? DEFAULT_CONTENT_TYPE : detected;
    } catch (IOException ex) {
      return DEFAULT_CONTENT_TYPE;
    }
  }

  @Override
  public Long getContentLength() {
    Path filePath = storagePath.resolve(id + ".docx");
    try {
      return Files.size(filePath);
    } catch (IOException ex) {
      return null;
    }
  }

  @Override
  public void replaceContent(InputStream in, Long length)
      throws BadRequestException, ConflictException, NotAuthorizedException {
    try {
      Files.createDirectories(storagePath);
      Files.copy(in, storagePath.resolve(id + ".docx"), StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException ex) {
      throw new BadRequestException("Unable to store template content");
    }
  }

  @Override
  public String getUniqueId() {
    return id.toString();
  }

  @Override
  public String getName() {
    return id + ".docx";
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
    Path filePath = storagePath.resolve(id + ".docx");
    try {
      return new Date(Files.getLastModifiedTime(filePath).toMillis());
    } catch (IOException ex) {
      return new Date();
    }
  }

  @Override
  public String checkRedirect(Request request) {
    return null;
  }

  @Override
  public Date getCreateDate() {
    return getModifiedDate();
  }

  @Override
  public LockResult lock(LockTimeout timeout, LockInfo info)
      throws NotAuthorizedException, PreConditionFailedException, LockedException {
    LockToken existing = getCurrentLock();
    if (existing != null) {
      return LockResult.failed(LockResult.FailureReason.ALREADY_LOCKED);
    }
    LockToken token = new LockToken(UUID.randomUUID().toString(), info, timeout);
    token.setFrom(new Date());
    LOCKS.put(id, token);
    return LockResult.success(token);
  }

  @Override
  public LockResult refreshLock(String tokenId)
      throws NotAuthorizedException, PreConditionFailedException {
    LockToken existing = getCurrentLock();
    if (existing == null || !existing.tokenId.equals(tokenId)) {
      return LockResult.failed(LockResult.FailureReason.PRECONDITION_FAILED);
    }
    existing.setFrom(new Date());
    LOCKS.put(id, existing);
    return LockResult.success(existing);
  }

  @Override
  public void unlock(String tokenId)
      throws NotAuthorizedException, PreConditionFailedException {
    LockToken existing = getCurrentLock();
    if (existing == null || !existing.tokenId.equals(tokenId)) {
      throw new PreConditionFailedException(this);
    }
    LOCKS.remove(id);
  }

  @Override
  public LockToken getCurrentLock() {
    LockToken token = LOCKS.get(id);
    if (token != null && token.isExpired()) {
      LOCKS.remove(id);
      return null;
    }
    return token;
  }
}
