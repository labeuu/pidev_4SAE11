package tn.esprit.freelanciajob.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Serves uploaded files as static resources.
 * GET /uploads/applications/{appId}/{filename} streams the file directly.
 * This means the frontend can link to attachments without a dedicated download endpoint.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.base-dir:uploads}")
    private String uploadBaseDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absolutePath = Paths.get(uploadBaseDir).toAbsolutePath().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + absolutePath + "/");
    }
}
