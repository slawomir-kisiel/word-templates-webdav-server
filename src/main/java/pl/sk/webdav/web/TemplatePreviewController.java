package pl.sk.webdav.web;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.docx4j.Docx4J;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.Part;
import org.docx4j.openpackaging.parts.WordprocessingML.MetafileEmfPart;
import org.docx4j.openpackaging.parts.WordprocessingML.MetafileWmfPart;
import org.freehep.graphicsio.emf.EMFInputStream;
import org.freehep.graphicsio.emf.EMFRenderer;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;
import pl.sk.webdav.template.DocumentTemplate;
import pl.sk.webdav.template.DocumentTemplateRepository;
import pl.sk.webdav.template.DocumentTemplateService;

@RestController
public class TemplatePreviewController {
  private final DocumentTemplateRepository repository;
  private final DocumentTemplateService service;

  public TemplatePreviewController(
      DocumentTemplateRepository repository, DocumentTemplateService service) {
    this.repository = repository;
    this.service = service;
  }

  @GetMapping("/templates/{id}/preview")
  public ResponseEntity<byte[]> preview(@PathVariable("id") UUID id) {
    Optional<DocumentTemplate> template = repository.findById(id);
    if (template.isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    Path source = service.resolvePath(id);
    if (!Files.exists(source)) {
      return ResponseEntity.notFound().build();
    }
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      WordprocessingMLPackage pkg = WordprocessingMLPackage.load(source.toFile());
      convertMetafilesToPng(pkg);
      Docx4J.toPDF(pkg, out);
      byte[] payload = out.toByteArray();
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_PDF);
      headers.setCacheControl(CacheControl.noStore());
      headers.setContentLength(payload.length);
      return new ResponseEntity<>(payload, headers, HttpStatus.OK);
    } catch (Exception ex) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  @GetMapping("/templates/{id}/preview-meta")
  public ResponseEntity<Map<String, Object>> previewMeta(@PathVariable("id") UUID id) {
    Optional<DocumentTemplate> template = repository.findById(id);
    if (template.isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    Path source = service.resolvePath(id);
    if (!Files.exists(source)) {
      return ResponseEntity.notFound().build();
    }
    try {
      long lastModified = service.getLastModified(id);
      Map<String, Object> payload = new HashMap<>();
      payload.put("lastModified", lastModified);
      return ResponseEntity.ok(payload);
    } catch (IOException ex) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  private void convertMetafilesToPng(WordprocessingMLPackage pkg) {
    List<Part> toRemove = new ArrayList<>();
    for (Part part : pkg.getParts().getParts().values()) {
      try {
        if (part instanceof MetafileEmfPart) {
          byte[] png = convertEmfToPng((MetafileEmfPart) part);
          replaceBinaryAsPng(pkg, part, png);
        } else if (part instanceof MetafileWmfPart) {
          byte[] png = convertWmfToPng((MetafileWmfPart) part);
          replaceBinaryAsPng(pkg, part, png);
        }
      } catch (Exception ex) {
        if (part instanceof MetafileEmfPart || part instanceof MetafileWmfPart) {
          toRemove.add(part);
        }
      }
    }
    for (Part part : toRemove) {
      part.remove();
    }
  }

  private void replaceBinaryAsPng(WordprocessingMLPackage pkg, Part part, byte[] png) {
    if (png == null || png.length == 0) {
      return;
    }
    if (part instanceof org.docx4j.openpackaging.parts.WordprocessingML.BinaryPart) {
      org.docx4j.openpackaging.parts.WordprocessingML.BinaryPart binaryPart =
          (org.docx4j.openpackaging.parts.WordprocessingML.BinaryPart) part;
      binaryPart.setBinaryData(png);
      pkg.getContentTypeManager()
          .addOverrideContentType(part.getPartName(), "image/png");
    }
  }

  private byte[] convertEmfToPng(MetafileEmfPart part) throws IOException {
    byte[] data = part.getBytes();
    EMFRenderer renderer;
    try (ByteArrayInputStream input = new ByteArrayInputStream(data)) {
      EMFInputStream emfInputStream = new EMFInputStream(input);
      renderer = new EMFRenderer(emfInputStream);
    }
    Dimension size = renderer.getSize();
    int width = Math.max(1, size.width);
    int height = Math.max(1, size.height);
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    Graphics2D graphics = image.createGraphics();
    graphics.setPaint(Color.WHITE);
    graphics.fillRect(0, 0, width, height);
    renderer.paint(graphics);
    graphics.dispose();
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      ImageIO.write(image, "png", out);
      return out.toByteArray();
    }
  }

  private byte[] convertWmfToPng(MetafileWmfPart part) throws Exception {
    MetafileWmfPart.SvgDocument svg = part.toSVG();
    PNGTranscoder transcoder = new PNGTranscoder();
    TranscoderInput input = new TranscoderInput(svg.getDomDocument());
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      TranscoderOutput output = new TranscoderOutput(out);
      transcoder.transcode(input, output);
      return out.toByteArray();
    }
  }
}
