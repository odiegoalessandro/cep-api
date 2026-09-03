package com.cepapi.demo.repository.mapper;

import com.cepapi.demo.domain.Cep;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CepTableMapperTest {

  @Test
  void shouldMapCepToTableEntityAndBack() {
    var cep = new Cep(
      "01001-000",
      "Praça da Sé",
      "lado ímpar",
      "Sé",
      "São Paulo",
      "SP",
      "3550308",
      "11"
    );

    var entity = CepTableMapper.toTableEntity(cep);
    var restored = CepTableMapper.toDomain(entity);

    assertThat(entity.getPartitionKey()).isEqualTo("01");
    assertThat(entity.getRowKey()).isEqualTo("01001000");
    assertThat(restored).isEqualTo(cep.withCep("01001000"));
  }

  @Test
  void shouldPreserveMissingOptionalFields() {
    var cep = new Cep(
      "01001000",
      "Praça da Sé",
      null,
      "Sé",
      "São Paulo",
      "SP",
      null,
      null
    );

    var restored = CepTableMapper.toDomain(CepTableMapper.toTableEntity(cep));

    assertThat(restored.complemento()).isNull();
    assertThat(restored.ibge()).isNull();
    assertThat(restored.ddd()).isNull();
  }
}
