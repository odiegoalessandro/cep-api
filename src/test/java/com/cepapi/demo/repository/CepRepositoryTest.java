package com.cepapi.demo.repository;

import com.azure.core.exception.AzureException;
import com.azure.core.exception.HttpResponseException;
import com.azure.core.http.HttpResponse;
import com.azure.data.tables.TableClient;
import com.azure.data.tables.models.TableEntity;
import com.cepapi.demo.domain.Cep;
import com.cepapi.demo.exception.CepCacheException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CepRepositoryTest {

  @Mock
  private TableClient tableClient;

  private CepRepository repository;

  @BeforeEach
  void setUp() {
    repository = new CepRepository(tableClient);
  }

  @Test
  void shouldReturnCachedCep() {
    var entity = new TableEntity("01", "01001000")
      .addProperty("logradouro", "Praça da Sé")
      .addProperty("cidade", "São Paulo")
      .addProperty("uf", "SP");

    when(tableClient.getEntity("01", "01001000")).thenReturn(entity);

    assertThat(repository.findByCep("01001-000"))
      .get()
      .extracting(Cep::cep)
      .isEqualTo("01001000");
  }

  @Test
  void shouldReturnEmptyWhenCacheDoesNotContainCep() {
    when(tableClient.getEntity("01", "01001000"))
      .thenThrow(httpException(404));

    assertThat(repository.findByCep("01001000")).isEmpty();
  }

  @Test
  void shouldTranslateCacheReadFailure() {
    when(tableClient.getEntity("01", "01001000"))
      .thenThrow(httpException(500));

    assertThatThrownBy(() -> repository.findByCep("01001000"))
      .isInstanceOf(CepCacheException.class)
      .hasMessage("Falha ao consultar o cache de CEP");
  }

  @Test
  void shouldSaveUsingCanonicalKeys() {
    var cep = cep("01001-000");
    var entityCaptor = ArgumentCaptor.forClass(TableEntity.class);

    repository.save(cep);

    verify(tableClient).upsertEntity(entityCaptor.capture());
    assertThat(entityCaptor.getValue().getPartitionKey()).isEqualTo("01");
    assertThat(entityCaptor.getValue().getRowKey()).isEqualTo("01001000");
  }

  @Test
  void shouldTranslateCacheWriteFailure() {
    doThrow(mock(AzureException.class))
      .when(tableClient)
      .upsertEntity(any(TableEntity.class));

    assertThatThrownBy(() -> repository.save(cep("01001000")))
      .isInstanceOf(CepCacheException.class)
      .hasMessage("Falha ao salvar o CEP no cache");
  }

  private HttpResponseException httpException(int status) {
    var response = mock(HttpResponse.class);
    var exception = mock(HttpResponseException.class);
    when(response.getStatusCode()).thenReturn(status);
    when(exception.getResponse()).thenReturn(response);
    return exception;
  }

  private Cep cep(String value) {
    return new Cep(
      value,
      "Praça da Sé",
      null,
      "Sé",
      "São Paulo",
      "SP",
      "3550308",
      "11"
    );
  }
}
