package com.cepapi.demo.exception;

public class InvalidCepException extends RuntimeException {

  public InvalidCepException() {
    super("CEP deve conter oito dígitos, com hífen opcional no formato 00000-000");
  }
}
