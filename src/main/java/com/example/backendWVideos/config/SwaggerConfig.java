package com.example.backendWVideos.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Value('${app.frontend-url:http://localhost:5173}')
    private String frontendUrl;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title('WVideos Backend API')
                        .version('1.0.0')
                        .description('API documentation cho WVideos Backend với JWT Authentication. ' +
                                'Sử dụng JWT token để xác thực các endpoint được bảo vệ.')
                        .contact(new Contact()
                                .name('WVideos Development Team')
                                .email('support@wvideos.com')
                                .url('https://wvideos.com'))
                        .license(new License()
                                .name('Apache 2.0')
                                .url('https://www.apache.org/licenses/LICENSE-2.0.html')))
                .servers(List.of(
                        new Server()
                                .url('http://localhost:8080')
                                .description('Local Development Server'),
                        new Server()
                                .url(frontendUrl)
                                .description('Frontend URL')
                ))
                .addSecurityItem(new SecurityRequirement().addList('Bearer Authentication'))
                .components(new Components()
                        .addSecuritySchemes('Bearer Authentication',
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme('bearer')
                                        .bearerFormat('JWT')
                                        .name('Authorization')
                                        .description('Nhập JWT token vào đây. Token có thể lấy từ endpoint POST /auth/token sau khi đăng nhập thành công.')));
    }
}
