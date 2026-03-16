CREATE TABLE document_template (
  id UUID PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  description VARCHAR(1000)
);

CREATE UNIQUE INDEX ux_document_template_name ON document_template (name);
