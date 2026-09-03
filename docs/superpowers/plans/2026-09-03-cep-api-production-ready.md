# CEP API Production Readiness Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Finalizar a CEP API como projeto público verificável, com fallback resiliente, cache tolerante a falhas, contrato HTTP consistente, testes, empacotamento Azure Functions e CI.

**Architecture:** O `CepService` continuará sendo o orquestrador do fluxo cache-aside e da cadeia ordenada de provedores. Entradas serão normalizadas na borda do domínio, falhas Azure serão traduzidas pelo repositório e falhas HTTP externas serão classificadas pelo serviço. O controller exporá um contrato REST pequeno e um advice converterá exceções de aplicação em `ProblemDetail`.

**Tech Stack:** Java 21, Spring Boot 4.0.7, Spring MVC, Spring RestClient, Azure Data Tables, Spring Cloud Function Azure Web Adapter, Maven, JUnit 5, Mockito, AssertJ, MockMvc e GitHub Actions.

**Spec:** `docs/superpowers/specs/2026-09-03-cep-api-production-ready-design.md`

## Global Constraints

- Aceitar somente CEP com oito dígitos ou no formato `00000-000`.
- Retornar sempre o CEP canônico com oito dígitos.
- Usar timeout de conexão de 1 segundo e timeout de leitura de 2 segundos por padrão.
- Não aplicar retry, circuit breaker ou rate limiting nesta entrega.
- Tratar o Azure Table Storage como cache best effort depois da inicialização da configuração.
- Retornar 400 para entrada inválida, 404 para CEP ausente e 503 quando todos os provedores falharem.
- Usar connection string apenas por variável de ambiente e nunca versionar credenciais.
- Não declarar implantação pública no Azure.
- Preservar a ordem ViaCEP, OpenCEP e BrasilAPI.
- Capturar apenas exceções esperadas nas fronteiras de infraestrutura.

## File Map

### Novos arquivos de produção

- `src/main/java/com/cepapi/demo/validation/CepNormalizer.java`: valida e converte a entrada para oito dígitos.
- `src/main/java/com/cepapi/demo/exception/InvalidCepException.java`: representa entrada fora do contrato.
- `src/main/java/com/cepapi/demo/exception/CepNotFoundException.java`: representa ausência confirmada pelos provedores.
- `src/main/java/com/cepapi/demo/exception/CepProvidersUnavailableException.java`: representa falha de todos os provedores.
- `src/main/java/com/cepapi/demo/exception/CepCacheException.java`: isola falhas da SDK Azure.
- `src/main/java/com/cepapi/demo/controller/ApiExceptionHandler.java`: converte exceções em `ProblemDetail`.

### Novos testes

- `src/test/java/com/cepapi/demo/validation/CepNormalizerTest.java`
- `src/test/java/com/cepapi/demo/repository/mapper/CepTableMapperTest.java`
- `src/test/java/com/cepapi/demo/repository/CepRepositoryTest.java`
- `src/test/java/com/cepapi/demo/service/CepServiceTest.java`
- `src/test/java/com/cepapi/demo/controller/CepControllerTest.java`

### Arquivos modificados

- `src/main/java/com/cepapi/demo/domain/Cep.java`: remove Lombok e adiciona cópia imutável com CEP canônico.
- `src/main/java/com/cepapi/demo/repository/mapper/CepTableMapper.java`: usa o normalizador e construtor do record.
- `src/main/java/com/cepapi/demo/config/AzureTableConfig.java`: constrói `TableClient` sem operação de rede.
- `src/main/java/com/cepapi/demo/repository/CepRepository.java`: traduz falhas Azure.
- `src/main/java/com/cepapi/demo/service/CepService.java`: implementa cache best effort e fallback classificado.
- `src/main/java/com/cepapi/demo/client/ViaCepClient.java`: explicita o qualifier correto.
- `src/main/java/com/cepapi/demo/controller/CepController.java`: simplifica o caminho de sucesso.
- `src/main/resources/application.properties`: corrige identidade, configuração Azure e timeouts.
- `.env.example`: documenta variáveis realmente consumidas.
- `pom.xml`: remove dependências sem uso e mantém o pacote Azure verificável.
- `README.md`: documenta o comportamento final reproduzível.
- `mvnw`: recebe modo executável.
- `host.json`: será movido para `src/main/resources/host.json`.
- `.github/workflows/ci.yml`: cria a verificação automatizada.

