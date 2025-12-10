package com.example.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.sql.DataSource;
import java.security.Principal;
import java.util.Map;

@SpringBootApplication
public class AuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }

    @Bean
    JdbcUserDetailsManager jdbcUserDetailsManager(DataSource dataSource) {
        var u = new JdbcUserDetailsManager(dataSource);
        u.setEnableUpdatePassword(true);
        return u;
    }

    @Bean
    Customizer<HttpSecurity> securityCustomizer() {
        return http -> http
                .oauth2AuthorizationServer(a -> a.oidc(Customizer.withDefaults()))
                .webAuthn(a -> a
                        .rpId("localhost")
                        .rpName("bootiful")
                        .allowedOrigins("http://localhost:9090")
                )
                .oneTimeTokenLogin(ott -> ott
                        .tokenGenerationSuccessHandler(
                                (request, response, oneTimeToken) -> {

                                    response.getWriter()
                                            .println("you've got console mail!");
                                    response.setContentType(MediaType.TEXT_PLAIN_VALUE);
                                    // console mail
                                    var msg = "please go to http://localhost:9090/login/ott?token=" +
                                            oneTimeToken.getTokenValue();
                                    IO.println(msg);


                                }
                        ));
    }


}
