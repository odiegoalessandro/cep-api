package com.cepapi.demo.exception;

public class CepProvidersUnavailableException extends RuntimeException {

  public CepProvidersUnavailableException() {
    super("Os provedores de CEP estão temporariamente indisponíveis");
  }
}
