package com.cepapi.demo.repository.mapper;


import com.azure.data.tables.models.TableEntity;
import com.cepapi.demo.domain.Cep;

public final class CepTableMapper {

  private CepTableMapper() {
  }

  public static TableEntity toTableEntity(Cep cep) {
    var normalizedCep = normalize(cep.cep());

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
    return Cep.builder()
      .cep(entity.getRowKey())
      .logradouro(property(entity, "logradouro"))
      .complemento(property(entity, "complemento"))
      .bairro(property(entity, "bairro"))
      .cidade(property(entity, "cidade"))
      .uf(property(entity, "uf"))
      .ibge(property(entity, "ibge"))
      .ddd(property(entity, "ddd"))
      .build();
  }

  public static String partitionKey(String cep) {
    return cep.substring(0, 2);
  }

  public static String normalize(String cep) {
    return cep.replaceAll("\\D", "");
  }

  private static String property(TableEntity entity, String name) {
    var value = entity.getProperty(name);
    return value != null ? value.toString() : null;
  }
}
