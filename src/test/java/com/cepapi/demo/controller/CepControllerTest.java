package com.cepapi.demo.controller;

import com.cepapi.demo.domain.Cep;
import com.cepapi.demo.exception.CepNotFoundException;
import com.cepapi.demo.exception.CepProvidersUnavailableException;
import com.cepapi.demo.exception.InvalidCepException;
import com.cepapi.demo.service.CepService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CepControllerTest {

  @Mock
  private CepService cepService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders
      .standaloneSetup(new CepController(cepService))
      .setControllerAdvice(new ApiExceptionHandler())
      .build();
  }

  @Test
  void shouldReturnCep() throws Exception {
    when(cepService.findByCep("01001000")).thenReturn(cep());

    mockMvc.perform(get("/ceps/01001000"))
      .andExpect(status().isOk())
      .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
      .andExpect(jsonPath("$.cep").value("01001000"))
      .andExpect(jsonPath("$.cidade").value("São Paulo"));
  }

  @Test
  void shouldReturnBadRequestForInvalidCep() throws Exception {
    when(cepService.findByCep("invalid")).thenThrow(new InvalidCepException());

    mockMvc.perform(get("/ceps/invalid"))
      .andExpect(status().isBadRequest())
      .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
      .andExpect(jsonPath("$.title").value("CEP inválido"))
      .andExpect(jsonPath("$.detail").value(
        "CEP deve conter oito dígitos, com hífen opcional no formato 00000-000"
      ));
  }

  @Test
  void shouldReturnNotFound() throws Exception {
    when(cepService.findByCep("99999999"))
      .thenThrow(new CepNotFoundException("99999999"));

    mockMvc.perform(get("/ceps/99999999"))
      .andExpect(status().isNotFound())
      .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
      .andExpect(jsonPath("$.title").value("CEP não encontrado"))
      .andExpect(jsonPath("$.detail").value("CEP não encontrado: 99999999"));
  }

  @Test
  void shouldReturnServiceUnavailable() throws Exception {
    when(cepService.findByCep("01001000"))
      .thenThrow(new CepProvidersUnavailableException());

    mockMvc.perform(get("/ceps/01001000"))
      .andExpect(status().isServiceUnavailable())
      .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
      .andExpect(jsonPath("$.title").value("Serviço temporariamente indisponível"))
      .andExpect(jsonPath("$.detail").value(
        "Os provedores de CEP estão temporariamente indisponíveis"
      ));
  }

  private Cep cep() {
    return new Cep(
      "01001000",
      "Praça da Sé",
      "lado ímpar",
      "Sé",
      "São Paulo",
      "SP",
      "3550308",
      "11"
    );
  }
}
