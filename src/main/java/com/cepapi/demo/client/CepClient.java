package com.cepapi.demo.client;

import com.cepapi.demo.domain.Cep;

import java.util.Optional;

public interface CepClient {
  Optional<Cep> findByCep(String cep);
}
