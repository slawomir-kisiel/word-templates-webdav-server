package pl.sk.webdav.webdav;

import com.bradmcevoy.http.MiltonServlet;
import java.util.Map;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebDavServletConfig {
  @Bean
  public ServletRegistrationBean<MiltonServlet> miltonServlet() {
    ServletRegistrationBean<MiltonServlet> registration =
        new ServletRegistrationBean<>(new MiltonServlet(), "/webdav/*");
    Map<String, String> initParams = new java.util.HashMap<>();
    initParams.put("resource.factory.class", WebDavResourceFactory.class.getName());
    registration.setInitParameters(initParams);
    registration.setLoadOnStartup(1);
    return registration;
  }
}
