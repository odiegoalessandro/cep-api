package com.cepapi.demo.config;

import com.azure.data.tables.TableClient;
import com.azure.data.tables.TableClientBuilder;
import com.azure.identity.DefaultAzureCredentialBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AzureTableConfig {

  @Bean
  public TableClient cepTableClient(
    @Value("${azure.storage.tables.endpoint}") String endpoint,
    @Value("${azure.storage.tables.cep-table}") String tableName
  ) {
    return new TableClientBuilder()
      .endpoint(endpoint)
      .tableName(tableName)
      .credential(new DefaultAzureCredentialBuilder().build())
      .buildClient();
  }
}