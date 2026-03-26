package org.example.springsecurityf25b.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class ProjectSecurityConfig {

    @Bean
    SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrfConfig -> csrfConfig.disable())
                .authorizeHttpRequests((requests) -> { requests
                        .requestMatchers("/myAccount","/myBalance").authenticated()
                        .requestMatchers("/contact", "/register").permitAll();
                });
        http.formLogin(Customizer.withDefaults());
        //http.formLogin(flc -> flc.disable());
        http.httpBasic(Customizer.withDefaults());
        //Spring7 nu no longer supports formLogin and httpBasic
        //http.formLogin(form -> form.withDef;
        //http.httpBasic(basic -> basic.withDefaults());

        var obj = http.build();
        return obj;
    }

    //ingen kryptering
    public PasswordEncoder passwordEncoderNone() {
        return NoOpPasswordEncoder.getInstance();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        var obj = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        return obj;
    }



}
