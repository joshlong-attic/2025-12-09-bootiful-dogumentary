package com.example.gateway;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions;
import org.springframework.cloud.gateway.server.mvc.filter.TokenRelayFilterFunctions;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.web.servlet.function.RouterFunctions.route;

@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

    @Order(Ordered.HIGHEST_PRECEDENCE)
    @Bean
    RouterFunction<@NonNull ServerResponse> api() {
        return route()
                .before(BeforeFilterFunctions.uri("http://localhost:8080"))
                .before(BeforeFilterFunctions.rewritePath("/api", "/"))
                .filter(TokenRelayFilterFunctions.tokenRelay())
                .GET("/api/**", http())
                .build();
    }

    @Order(Ordered.LOWEST_PRECEDENCE)
    @Bean
    RouterFunction<@NonNull ServerResponse> ui() {
        return route()
                .before(BeforeFilterFunctions.uri("http://localhost:8020"))
                .GET("/**", http())
                .build();
    }

}
/*

@Controller
@ResponseBody
class MeController {

    @GetMapping("/me")
    Map<String, String> me(Principal principal,
                           @RegisteredOAuth2AuthorizedClient OAuth2AuthorizedClient client) {
        var at = client.getAccessToken();
        IO.println(at.getTokenValue());
        return Map.of("name", principal.getName());
    }
}*/
