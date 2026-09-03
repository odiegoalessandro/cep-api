package com.cepapi.demo.domain;

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

  public Cep withCep(String canonicalCep) {
    return new Cep(
      canonicalCep,
      logradouro,
      complemento,
      bairro,
      cidade,
      uf,
      ibge,
      ddd
    );
  }
}