## Task 1: Restaurar build reproduzível e CI

**Files:**

- Modify: `mvnw`
- Move: `host.json` to `src/main/resources/host.json`
- Modify: `.env.example`
- Modify: `src/main/resources/application.properties`
- Modify: `pom.xml:18-111`
- Create: `.github/workflows/ci.yml`

**Interfaces:**

- Consumes: estrutura Maven e configuração Azure atuais.
- Produces: Maven Wrapper executável, pacote Azure verificável e workflow `CI`.

- [ ] **Step 1: Registrar a reprodução dos bloqueios atuais**

Run:

```bash
./mvnw clean verify
```

Expected: FAIL com `Permission denied`, pois `mvnw` está em modo `100644`.

Run:

```bash
git ls-files -s mvnw
```

Expected: primeira coluna igual a `100644`.

- [ ] **Step 2: Corrigir o modo do Maven Wrapper**

Run:

```bash
chmod +x mvnw
git add mvnw
```

Expected: `git diff --cached --summary` mostra mudança para modo `100755`.

- [ ] **Step 3: Mover o host.json para o caminho consumido pelo plugin**

Create `src/main/resources/host.json` with:

```json
{
  "version": "2.0",
  "extensionBundle": {
    "id": "Microsoft.Azure.Functions.ExtensionBundle",
    "version": "[4.*, 5.2.0)"
  }
}
```

Delete the root `host.json` after confirming the contents are identical.

- [ ] **Step 4: Alinhar variáveis e timeouts**

Replace `.env.example` with:

```dotenv
AZURE_STORAGE_CONNECTION_STRING=
AZURE_STORAGE_TABLES_CEP_TABLE=ceps
HTTP_CLIENT_CONNECT_TIMEOUT=1s
HTTP_CLIENT_READ_TIMEOUT=2s
```

Replace `src/main/resources/application.properties` with:

```properties
spring.application.name=cep-api

azure.storage.connection-string=${AZURE_STORAGE_CONNECTION_STRING}
azure.storage.tables.cep-table=${AZURE_STORAGE_TABLES_CEP_TABLE:ceps}

spring.http.clients.connect-timeout=${HTTP_CLIENT_CONNECT_TIMEOUT:1s}
spring.http.clients.read-timeout=${HTTP_CLIENT_READ_TIMEOUT:2s}

server.port=8080
spring.servlet.encoding.charset=UTF-8
spring.servlet.encoding.enabled=true
spring.servlet.encoding.force=true
```

- [ ] **Step 5: Remover a dependência de validação sem uso**

From `pom.xml`, remove:

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

As dependências `azure-identity` e Lombok permanecem temporariamente porque o código atual ainda importa suas classes. Elas serão removidas nas tarefas que eliminam esses usos.

- [ ] **Step 6: Criar o workflow de CI**

Create `.github/workflows/ci.yml`:

```yaml
name: CI

on:
  push:
  pull_request:
    branches:
      - master

permissions:
  contents: read

jobs:
  verify:
    runs-on: ubuntu-latest
    timeout-minutes: 10

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up Java 21
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "21"
          cache: maven

      - name: Verify
        run: ./mvnw -B -ntp clean verify

      - name: Verify Azure Functions package
        run: test -f target/azure-functions/cep-api/host.json
```

- [ ] **Step 7: Executar a verificação completa**

Run:

```bash
./mvnw -B -ntp clean verify
test -f target/azure-functions/cep-api/host.json
```

Expected: Maven exits 0, the six existing client tests pass and the Azure package contains `host.json`.

If Maven Central is unreachable in the local runtime, push this isolated commit to the feature branch and use the `CI` workflow as the execution environment. Record the workflow URL and do not infer success from static inspection.

- [ ] **Step 8: Commit**

```bash
git add .env.example .github/workflows/ci.yml pom.xml src/main/resources/host.json src/main/resources/application.properties mvnw
git add -u host.json
git commit -m "build: add reproducible verification pipeline"
```

