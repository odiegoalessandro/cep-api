package com.cepapi.demo.controller;

import com.cepapi.demo.exception.CepNotFoundException;
import com.cepapi.demo.exception.CepProvidersUnavailableException;
import com.cepapi.demo.exception.InvalidCepException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(InvalidCepException.class)
  ProblemDetail handleInvalidCep(InvalidCepException exception) {
    return problem(HttpStatus.BAD_REQUEST, "CEP inválido", exception.getMessage());
  }

  @ExceptionHandler(CepNotFoundException.class)
  ProblemDetail handleNotFound(CepNotFoundException exception) {
    return problem(HttpStatus.NOT_FOUND, "CEP não encontrado", exception.getMessage());
  }

  @ExceptionHandler(CepProvidersUnavailableException.class)
  ProblemDetail handleProvidersUnavailable(CepProvidersUnavailableException exception) {
    return problem(
      HttpStatus.SERVICE_UNAVAILABLE,
      "Serviço temporariamente indisponível",
      exception.getMessage()
    );
  }

  private ProblemDetail problem(HttpStatus status, String title, String detail) {
    var problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setTitle(title);
    return problem;
  }
}
