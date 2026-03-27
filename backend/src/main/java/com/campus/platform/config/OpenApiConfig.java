package com.campus.platform.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("院校管理平台 API")
                .version("1.0.0")
                .description("中国政府奖学金项目留学生招生录取管理平台 RESTful API 文档")
                .contact(new Contact()
                    .name("开发团队")
                    .email("dev@campus-platform.com"))
            );
    }
}
