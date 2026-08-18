package com.cepapi.demo.dto;

public record BrasilApiResponse(
  String cep,
  String state,
  String city,
  String neighborhood,
  String street,
  String service
) {
}