## Task 2: Validar e normalizar CEP

**Files:**

- Create: `src/main/java/com/cepapi/demo/validation/CepNormalizer.java`
- Create: `src/main/java/com/cepapi/demo/exception/InvalidCepException.java`
- Modify: `pom.xml`
- Modify: `src/main/java/com/cepapi/demo/domain/Cep.java`
- Modify: `src/main/java/com/cepapi/demo/repository/mapper/CepTableMapper.java`
- Create: `src/test/java/com/cepapi/demo/validation/CepNormalizerTest.java`
- Create: `src/test/java/com/cepapi/demo/repository/mapper/CepTableMapperTest.java`

**Interfaces:**

- Produces: `CepNormalizer.normalize(String): String`.
- Produces: `Cep.withCep(String): Cep`.
- Produces: `InvalidCepException(): RuntimeException`.
- Consumers: `CepRepository` and `CepService` in later tasks.

- [ ] **Step 1: Escrever testes falhando do normalizador**

Create `CepNormalizerTest.java`:

```java
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
```

- [ ] **Step 2: Executar o teste para confirmar falha**

Run:

```bash
./mvnw -B -ntp -Dtest=CepNormalizerTest test
```

Expected: FAIL porque `CepNormalizer` e `InvalidCepException` não existem.

- [ ] **Step 3: Implementar exceção e normalizador mínimos**

Create `InvalidCepException.java`:

```java
package com.cepapi.demo.exception;

public class InvalidCepException extends RuntimeException {

  public InvalidCepException() {
    super("CEP deve conter oito dígitos, com hífen opcional no formato 00000-000");
  }
}
```

Create `CepNormalizer.java`:

```java
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
```

- [ ] **Step 4: Remover Lombok do domínio e garantir cópia canônica**

Remove the Lombok dependency from `pom.xml` in the same change that removes `@Builder` from `Cep`.

Replace `Cep.java` with:

```java
package com.cepapi.demo.domain;

public record Cep(
  String cep,
  String logradouro,
  String complemento,
  String bairro,
  String cidade,
  String uf,
  String ibge,
  String ddd
) {

  public Cep withCep(String canonicalCep) {
    return new Cep(
      canonicalCep,
      logradouro,
      complemento,
      bairro,
      cidade,
      uf,
      ibge,
      ddd
    );
  }
}
```

- [ ] **Step 5: Testar o mapeamento Azure**

Create `CepTableMapperTest.java`:

```java
package com.cepapi.demo.repository.mapper;

import com.cepapi.demo.domain.Cep;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CepTableMapperTest {

  @Test
  void shouldMapCepToTableEntityAndBack() {
    var cep = new Cep(
      "01001-000",
      "Praça da Sé",
      "lado ímpar",
      "Sé",
      "São Paulo",
      "SP",
      "3550308",
      "11"
    );

    var entity = CepTableMapper.toTableEntity(cep);
    var restored = CepTableMapper.toDomain(entity);

    assertThat(entity.getPartitionKey()).isEqualTo("01");
    assertThat(entity.getRowKey()).isEqualTo("01001000");
    assertThat(restored).isEqualTo(cep.withCep("01001000"));
  }

  @Test
  void shouldPreserveMissingOptionalFields() {
    var cep = new Cep(
      "01001000",
      "Praça da Sé",
      null,
      "Sé",
      "São Paulo",
      "SP",
      null,
      null
    );

    var restored = CepTableMapper.toDomain(CepTableMapper.toTableEntity(cep));

    assertThat(restored.complemento()).isNull();
    assertThat(restored.ibge()).isNull();
    assertThat(restored.ddd()).isNull();
  }
}
```

- [ ] **Step 6: Executar o teste do mapper para confirmar falha**

Run:

```bash
./mvnw -B -ntp -Dtest=CepTableMapperTest test
```

Expected: FAIL depois da remoção do builder até o mapper ser atualizado.

- [ ] **Step 7: Atualizar o mapper**

Use `CepNormalizer.normalize(cep.cep())` before creating the `TableEntity`. Replace `Cep.builder()` in `toDomain` with the record constructor:

