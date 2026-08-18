package com.cepapi.demo.controller;


import com.cepapi.demo.domain.Cep;
import com.cepapi.demo.service.CepService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ceps")
public class CepController {
  private final CepService cepService;

  public CepController(CepService cepService) {
    this.cepService = cepService;
  }

  @GetMapping("/{cep}")
  public ResponseEntity<Cep> findByCep(@PathVariable String cep) {
    return cepService.findByCep(cep)
      .map(ResponseEntity::ok)
      .orElseGet(() -> ResponseEntity.notFound().build());
  }
}
