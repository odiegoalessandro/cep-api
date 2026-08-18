package com.cepapi.demo.dto;

public record OpenCepResponse(
  String cep,
  String logradouro,
  String complemento,
  String bairro,
  String localidade,
  String uf,
  String ibge
) {
}