```java
return new Cep(
  entity.getRowKey(),
  property(entity, "logradouro"),
  property(entity, "complemento"),
  property(entity, "bairro"),
  property(entity, "cidade"),
  property(entity, "uf"),
  property(entity, "ibge"),
  property(entity, "ddd")
);
```

Delete `CepTableMapper.normalize`. `partitionKey` must receive an already normalized value and return `value.substring(0, 2)`.

- [ ] **Step 8: Executar testes focados e suíte completa**

Run:

```bash
./mvnw -B -ntp -Dtest=CepNormalizerTest,CepTableMapperTest test
./mvnw -B -ntp test
```

Expected: PASS.

- [ ] **Step 9: Commit**

```bash
git add pom.xml src/main/java/com/cepapi/demo/domain/Cep.java src/main/java/com/cepapi/demo/exception/InvalidCepException.java src/main/java/com/cepapi/demo/validation/CepNormalizer.java src/main/java/com/cepapi/demo/repository/mapper/CepTableMapper.java src/test/java/com/cepapi/demo/validation/CepNormalizerTest.java src/test/java/com/cepapi/demo/repository/mapper/CepTableMapperTest.java
git commit -m "feat: validate and canonicalize CEP values"
```

## Task 3: Isolar e tolerar falhas do cache Azure

**Files:**

- Create: `src/main/java/com/cepapi/demo/exception/CepCacheException.java`
- Modify: `src/main/java/com/cepapi/demo/config/AzureTableConfig.java`
- Modify: `src/main/java/com/cepapi/demo/repository/CepRepository.java`
- Modify: `pom.xml`
- Create: `src/test/java/com/cepapi/demo/repository/CepRepositoryTest.java`

**Interfaces:**

- Consumes: `CepNormalizer.normalize(String)` and `CepTableMapper`.
- Produces: `CepRepository.findByCep(String): Optional<Cep>`.
- Produces: `CepRepository.save(Cep): void`.
- Produces: `CepCacheException(String, Throwable): RuntimeException`.
- Consumer: `CepService` in Task 4.

- [ ] **Step 1: Escrever testes falhando do repositório**

Create `CepRepositoryTest.java`:

```java
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
```

- [ ] **Step 2: Executar o teste para confirmar falha**

Run:

```bash
./mvnw -B -ntp -Dtest=CepRepositoryTest test
```

Expected: FAIL porque a exceção própria não existe e erros Azure ainda escapam diretamente.

- [ ] **Step 3: Implementar a exceção de cache**

Create `CepCacheException.java`:

```java
package com.cepapi.demo.exception;

public class CepCacheException extends RuntimeException {

  public CepCacheException(String message, Throwable cause) {
    super(message, cause);
  }
}
```

- [ ] **Step 4: Remover operação de rede do startup**

Replace the bean body in `AzureTableConfig` with:

```java
return new TableClientBuilder()
  .connectionString(connectionString)
  .tableName(tableName)
  .buildClient();
```

Remove imports for `TableServiceClient`, `TableServiceClientBuilder` and `DefaultAzureCredentialBuilder`. Annotate the configuration with `@Configuration(proxyBeanMethods = false)`.

Remove the `azure-identity` dependency from `pom.xml` in the same change.

- [ ] **Step 5: Traduzir falhas da SDK no repositório**

Replace `CepRepository.java` with:

```java
package com.cepapi.demo.repository;

import com.azure.core.exception.AzureException;
import com.azure.core.exception.HttpResponseException;
import com.azure.data.tables.TableClient;
import com.cepapi.demo.domain.Cep;
import com.cepapi.demo.exception.CepCacheException;
import com.cepapi.demo.repository.mapper.CepTableMapper;
import com.cepapi.demo.validation.CepNormalizer;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class CepRepository {

  private final TableClient tableClient;

  public CepRepository(TableClient tableClient) {
    this.tableClient = tableClient;
  }

  public Optional<Cep> findByCep(String cep) {
    var canonicalCep = CepNormalizer.normalize(cep);

    try {
      var entity = tableClient.getEntity(
        CepTableMapper.partitionKey(canonicalCep),
        canonicalCep
      );

      return Optional.of(CepTableMapper.toDomain(entity));
    } catch (HttpResponseException exception) {
      if (exception.getResponse() != null
        && exception.getResponse().getStatusCode() == 404) {
        return Optional.empty();
      }

      throw readFailure(exception);
    } catch (AzureException exception) {
      throw readFailure(exception);
    }
  }

  public void save(Cep cep) {
    var entity = CepTableMapper.toTableEntity(cep);

    try {
      tableClient.upsertEntity(entity);
    } catch (AzureException exception) {
      throw new CepCacheException("Falha ao salvar o CEP no cache", exception);
    }
  }

  private CepCacheException readFailure(AzureException cause) {
    return new CepCacheException("Falha ao consultar o cache de CEP", cause);
  }
}
```

