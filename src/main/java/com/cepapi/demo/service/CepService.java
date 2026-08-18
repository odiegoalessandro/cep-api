package com.cepapi.demo.service;


import com.cepapi.demo.client.CepClient;
import com.cepapi.demo.domain.Cep;
import com.cepapi.demo.repository.CepRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
@Service
public class CepService {

  private final CepRepository cepRepository;
  private final List<CepClient> cepClients;

  public CepService(
    CepRepository cepRepository,
    List<CepClient> cepClients
  ) {
    this.cepRepository = cepRepository;
    this.cepClients = cepClients;
  }

  public Optional<Cep> findByCep(String cep) {
    var cachedCep = cepRepository.findByCep(cep);

    if (cachedCep.isPresent()) {
      return cachedCep;
    }

    for (var client : cepClients) {
      var result = client.findByCep(cep);

      if (result.isPresent()) {
        cepRepository.save(result.get());
        return result;
      }
    }

    return Optional.empty();
  }

  public void save(Cep cep) {
    if (cepRepository.findByCep(cep.cep()).isPresent()) {
      throw new IllegalArgumentException("CEP already exists");
    }

    cepRepository.save(cep);
  }
}