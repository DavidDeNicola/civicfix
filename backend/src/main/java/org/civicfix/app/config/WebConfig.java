package org.civicfix.app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Espone i file caricati (foto delle segnalazioni) come risorse statiche,
 * leggibili via URL diretto senza passare da un controller: sono file
 * pubblici già validati da FileStorageService.
 */

@Configuration
public class WebConfig implements  WebMvcConfigurer {

    @Value("${app.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/reports/**")
                .addResourceLocations("file:" + uploadDir + "/");
    }
}