- [ ] **Step 6: Executar testes focados e suíte completa**

Run:

```bash
./mvnw -B -ntp -Dtest=CepRepositoryTest test
./mvnw -B -ntp test
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add pom.xml src/main/java/com/cepapi/demo/config/AzureTableConfig.java src/main/java/com/cepapi/demo/exception/CepCacheException.java src/main/java/com/cepapi/demo/repository/CepRepository.java src/test/java/com/cepapi/demo/repository/CepRepositoryTest.java
git commit -m "feat: isolate Azure cache failures"
```

## Task 4: Implementar fallback e semântica de indisponibilidade

**Files:**

- Create: `src/main/java/com/cepapi/demo/exception/CepNotFoundException.java`
- Create: `src/main/java/com/cepapi/demo/exception/CepProvidersUnavailableException.java`
- Modify: `src/main/java/com/cepapi/demo/service/CepService.java`
- Modify: `src/main/java/com/cepapi/demo/client/ViaCepClient.java`
- Create: `src/test/java/com/cepapi/demo/service/CepServiceTest.java`

**Interfaces:**

- Consumes: `CepNormalizer`, `CepRepository`, ordered `List<CepClient>` and `CepCacheException`.
- Produces: `CepService.findByCep(String): Cep`.
- Produces: `CepNotFoundException(String): RuntimeException`.
- Produces: `CepProvidersUnavailableException(): RuntimeException`.
- Consumer: `CepController` in Task 5.

- [ ] **Step 1: Escrever testes falhando do serviço**

Create `CepServiceTest.java`:

```java
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
```

- [ ] **Step 2: Executar o teste para confirmar falha**

Run:

```bash
./mvnw -B -ntp -Dtest=CepServiceTest test
```

Expected: FAIL porque o serviço ainda retorna `Optional<Cep>` e interrompe em falhas.

- [ ] **Step 3: Implementar exceções da aplicação**

Create `CepNotFoundException.java`:

```java
package com.cepapi.demo.exception;

public class CepNotFoundException extends RuntimeException {

  public CepNotFoundException(String cep) {
    super("CEP não encontrado: " + cep);
  }
}
```

Create `CepProvidersUnavailableException.java`:

```java
package com.cepapi.demo.exception;

public class CepProvidersUnavailableException extends RuntimeException {

  public CepProvidersUnavailableException() {
    super("Os provedores de CEP estão temporariamente indisponíveis");
  }
}
```

- [ ] **Step 4: Reescrever a orquestração do serviço**

Replace `CepService.java` with:

