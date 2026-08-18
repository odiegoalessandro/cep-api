package com.cepapi.demo.client;

import com.cepapi.demo.domain.Cep;
import com.cepapi.demo.dto.OpenCepResponse;
import com.cepapi.demo.mapper.CepMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Component
@Order(2)
public class OpenCepClient implements CepClient {

  private final RestClient restClient;

  public OpenCepClient(
    @Qualifier("openCepRestClient") RestClient restClient
  ) {
    this.restClient = restClient;
  }

  @Override
  public Optional<Cep> findByCep(String cep) {
    try {
      var response = restClient.get()
        .uri("/v1/{cep}.json", cep)
        .retrieve()
        .body(OpenCepResponse.class);

      return Optional.ofNullable(response)
        .map(CepMapper::toDomain);
    } catch (HttpClientErrorException.NotFound exception) {
      return Optional.empty();
    }
  }
}