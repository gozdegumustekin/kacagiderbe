package com.example.kacagider.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.root:/data/uploads}")
    private String uploadRoot;

    @Value("${app.upload.public-prefix:/uploads}")
    private String publicPrefix;

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        Path uploadPath = Paths.get(uploadRoot).toAbsolutePath().normalize();

        String pattern = publicPrefix.endsWith("/**")
                ? publicPrefix
                : (publicPrefix.endsWith("/") ? publicPrefix + "**" : publicPrefix + "/**");

        String location = uploadPath.toUri().toString();

        registry.addResourceHandler(pattern)
                .addResourceLocations(location);
    }
}