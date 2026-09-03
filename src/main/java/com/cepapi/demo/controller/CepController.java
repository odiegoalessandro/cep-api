package com.cepapi.demo.controller;

import com.cepapi.demo.domain.Cep;
import com.cepapi.demo.service.CepService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ceps")
public class CepController {
  private final CepService cepService;

  public CepController(CepService cepService) {
    this.cepService = cepService;
  }

  @GetMapping("/{cep}")
  public Cep findByCep(@PathVariable String cep) {
    return cepService.findByCep(cep);
  }
}
