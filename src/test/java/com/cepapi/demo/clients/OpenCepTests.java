package com.cepapi.demo.clients;

import com.cepapi.demo.client.OpenCepClient;
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

public class OpenCepTests {

  private MockRestServiceServer mockServer;
  private OpenCepClient openCepClient;

  @BeforeEach
  public void setup() {
    var builder = RestClient.builder()
      .baseUrl("https://opencep.com");

    mockServer = MockRestServiceServer.bindTo(builder).build();
    openCepClient = new OpenCepClient(builder.build());
  }

  @Test
  void shouldFindCep() {
    mockServer.expect(
        once(),
        requestTo("https://opencep.com/v1/01001000.json")
      )
      .andExpect(method(HttpMethod.GET))
      .andRespond(withSuccess("""
        {
          "cep": "01001-000",
          "logradouro": "Praça da Sé",
          "complemento": "",
          "bairro": "Sé",
          "localidade": "São Paulo",
          "uf": "SP",
          "ibge": "3550308"
        }
        """, MediaType.APPLICATION_JSON));

    var result = openCepClient.findByCep("01001000");

    assertThat(result).isPresent();

    var cep = result.orElseThrow();

    assertThat(cep.cep()).isEqualTo("01001-000");
    assertThat(cep.logradouro()).isEqualTo("Praça da Sé");
    assertThat(cep.bairro()).isEqualTo("Sé");
    assertThat(cep.cidade()).isEqualTo("São Paulo");
    assertThat(cep.uf()).isEqualTo("SP");
    assertThat(cep.ibge()).isEqualTo("3550308");

    mockServer.verify();
  }

  @Test
  void shouldReturnEmptyWhenCepDoesNotExist() {
    mockServer.expect(
        once(),
        requestTo("https://opencep.com/v1/99999999.json")
      )
      .andExpect(method(HttpMethod.GET))
      .andRespond(withStatus(HttpStatus.NOT_FOUND));

    var result = openCepClient.findByCep("99999999");

    assertThat(result).isEmpty();

    mockServer.verify();
  }
}