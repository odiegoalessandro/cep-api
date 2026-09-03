package com.cepapi.demo.repository.mapper;

import com.azure.data.tables.models.TableEntity;
import com.cepapi.demo.domain.Cep;
import com.cepapi.demo.validation.CepNormalizer;

public final class CepTableMapper {

  private CepTableMapper() {
  }

  public static TableEntity toTableEntity(Cep cep) {
    var normalizedCep = CepNormalizer.normalize(cep.cep());

    return new TableEntity(
      partitionKey(normalizedCep),
      normalizedCep
    )
      .addProperty("logradouro", cep.logradouro())
      .addProperty("complemento", cep.complemento())
      .addProperty("bairro", cep.bairro())
      .addProperty("cidade", cep.cidade())
      .addProperty("uf", cep.uf())
      .addProperty("ibge", cep.ibge())
      .addProperty("ddd", cep.ddd());
  }

  public static Cep toDomain(TableEntity entity) {
    return new Cep(
      entity.getRowKey(),
      property(entity, "logradouro"),
      property(entity, "complemento"),
      property(entity, "bairro"),
      property(entity, "cidade"),
      property(entity, "uf"),
      property(entity, "ibge"),
      property(entity, "ddd")
    );
  }

  public static String partitionKey(String cep) {
    return cep.substring(0, 2);
  }

  private static String property(TableEntity entity, String name) {
    var value = entity.getProperty(name);
    return value != null ? value.toString() : null;
  }
}
