package com.ll.netmong.base.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

@Configuration
@PropertySource("classpath:/application.yml")
@Component
public class WebConfig implements WebMvcConfigurer {
    private static final Logger logger = Logger.getLogger(WebConfig.class.getName());

//    @Value("${spring.servlet.multipart.location}")
//    private String imagePath;
    @Value("${custom.image.url}")
    private String imagePath;

    @Value("${domain}")
    String  domain;

    //파일 서버 생성 가능
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        exposeDirectory(registry);
    }

    void exposeDirectory(ResourceHandlerRegistry registry)
    {
        Path uploadDir = Paths.get(imagePath);
        String uploadPath = uploadDir.toFile().getAbsolutePath();
        logger.info("업로드 경로 : {}" + uploadPath);

        if(imagePath.startsWith("../"))
        {
            imagePath = imagePath.replace("../", "");
        }

        logger.info("업로드 상대적 경로 : {}" + imagePath);
        registry.addResourceHandler("/" + imagePath + "**")
                .addResourceLocations("file:/" + uploadPath + "/");
    }
}
