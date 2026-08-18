package com.cepapi.demo.mapper;

import com.cepapi.demo.domain.Cep;
import com.cepapi.demo.dto.BrasilApiResponse;
import com.cepapi.demo.dto.OpenCepResponse;
import com.cepapi.demo.dto.ViaCepResponse;

public final class CepMapper {

  private CepMapper() {
  }

  public static Cep toDomain(ViaCepResponse response) {
    return new Cep(
      response.cep(),
      response.logradouro(),
      response.complemento(),
      response.bairro(),
      response.localidade(),
      response.uf(),
      response.ibge(),
      response.ddd()
    );
  }

  public static Cep toDomain(BrasilApiResponse response) {
    return new Cep(
      response.cep(),
      response.street(),
      "",
      response.neighborhood(),
      response.city(),
      response.state(),
      null,
      null
    );
  }

  public static Cep toDomain(OpenCepResponse response) {
    return new Cep(
      response.cep(),
      response.logradouro(),
      response.complemento(),
      response.bairro(),
      response.localidade(),
      response.uf(),
      response.ibge(),
      null
    );
  }
}