package com.cepapi.demo.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.CharacterEncodingFilter;

import java.nio.charset.StandardCharsets;

@Configuration
public class EncodingConfig {

  @Bean
  public FilterRegistrationBean<CharacterEncodingFilter> utf8EncodingFilter() {
    var filter = new CharacterEncodingFilter(
      StandardCharsets.UTF_8.name(),
      true
    );

    var registration = new FilterRegistrationBean<>(filter);

    registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
    registration.addUrlPatterns("/*");

    return registration;
  }
}