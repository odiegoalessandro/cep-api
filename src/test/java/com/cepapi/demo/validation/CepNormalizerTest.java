package com.cepapi.demo.validation;

import com.cepapi.demo.exception.InvalidCepException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CepNormalizerTest {

  @Test
  void shouldKeepEightDigits() {
    assertThat(CepNormalizer.normalize("01001000")).isEqualTo("01001000");
  }

  @Test
  void shouldRemoveFormattingHyphen() {
    assertThat(CepNormalizer.normalize("01001-000")).isEqualTo("01001000");
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"0100100", "010010000", "0100A000", "01001 000", "01.001-000"})
  void shouldRejectInvalidCep(String value) {
    assertThatThrownBy(() -> CepNormalizer.normalize(value))
      .isInstanceOf(InvalidCepException.class)
      .hasMessage("CEP deve conter oito dígitos, com hífen opcional no formato 00000-000");
  }
}
