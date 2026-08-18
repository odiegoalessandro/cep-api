package com.cepapi.demo.repository;

import com.azure.data.tables.TableClient;
import com.azure.data.tables.models.TableEntity;
import com.azure.core.exception.HttpResponseException;
import com.cepapi.demo.domain.Cep;
import com.cepapi.demo.repository.mapper.CepTableMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class CepRepository {

  private final TableClient tableClient;

  public CepRepository(TableClient tableClient) {
    this.tableClient = tableClient;
  }

  public void save(Cep cep) {
    tableClient.upsertEntity(
      CepTableMapper.toTableEntity(cep)
    );
  }

  public Optional<Cep> findByCep(String cep) {
    var normalizedCep = CepTableMapper.normalize(cep);
    var partitionKey = CepTableMapper.partitionKey(normalizedCep);

    try {
      TableEntity entity = tableClient.getEntity(
        partitionKey,
        normalizedCep
      );

      return Optional.of(
        CepTableMapper.toDomain(entity)
      );
    } catch (HttpResponseException exception) {
      if (exception.getResponse().getStatusCode() == 404) {
        return Optional.empty();
      }

      throw exception;
    }
  }

  public void delete(String cep) {
    var normalizedCep = CepTableMapper.normalize(cep);

    tableClient.deleteEntity(
      CepTableMapper.partitionKey(normalizedCep),
      normalizedCep
    );
  }
}