package com.cepapi.demo.config;

import com.azure.data.tables.TableClient;
import com.azure.data.tables.TableClientBuilder;
import com.azure.data.tables.TableServiceClient;
import com.azure.data.tables.TableServiceClientBuilder;
import com.azure.identity.DefaultAzureCredentialBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AzureTableConfig {

  @Bean
  public TableClient cepTableClient(
    @Value("${azure.storage.connection-string}") String connectionString,
    @Value("${azure.storage.tables.cep-table}") String tableName
  ) {
    TableServiceClient serviceClient = new TableServiceClientBuilder()
      .connectionString(connectionString)
      .buildClient();

    serviceClient.createTableIfNotExists(tableName);

    return serviceClient.getTableClient(tableName);
  }
}