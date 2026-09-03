package com.cepapi.demo.repository;

import com.azure.core.exception.AzureException;
import com.azure.core.exception.HttpResponseException;
import com.azure.data.tables.TableClient;
import com.cepapi.demo.domain.Cep;
import com.cepapi.demo.exception.CepCacheException;
import com.cepapi.demo.repository.mapper.CepTableMapper;
import com.cepapi.demo.validation.CepNormalizer;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class CepRepository {

  private final TableClient tableClient;

  public CepRepository(TableClient tableClient) {
    this.tableClient = tableClient;
  }

  public Optional<Cep> findByCep(String cep) {
    var canonicalCep = CepNormalizer.normalize(cep);

    try {
      var entity = tableClient.getEntity(
        CepTableMapper.partitionKey(canonicalCep),
        canonicalCep
      );

      return Optional.of(CepTableMapper.toDomain(entity));
    } catch (HttpResponseException exception) {
      if (exception.getResponse() != null
        && exception.getResponse().getStatusCode() == 404) {
        return Optional.empty();
      }

      throw readFailure(exception);
    } catch (AzureException exception) {
      throw readFailure(exception);
    }
  }

  public void save(Cep cep) {
    var entity = CepTableMapper.toTableEntity(cep);

    try {
      tableClient.upsertEntity(entity);
    } catch (AzureException exception) {
      throw new CepCacheException("Falha ao salvar o CEP no cache", exception);
    }
  }

  private CepCacheException readFailure(AzureException cause) {
    return new CepCacheException("Falha ao consultar o cache de CEP", cause);
  }
}
