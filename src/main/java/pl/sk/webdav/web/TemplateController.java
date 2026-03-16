package pl.sk.webdav.web;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.sk.webdav.template.DocumentTemplate;
import pl.sk.webdav.template.DocumentTemplateService;

@Controller
public class TemplateController {
  private final DocumentTemplateService service;

  public TemplateController(DocumentTemplateService service) {
    this.service = service;
  }

  @GetMapping("/")
  public String index(
      @RequestParam(name = "q", required = false) String query,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "dir", required = false) String dir,
      HttpServletRequest request,
      Model model) {
    List<DocumentTemplate> templates = service.list(query, sort, dir);
    model.addAttribute("templates", templates);
    model.addAttribute("q", query == null ? "" : query);
    model.addAttribute("sort", sort == null ? "name" : sort);
    model.addAttribute("dir", dir == null ? "asc" : dir);
    model.addAttribute("baseUrl", buildBaseUrl(request));
    return "index";
  }

  @PostMapping("/templates")
  public String upload(
      @RequestParam("name") String name,
      @RequestParam("description") String description,
      @RequestParam("file") MultipartFile file,
      RedirectAttributes redirectAttributes) {
    if (name == null || name.isBlank() || file == null || file.isEmpty()) {
      redirectAttributes.addFlashAttribute(
          "error", "Name and file are required.");
      return "redirect:/";
    }
    try {
      service.create(name.trim(), description == null ? null : description.trim(), file);
      redirectAttributes.addFlashAttribute("success", "Template uploaded.");
    } catch (IOException ex) {
      redirectAttributes.addFlashAttribute("error", "Upload failed.");
    }
    return "redirect:/";
  }

  @PostMapping("/templates/{id}/delete")
  public String delete(@PathVariable("id") UUID id, RedirectAttributes redirectAttributes) {
    try {
      service.delete(id);
      redirectAttributes.addFlashAttribute("success", "Template deleted.");
    } catch (IOException ex) {
      redirectAttributes.addFlashAttribute("error", "Delete failed.");
    }
    return "redirect:/";
  }

  private String buildBaseUrl(HttpServletRequest request) {
    String scheme = request.getScheme();
    String host = request.getServerName();
    int port = request.getServerPort();
    String contextPath = request.getContextPath();
    boolean defaultPort = ("http".equalsIgnoreCase(scheme) && port == 80)
        || ("https".equalsIgnoreCase(scheme) && port == 443);
    String portPart = defaultPort ? "" : ":" + port;
    return scheme + "://" + host + portPart + contextPath;
  }
}
