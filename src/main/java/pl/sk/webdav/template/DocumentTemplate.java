package pl.sk.webdav.template;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "document_template")
public class DocumentTemplate {
  @Id
  @Column(nullable = false, columnDefinition = "UUID")
  private UUID id;

  @Column(nullable = false)
  private String name;

  @Column(length = 1000)
  private String description;

  protected DocumentTemplate() {}

  public DocumentTemplate(UUID id, String name, String description) {
    this.id = id;
    this.name = name;
    this.description = description;
  }

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }
}
