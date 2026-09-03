package com.cepapi.demo.service;

import com.cepapi.demo.client.CepClient;
import com.cepapi.demo.domain.Cep;
import com.cepapi.demo.exception.CepCacheException;
import com.cepapi.demo.exception.CepNotFoundException;
import com.cepapi.demo.exception.CepProvidersUnavailableException;
import com.cepapi.demo.repository.CepRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CepServiceTest {

  private static final String CEP = "01001000";

  @Mock
  private CepRepository repository;

  @Mock
  private CepClient firstClient;

  @Mock
  private CepClient secondClient;

  @Mock
  private CepClient thirdClient;

  private CepService service;

  @BeforeEach
  void setUp() {
    service = new CepService(
      repository,
      List.of(firstClient, secondClient, thirdClient)
    );
  }

  @Test
  void shouldReturnCachedCepWithoutCallingProviders() {
    var cachedCep = cep(CEP);
    when(repository.findByCep(CEP)).thenReturn(Optional.of(cachedCep));

    assertThat(service.findByCep("01001-000")).isEqualTo(cachedCep);
    verifyNoInteractions(firstClient, secondClient, thirdClient);
  }

  @Test
  void shouldContinueAfterProviderFailure() {
    when(repository.findByCep(CEP)).thenReturn(Optional.empty());
    when(firstClient.findByCep(CEP))
      .thenThrow(new ResourceAccessException("timeout"));
    when(secondClient.findByCep(CEP))
      .thenReturn(Optional.of(cep("01001-000")));

    var result = service.findByCep(CEP);

    assertThat(result.cep()).isEqualTo(CEP);
    verify(repository).save(result);
    verifyNoInteractions(thirdClient);
  }

  @Test
  void shouldContinueAfterProviderReturnsEmpty() {
    when(repository.findByCep(CEP)).thenReturn(Optional.empty());
    when(firstClient.findByCep(CEP)).thenReturn(Optional.empty());
    when(secondClient.findByCep(CEP)).thenReturn(Optional.of(cep(CEP)));

    assertThat(service.findByCep(CEP)).isEqualTo(cep(CEP));
    verifyNoInteractions(thirdClient);
  }

  @Test
  void shouldContinueWhenCacheReadFails() {
    when(repository.findByCep(CEP))
      .thenThrow(new CepCacheException("Falha ao consultar o cache de CEP", new RuntimeException()));
    when(firstClient.findByCep(CEP)).thenReturn(Optional.of(cep(CEP)));

    assertThat(service.findByCep(CEP)).isEqualTo(cep(CEP));
  }

  @Test
  void shouldReturnProviderResultWhenCacheWriteFails() {
    var providerCep = cep("01001-000");
    when(repository.findByCep(CEP)).thenReturn(Optional.empty());
    when(firstClient.findByCep(CEP)).thenReturn(Optional.of(providerCep));
    doThrow(new CepCacheException("Falha ao salvar o CEP no cache", new RuntimeException()))
      .when(repository)
      .save(any(Cep.class));

    assertThat(service.findByCep(CEP)).isEqualTo(providerCep.withCep(CEP));
  }

  @Test
  void shouldReportNotFoundWhenProvidersRespondWithoutData() {
    when(repository.findByCep(CEP)).thenReturn(Optional.empty());
    when(firstClient.findByCep(CEP)).thenReturn(Optional.empty());
    when(secondClient.findByCep(CEP)).thenReturn(Optional.empty());
    when(thirdClient.findByCep(CEP)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.findByCep(CEP))
      .isInstanceOf(CepNotFoundException.class)
      .hasMessage("CEP não encontrado: 01001000");
  }

  @Test
  void shouldReportUnavailableWhenEveryProviderFails() {
    when(repository.findByCep(CEP)).thenReturn(Optional.empty());
    when(firstClient.findByCep(CEP)).thenThrow(new ResourceAccessException("first"));
    when(secondClient.findByCep(CEP)).thenThrow(new ResourceAccessException("second"));
    when(thirdClient.findByCep(CEP)).thenThrow(new ResourceAccessException("third"));

    assertThatThrownBy(() -> service.findByCep(CEP))
      .isInstanceOf(CepProvidersUnavailableException.class)
      .hasMessage("Os provedores de CEP estão temporariamente indisponíveis");
  }

  @Test
  void shouldReportNotFoundWhenAtLeastOneProviderResponds() {
    when(repository.findByCep(CEP)).thenReturn(Optional.empty());
    when(firstClient.findByCep(CEP)).thenReturn(Optional.empty());
    when(secondClient.findByCep(CEP)).thenThrow(new ResourceAccessException("second"));
    when(thirdClient.findByCep(CEP)).thenThrow(new ResourceAccessException("third"));

    assertThatThrownBy(() -> service.findByCep(CEP))
      .isInstanceOf(CepNotFoundException.class);
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
