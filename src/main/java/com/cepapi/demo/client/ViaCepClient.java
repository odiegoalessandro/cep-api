package com.cepapi.demo.client;

import com.cepapi.demo.domain.Cep;
import com.cepapi.demo.dto.ViaCepResponse;
import com.cepapi.demo.mapper.CepMapper;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Component
@Order(1)
public class ViaCepClient implements CepClient {
  private final RestClient restClient;

  public ViaCepClient(RestClient viaCepRestClient) {
    this.restClient = viaCepRestClient;
  }

  @Override
  public Optional<Cep> findByCep(String cep) {
    var response = restClient.get()
      .uri("/ws/{cep}/json/", cep)
      .retrieve()
      .body(ViaCepResponse.class);

    if (response == null || Boolean.TRUE.equals(response.erro())) {
      return Optional.empty();
    }

    return Optional.of(CepMapper.toDomain(response));
  }
}
