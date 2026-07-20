package com.syty.config;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
/**
 * Knife4j / SpringDoc OpenAPI 配置
 * 访问地址: http://localhost:8080/doc.html （Knife4j 增强 UI）
 *           http://localhost:8080/swagger-ui/index.html （原生Swagger UI）
 */
@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("三益穿线系统 API")
                        .version("1.0.0")
                        .description("穿线管理系统后端接口文档")
                        .contact(new Contact()
                                .name("三益团队")
                                .email("admin@syty.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                // 全局安全方案：Bearer Token
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("BearerAuth", new SecurityScheme()
                                .name("Authorization")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("Bearer")
                                .bearerFormat("JWT")
                                .description("输入 Sa-Token 登录后返回的 token")));
    }
}
