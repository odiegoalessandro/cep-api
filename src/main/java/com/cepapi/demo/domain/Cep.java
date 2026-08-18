package com.cepapi.demo.domain;

import lombok.Builder;

@Builder
public record Cep(
  String cep,
  String logradouro,
  String complemento,
  String bairro,
  String cidade,
  String uf,
  String ibge,
  String ddd
) {
}