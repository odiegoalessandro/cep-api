# CEP API

API em Java/Spring Boot para consultar endereços brasileiros a partir de um CEP.

A aplicação busca o CEP em cache no Azure Table Storage. Caso não encontre, consulta provedores externos em sequência e salva o resultado para próximas consultas.

## O que o projeto faz

- Expõe um endpoint HTTP para consulta de CEP.
- Consulta múltiplas APIs públicas de CEP:
  - ViaCEP
  - OpenCEP
  - BrasilAPI
- Armazena os CEPs consultados no Azure Table Storage.
- Pode ser executado como aplicação Spring Boot ou empacotado para Azure Functions.

## Tecnologias

- Java 21
- Spring Boot
- Spring Web MVC
- Spring RestClient
- Azure Functions
- Azure Table Storage
- Maven
- Lombok

## Como funciona

Fluxo principal da consulta:

1. O cliente chama `GET /ceps/{cep}`.
2. A aplicação procura o CEP no Azure Table Storage.
3. Se encontrar, retorna o endereço salvo.
4. Se não encontrar, consulta os clientes externos na ordem configurada:
   1. ViaCEP
   2. OpenCEP
   3. BrasilAPI
5. Quando algum provedor retorna o endereço, o resultado é salvo no Azure Table Storage.
6. Se nenhum provedor encontrar o CEP, a API retorna `404 Not Found`.

## Endpoint

### Consultar CEP

```http
GET /ceps/{cep}
```

Exemplo:

```bash
curl http://localhost:8080/ceps/01001000
```

Resposta esperada:

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

## Configuração

A aplicação precisa das variáveis de ambiente para acessar o Azure Table Storage:

```env
AZURE_STORAGE_CONNECTION_STRING=
AZURE_STORAGE_TABLES_CEP_TABLE=
```

Crie um arquivo `.env` ou configure essas variáveis no ambiente de execução.

O arquivo `.env.example` mostra as variáveis esperadas pelo projeto.

## Como executar localmente

Pré-requisitos:

- Java 21
- Maven
- Uma conta/storage configurado no Azure Table Storage

Execute:

```bash
mvn spring-boot:run
```

A API ficará disponível em:

```text
http://localhost:8080
```

## Como rodar os testes

```bash
mvn test
```

## Empacotamento para Azure Functions

O projeto possui configuração do plugin `azure-functions-maven-plugin`.

Para gerar o pacote:

```bash
mvn package
```

A aplicação está configurada para usar Java 21 e Azure Functions v4.

## Estrutura principal

```text
src/main/java/com/cepapi/demo
├── client/       # Clientes para ViaCEP, OpenCEP e BrasilAPI
├── config/       # Configurações de RestClient, encoding e Azure Table Storage
├── controller/   # Endpoint HTTP da API
├── domain/       # Modelo de domínio CEP
├── dto/          # Respostas das APIs externas
├── mapper/       # Conversão entre DTOs e domínio
├── repository/   # Persistência no Azure Table Storage
└── service/      # Regra de consulta, cache e fallback entre provedores
```

## Observações

- O CEP é salvo no Azure Table Storage após a primeira consulta bem-sucedida.
- Consultas repetidas para o mesmo CEP tendem a usar o cache salvo.
- Se o CEP não existir nos provedores externos, o endpoint retorna `404`.
