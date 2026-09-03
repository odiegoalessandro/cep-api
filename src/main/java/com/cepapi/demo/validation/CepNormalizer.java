package com.cepapi.demo.validation;

import com.cepapi.demo.exception.InvalidCepException;

import java.util.regex.Pattern;

public final class CepNormalizer {

  private static final Pattern PLAIN_CEP = Pattern.compile("\\d{8}");
  private static final Pattern FORMATTED_CEP = Pattern.compile("\\d{5}-\\d{3}");

  private CepNormalizer() {
  }

  public static String normalize(String value) {
    if (value == null || !isValid(value)) {
      throw new InvalidCepException();
    }

    return value.replace("-", "");
  }

  private static boolean isValid(String value) {
    return PLAIN_CEP.matcher(value).matches()
      || FORMATTED_CEP.matcher(value).matches();
  }
}
