package com.cepapi.demo.service;

import com.cepapi.demo.client.CepClient;
import com.cepapi.demo.domain.Cep;
import com.cepapi.demo.exception.CepCacheException;
import com.cepapi.demo.exception.CepNotFoundException;
import com.cepapi.demo.exception.CepProvidersUnavailableException;
import com.cepapi.demo.repository.CepRepository;
import com.cepapi.demo.validation.CepNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Optional;

@Service
public class CepService {

  private static final Logger log = LoggerFactory.getLogger(CepService.class);

  private final CepRepository cepRepository;
  private final List<CepClient> cepClients;

  public CepService(CepRepository cepRepository, List<CepClient> cepClients) {
    this.cepRepository = cepRepository;
    this.cepClients = cepClients;
  }

  public Cep findByCep(String value) {
    var cep = CepNormalizer.normalize(value);
    var cachedCep = findInCache(cep);

    if (cachedCep.isPresent()) {
      return cachedCep.get();
    }

    var providerResponded = false;

    for (var client : cepClients) {
      try {
        var result = client.findByCep(cep);
        providerResponded = true;

        if (result.isPresent()) {
          var canonicalResult = result.get().withCep(cep);
          saveInCache(canonicalResult);
          return canonicalResult;
        }
      } catch (RestClientException exception) {
        log.warn(
          "CEP provider {} failed with {}",
          client.getClass().getSimpleName(),
          exception.getClass().getSimpleName()
        );
      }
    }

    if (providerResponded) {
      throw new CepNotFoundException(cep);
    }

    throw new CepProvidersUnavailableException();
  }

  private Optional<Cep> findInCache(String cep) {
    try {
      return cepRepository.findByCep(cep);
    } catch (CepCacheException exception) {
      log.warn("CEP cache read failed: {}", exception.getMessage());
      return Optional.empty();
    }
  }

  private void saveInCache(Cep cep) {
    try {
      cepRepository.save(cep);
    } catch (CepCacheException exception) {
      log.warn("CEP cache write failed: {}", exception.getMessage());
    }
  }
}
