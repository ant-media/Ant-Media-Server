package io.antmedia.swagger;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

@Configuration
@EnableSwagger2
public class SwaggerConfig {

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.basePackage("io.antmedia.rest"))
                .paths(PathSelectors.any())
                .build()
                .apiInfo(apiInfo());
    }

    private ApiInfo apiInfo() {
        return new ApiInfo(
                "Ant Media",
                "Rest Services Documentation",
                "1.0",
                "Terms of service",
                new Contact("Omer Enlicay", "github.com/enlicayomer", "omerenlicay@gmail.com"),
                "License of API", "API license URL", Collections.emptyList());
    }
}
