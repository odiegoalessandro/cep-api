package com.cepapi.demo.clients;

import com.cepapi.demo.client.BrasilApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class BrasilApiTests {

  private MockRestServiceServer mockServer;
  private BrasilApiClient brasilApiClient;

  @BeforeEach
  public void setup() {
    var builder = RestClient.builder()
      .baseUrl("https://brasilapi.com.br");

    mockServer = MockRestServiceServer.bindTo(builder).build();
    brasilApiClient = new BrasilApiClient(builder.build());
  }

  @Test
  void shouldFindCep() {
    mockServer.expect(
        once(),
        requestTo("https://brasilapi.com.br/api/cep/v1/01001000")
      )
      .andExpect(method(HttpMethod.GET))
      .andRespond(withSuccess("""
        {
          "cep": "01001000",
          "state": "SP",
          "city": "São Paulo",
          "neighborhood": "Sé",
          "street": "Praça da Sé",
          "service": "open-cep"
        }
        """, MediaType.APPLICATION_JSON));

    var result = brasilApiClient.findByCep("01001000");

    assertThat(result).isPresent();

    var cep = result.orElseThrow();

    assertThat(cep.cep()).isEqualTo("01001000");
    assertThat(cep.logradouro()).isEqualTo("Praça da Sé");
    assertThat(cep.bairro()).isEqualTo("Sé");
    assertThat(cep.cidade()).isEqualTo("São Paulo");
    assertThat(cep.uf()).isEqualTo("SP");

    mockServer.verify();
  }

  @Test
  void shouldReturnEmptyWhenCepDoesNotExist() {
    mockServer.expect(
        once(),
        requestTo("https://brasilapi.com.br/api/cep/v1/99999999")
      )
      .andExpect(method(HttpMethod.GET))
      .andRespond(withStatus(HttpStatus.NOT_FOUND));

    var result = brasilApiClient.findByCep("99999999");

    assertThat(result).isEmpty();

    mockServer.verify();
  }
}