```java
package com.cepapi.demo.service;

import com.cepapi.demo.client.CepClient;
import com.cepapi.demo.domain.Cep;
import com.cepapi.demo.exception.CepCacheException;
import com.cepapi.demo.exception.CepNotFoundException;
import com.cepapi.demo.exception.CepProvidersUnavailableException;
import com.cepapi.demo.repository.CepRepository;
import com.cepapi.demo.validation.CepNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Optional;

@Service
public class CepService {

  private static final Logger log = LoggerFactory.getLogger(CepService.class);

  private final CepRepository cepRepository;
  private final List<CepClient> cepClients;

  public CepService(CepRepository cepRepository, List<CepClient> cepClients) {
    this.cepRepository = cepRepository;
    this.cepClients = cepClients;
  }

  public Cep findByCep(String value) {
    var cep = CepNormalizer.normalize(value);
    var cachedCep = findInCache(cep);

    if (cachedCep.isPresent()) {
      return cachedCep.get();
    }

    var providerResponded = false;

    for (var client : cepClients) {
      try {
        var result = client.findByCep(cep);
        providerResponded = true;

        if (result.isPresent()) {
          var canonicalResult = result.get().withCep(cep);
          saveInCache(canonicalResult);
          return canonicalResult;
        }
      } catch (RestClientException exception) {
        log.warn(
          "CEP provider {} failed with {}",
          client.getClass().getSimpleName(),
          exception.getClass().getSimpleName()
        );
      }
    }

    if (providerResponded) {
      throw new CepNotFoundException(cep);
    }

    throw new CepProvidersUnavailableException();
  }

  private Optional<Cep> findInCache(String cep) {
    try {
      return cepRepository.findByCep(cep);
    } catch (CepCacheException exception) {
      log.warn("CEP cache read failed: {}", exception.getMessage());
      return Optional.empty();
    }
  }

  private void saveInCache(Cep cep) {
    try {
      cepRepository.save(cep);
    } catch (CepCacheException exception) {
      log.warn("CEP cache write failed: {}", exception.getMessage());
    }
  }
}
```

- [ ] **Step 5: Explicitar o bean ViaCEP**

Add `@Qualifier("viaCepRestClient")` to the `ViaCepClient` constructor parameter. This removes reliance on parameter-name matching when three `RestClient` beans exist.

- [ ] **Step 6: Executar testes focados e suíte completa**

Run:

```bash
./mvnw -B -ntp -Dtest=CepServiceTest test
./mvnw -B -ntp test
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/cepapi/demo/client/ViaCepClient.java src/main/java/com/cepapi/demo/exception/CepNotFoundException.java src/main/java/com/cepapi/demo/exception/CepProvidersUnavailableException.java src/main/java/com/cepapi/demo/service/CepService.java src/test/java/com/cepapi/demo/service/CepServiceTest.java
git commit -m "feat: add resilient provider fallback"
```

## Task 5: Padronizar contrato HTTP com ProblemDetail

**Files:**

- Create: `src/main/java/com/cepapi/demo/controller/ApiExceptionHandler.java`
- Modify: `src/main/java/com/cepapi/demo/controller/CepController.java`
- Create: `src/test/java/com/cepapi/demo/controller/CepControllerTest.java`

**Interfaces:**

- Consumes: `CepService.findByCep(String): Cep` and the three application exceptions.
- Produces: `GET /ceps/{cep}` with statuses 200, 400, 404 and 503.

- [ ] **Step 1: Escrever testes falhando do controller**

Create `CepControllerTest.java`:

```java
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
```

- [ ] **Step 2: Executar o teste para confirmar falha**

Run:

```bash
./mvnw -B -ntp -Dtest=CepControllerTest test
```

Expected: FAIL porque `ApiExceptionHandler` não existe e o controller espera `Optional`.

- [ ] **Step 3: Implementar o handler**

Create `ApiExceptionHandler.java`:

```java
package com.cepapi.demo.controller;

import com.cepapi.demo.exception.CepNotFoundException;
import com.cepapi.demo.exception.CepProvidersUnavailableException;
import com.cepapi.demo.exception.InvalidCepException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(InvalidCepException.class)
  ProblemDetail handleInvalidCep(InvalidCepException exception) {
    return problem(HttpStatus.BAD_REQUEST, "CEP inválido", exception.getMessage());
  }

  @ExceptionHandler(CepNotFoundException.class)
  ProblemDetail handleNotFound(CepNotFoundException exception) {
    return problem(HttpStatus.NOT_FOUND, "CEP não encontrado", exception.getMessage());
  }

  @ExceptionHandler(CepProvidersUnavailableException.class)
  ProblemDetail handleProvidersUnavailable(CepProvidersUnavailableException exception) {
    return problem(
      HttpStatus.SERVICE_UNAVAILABLE,
      "Serviço temporariamente indisponível",
      exception.getMessage()
    );
  }

  private ProblemDetail problem(HttpStatus status, String title, String detail) {
    var problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setTitle(title);
    return problem;
  }
}
```

- [ ] **Step 4: Simplificar o controller**

