package com.cepapi.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

  @Bean
  public RestClient viaCepRestClient(RestClient.Builder builder) {
    return builder.clone()
      .baseUrl("https://viacep.com.br")
      .build();
  }

  @Bean
  public RestClient brasilApiRestClient(RestClient.Builder builder) {
    return builder.clone()
      .baseUrl("https://brasilapi.com.br")
      .build();
  }

  @Bean
  public RestClient openCepRestClient(RestClient.Builder builder) {
    return builder.clone()
      .baseUrl("https://opencep.com")
      .build();
  }
}