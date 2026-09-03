# CEP API

[![CI](https://github.com/odiegoalessandro/cep-api/actions/workflows/ci.yml/badge.svg)](https://github.com/odiegoalessandro/cep-api/actions/workflows/ci.yml)

API REST para consultar endereços brasileiros por CEP, construída com Java 21 e Spring Boot.

O projeto aplica cache-aside com Azure Table Storage e fallback ordenado entre ViaCEP, OpenCEP e BrasilAPI. Falhas do cache não impedem a consulta aos provedores externos.

## Tecnologias

- Java 21
- Spring Boot 4
- Spring Web MVC
- Spring RestClient
- Azure Table Storage
- Azure Functions
- Maven Wrapper
- JUnit 5, Mockito, AssertJ e MockMvc
- GitHub Actions

## Fluxo da consulta

1. Recebe `GET /ceps/{cep}`.
2. Aceita oito dígitos ou o formato `00000-000`.
3. Normaliza o CEP para oito dígitos.
4. Consulta o Azure Table Storage.
5. Em caso de cache miss ou falha do cache, consulta ViaCEP, OpenCEP e BrasilAPI nessa ordem.
6. Normaliza e grava o primeiro resultado encontrado no cache.
7. Se a gravação falhar, ainda retorna o resultado do provedor.
8. Retorna `404` quando ao menos um provedor responde sem encontrar o CEP.
9. Retorna `503` quando todos os provedores falham.

## Contrato HTTP

### Consulta válida

```http
GET /ceps/01001-000
```

Resposta `200 OK`:

```json
{
  "cep": "01001000",
  "logradouro": "Praça da Sé",
  "complemento": "lado ímpar",
  "bairro": "Sé",
  "cidade": "São Paulo",
  "uf": "SP",
  "ibge": "3550308",
  "ddd": "11"
}
```

O campo `cep` sempre é retornado com oito dígitos. Campos não informados pelo provedor podem ser `null`.

### CEP inválido

Resposta `400 Bad Request` com `application/problem+json`:

```json
{
  "title": "CEP inválido",
  "status": 400,
  "detail": "CEP deve conter oito dígitos, com hífen opcional no formato 00000-000"
}
```

### CEP não encontrado

Resposta `404 Not Found` com `application/problem+json`:

```json
{
  "title": "CEP não encontrado",
  "status": 404,
  "detail": "CEP não encontrado: 99999999"
}
```

### Provedores indisponíveis

Resposta `503 Service Unavailable` com `application/problem+json`:

```json
{
  "title": "Serviço temporariamente indisponível",
  "status": 503,
  "detail": "Os provedores de CEP estão temporariamente indisponíveis"
}
```

## Resiliência

- Timeout de conexão padrão: 1 segundo.
- Timeout de leitura padrão: 2 segundos.
- Cache tratado como best effort durante as consultas.
- Fallback sequencial preservando a ordem dos provedores.
- Sem retry nesta versão.

## Configuração

Variáveis disponíveis em `.env.example`:

```dotenv
AZURE_STORAGE_CONNECTION_STRING=
AZURE_STORAGE_TABLES_CEP_TABLE=ceps
HTTP_CLIENT_CONNECT_TIMEOUT=1s
HTTP_CLIENT_READ_TIMEOUT=2s
```

O Spring Boot não carrega `.env` automaticamente. Exporte as variáveis no shell ou configure-as no ambiente de execução.

A tabela informada por `AZURE_STORAGE_TABLES_CEP_TABLE` deve existir. A aplicação não executa operações de rede durante a criação do bean `TableClient`.

## Execução local

Pré-requisitos:

- JDK 21
- Azure Table Storage acessível
- Tabela `ceps` criada ou outro nome configurado

```bash
export AZURE_STORAGE_CONNECTION_STRING='<connection-string>'
export AZURE_STORAGE_TABLES_CEP_TABLE='ceps'
export HTTP_CLIENT_CONNECT_TIMEOUT='1s'
export HTTP_CLIENT_READ_TIMEOUT='2s'
./mvnw spring-boot:run
```

Consulta:

```bash
curl http://localhost:8080/ceps/01001000
```

## Testes e CI

```bash
./mvnw -B -ntp clean verify
```

O workflow `CI` executa a suíte completa com Java 21 e verifica se o pacote do Azure Functions contém `host.json`.

## Azure Functions

O plugin `azure-functions-maven-plugin` gera o pacote durante o ciclo Maven:

```bash
./mvnw -B -ntp clean package
```

Saída esperada:

```text
target/azure-functions/cep-api/
```

Este repositório valida o empacotamento, mas não declara uma implantação pública no Azure.

## Estrutura principal

```text
src/main/java/com/cepapi/demo
├── client/       Clientes ViaCEP, OpenCEP e BrasilAPI
├── config/       RestClient e Azure Table Storage
├── controller/   Endpoint REST e tratamento de erros
├── domain/       Modelo de domínio
├── dto/          Contratos dos provedores externos
├── exception/    Exceções de aplicação e infraestrutura
├── mapper/       Conversão de DTOs
├── repository/   Cache no Azure Table Storage
├── service/      Cache-aside e fallback
└── validation/   Validação e normalização de CEP
```

## Limitações conhecidas

- Não possui retry, circuit breaker ou rate limiting.
- Não possui teste de integração com uma instância real do Azure Table Storage.
- Não provisiona automaticamente a tabela do cache.
- Não disponibiliza endpoint público neste repositório.