Replace `findByCep` with:

```java
@GetMapping("/{cep}")
public Cep findByCep(@PathVariable String cep) {
  return cepService.findByCep(cep);
}
```

Remove wildcard, `ResponseEntity` and unused `Map` imports.

- [ ] **Step 5: Executar testes focados e suíte completa**

Run:

```bash
./mvnw -B -ntp -Dtest=CepControllerTest test
./mvnw -B -ntp clean verify
```

Expected: PASS and Azure package generated.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/cepapi/demo/controller/ApiExceptionHandler.java src/main/java/com/cepapi/demo/controller/CepController.java src/test/java/com/cepapi/demo/controller/CepControllerTest.java
git commit -m "feat: expose consistent CEP error responses"
```

## Task 6: Documentar, revisar e publicar a branch

**Files:**

- Modify: `README.md`
- Verify: every changed production and test file.

**Interfaces:**

- Consumes: comportamento final verificado pelas Tasks 1 through 5.
- Produces: documentação fiel, pull request revisável e evidência para a postagem.

- [ ] **Step 1: Reescrever o README com o comportamento final**

The README must include these sections and facts:

- `Visão geral`: cache-aside com Azure Table Storage e três provedores.
- `Fluxo`: validação, cache, ViaCEP, OpenCEP, BrasilAPI, gravação best effort.
- `Contrato HTTP`: examples for 200, 400, 404 and 503.
- `Resiliência`: timeout de 1 segundo para conexão, 2 segundos para leitura, no retry and best-effort cache.
- `Configuração`: the four exact variables from `.env.example` and a warning that `.env` is not loaded automatically.
- `Execução local`: export variables and run `./mvnw spring-boot:run`.
- `Testes`: run `./mvnw -B -ntp clean verify`.
- `Azure Functions`: package with Maven and state explicitly that this repository does not claim a public deployment.
- `CI`: link and badge for `.github/workflows/ci.yml`.
- `Limitações`: no retry, rate limit, live Azure integration test or public endpoint.

Remove Lombok from the technology list. Do not claim coverage percentage, production use or live deployment.

- [ ] **Step 2: Verificar ausência de segredos**

Run:

```bash
git grep -n -E 'AccountKey=|DefaultEndpointsProtocol=|AZURE_STORAGE_CONNECTION_STRING=.+'
```

Expected: no matches containing credential values. Property names and empty examples are allowed.

- [ ] **Step 3: Executar verificação final**

Run:

```bash
./mvnw -B -ntp clean verify
test -f target/azure-functions/cep-api/host.json
git diff --check master...HEAD
git status --short
```

Expected: Maven and package checks exit 0, no whitespace errors and only intentional files pending before the documentation commit.

- [ ] **Step 4: Commit da documentação**

```bash
git add README.md
git commit -m "docs: document resilient CEP API"
```

- [ ] **Step 5: Publicar branch e abrir pull request**

Push `feat/cep-api-production-ready` and open a PR to `master` titled:

```text
feat: finish CEP API for portfolio
```

PR body:

```markdown
## Summary

- validates and canonicalizes CEP input
- keeps external lookup available when the Azure cache fails
- falls back across providers on timeout and server errors
- returns consistent RFC 9457 problem responses
- adds service, repository, mapper and controller tests
- verifies the Azure Functions package in GitHub Actions
- aligns configuration and documentation with the implementation

## Verification

- `./mvnw -B -ntp clean verify`
- Azure Functions package contains `host.json`
- GitHub Actions workflow completed successfully
```

- [ ] **Step 6: Confirmar CI e revisar diff do PR**

Required evidence:

- Workflow conclusion equals `success`.
- No secret scanning alert or committed credential.
- PR contains only the files listed in this plan.
- README statements match tests and implementation.

- [ ] **Step 7: Preparar a postagem do LinkedIn fora do repositório**

Write the post only after Step 6. It may mention Java 21, Spring Boot, RestClient, Azure Table Storage, Azure Functions packaging, ordered fallback, cache best effort, JUnit, Mockito, MockMvc and GitHub Actions.

It must not mention a public Azure deployment, production traffic, measured performance improvement or coverage percentage.
