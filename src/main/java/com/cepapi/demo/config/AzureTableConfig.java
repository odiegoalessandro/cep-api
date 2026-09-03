package com.cepapi.demo.config;

import com.azure.data.tables.TableClient;
import com.azure.data.tables.TableClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class AzureTableConfig {

  @Bean
  public TableClient cepTableClient(
    @Value("${azure.storage.connection-string}") String connectionString,
    @Value("${azure.storage.tables.cep-table}") String tableName
  ) {
    return new TableClientBuilder()
      .connectionString(connectionString)
      .tableName(tableName)
      .buildClient();
  }
}
