# CEP API pronta para portfólio

## Objetivo

Finalizar a API de consulta de CEP como um projeto público demonstrável para uma posição de Backend Java Pleno.

O resultado deve comprovar, por código e automação:

- Java 21 e Spring Boot.
- Integração com três provedores HTTP.
- Cache no Azure Table Storage.
- Fallback tolerante a falhas externas.
- Validação e contrato HTTP previsível.
- Testes automatizados.
- Empacotamento para Azure Functions.
- Integração contínua no GitHub Actions.

A publicação no LinkedIn apontará para o repositório. Uma implantação pública no Azure não faz parte desta entrega e não será alegada.

## Estado atual

O fluxo principal já consulta o Azure Table Storage, percorre ViaCEP, OpenCEP e BrasilAPI e salva resultados encontrados.

Os bloqueios confirmados são:

- O Maven Wrapper está versionado sem permissão de execução.
- `.env.example`, `application.properties` e README usam configurações Azure inconsistentes.
- O `pom.xml` referencia `src/main/resources/host.json`, mas o arquivo está na raiz.
- Timeout e respostas 5xx encerram a requisição sem tentar o próximo provedor.
- Falhas de leitura ou gravação do cache impedem uma resposta que poderia vir dos provedores.
- A entrada não é validada antes de chegar ao particionamento do Azure.
- O CEP pode ser retornado formatado na primeira consulta e sem formatação quando vem do cache.
- Somente os clientes HTTP possuem testes.
- O projeto não possui integração contínua.

## Escopo

### Validação e representação canônica

A API aceitará apenas estes formatos:

- `01001000`
- `01001-000`

O valor será convertido uma única vez para oito dígitos antes de acessar cache ou provedores. A resposta sempre utilizará os oito dígitos, independentemente da fonte.

Entradas nulas, curtas, longas, alfanuméricas ou com pontuação diferente do hífen esperado serão rejeitadas.

### Fluxo da consulta

1. Normalizar e validar o CEP.
2. Consultar o Azure Table Storage.
3. Retornar imediatamente em caso de cache encontrado.
4. Se o cache não contiver o CEP ou estiver indisponível, consultar os provedores na ordem existente.
5. Ao receber um endereço, padronizar o CEP, tentar salvá-lo no cache e retornar a resposta.
6. Se a gravação do cache falhar, registrar a falha e preservar a resposta do provedor.
7. Se ao menos um provedor responder que o CEP não existe e nenhum retornar dados, responder 404.
8. Se todos os provedores falharem por indisponibilidade, responder 503.

O projeto não aplicará retry. Com três provedores sequenciais, retry aumentaria a latência e a carga externa sem benefício necessário para este escopo.

### Timeouts

Os clientes HTTP usarão os recursos globais do Spring Boot para configurar:

- Timeout de conexão padrão de 1 segundo.
- Timeout de leitura padrão de 2 segundos.

Os valores poderão ser sobrescritos por variáveis de ambiente. Assim, a pior latência permanece limitada e o fallback é acionado de forma previsível.

### Cache tolerante a falhas

O repositório converterá falhas da SDK Azure em uma exceção própria de infraestrutura. Resposta 404 do Azure continuará representando ausência de cache.

A configuração construirá o `TableClient` sem executar operações de rede durante a inicialização da aplicação. A tabela deverá ser provisionada previamente. Assim, uma indisponibilidade temporária do Azure depois que a configuração for carregada não impede a aplicação de iniciar nem de consultar os provedores.

O serviço tratará o cache como otimização:

- Falha de leitura aciona os provedores.
- Falha de gravação não invalida um endereço encontrado.
- As duas situações serão registradas sem expor credenciais.

### Contrato HTTP

O endpoint permanecerá:

```http
GET /ceps/{cep}
```

Respostas:

- 200 para endereço encontrado.
- 400 para formato inválido.
- 404 para CEP não encontrado.
- 503 quando todos os provedores estiverem indisponíveis.

Erros usarão `ProblemDetail`, com título e detalhe estáveis.

### Configuração Azure

Esta entrega manterá autenticação por connection string, recebida exclusivamente por variável de ambiente. Nenhum segredo será versionado.

Configurações públicas:

```properties
azure.storage.connection-string=${AZURE_STORAGE_CONNECTION_STRING}
azure.storage.tables.cep-table=${AZURE_STORAGE_TABLES_CEP_TABLE:ceps}
```

O suporte a Managed Identity fica fora deste escopo porque exige configuração e validação em uma assinatura Azure real.

A criação automática da tabela durante a inicialização será removida. O README explicará como informar uma tabela existente, com `ceps` como nome padrão.

O `host.json` será movido para `src/main/resources`, conforme o caminho configurado no plugin e o exemplo oficial do Spring Cloud Function.

## Componentes alterados

- `CepService`: orquestra cache, fallback, classificação de indisponibilidade e resposta final.
- `AzureTableConfig`: cria o cliente sem chamada de rede durante o startup.
- `CepRepository`: traduz `AzureException` da SDK para uma exceção de infraestrutura.
- `CepController`: retorna o domínio no caminho de sucesso e delega erros ao handler.
- `RestClientConfig` e `application.properties`: recebem configuração previsível de timeouts.
- `Cep`: oferece uma forma imutável de aplicar o CEP canônico ao resultado externo.
- Novo normalizador de CEP: valida o contrato e produz oito dígitos.
- Novas exceções específicas para entrada inválida, CEP ausente, provedores indisponíveis e cache.
- Novo handler REST baseado em `ProblemDetail`.
- Maven e recursos Azure: corrigem wrapper e empacotamento.
- GitHub Actions: executa build, testes e valida o pacote Azure.
- README: documenta arquitetura, configuração real, execução e respostas.

## Tratamento de falhas

Os clientes continuarão convertendo respostas 404 em resultado vazio. Exceções do `RestClient`, incluindo timeout, erro 5xx e falha de desserialização, serão capturadas pelo serviço somente na fronteira de cada provedor.

O serviço registrará o nome do cliente e a categoria da falha. Não serão registrados connection strings, cabeçalhos ou corpos completos de respostas externas.

Exceções de programação não serão capturadas genericamente. Isso evita mascarar defeitos internos como indisponibilidade externa.

## Testes

### Serviço

- Retorna o cache sem chamar provedores.
- Normaliza CEP formatado.
- Salva e retorna o primeiro resultado encontrado.
- Avança quando um provedor não encontra o CEP.
- Avança quando um provedor falha.
- Retorna 404 quando houve resposta válida sem resultado.
- Retorna 503 quando todos os provedores falham.
- Continua quando a leitura do cache falha.
- Retorna o endereço quando a gravação do cache falha.

### Controller e erros

- Resposta 200 com corpo canônico.
- Resposta 400 para CEP inválido.
- Resposta 404 para CEP ausente.
- Resposta 503 para provedores indisponíveis.
- Corpo dos erros no formato `ProblemDetail`.

### Repositório e mapeamento

- Entidade Azure encontrada é convertida corretamente.
- Resposta 404 do Azure produz cache vazio.
- Outra falha Azure produz exceção própria.
- Gravação usa row key e partition key canônicas.
- Campos opcionais nulos são preservados.

### Clientes existentes

Os testes atuais de ViaCEP, OpenCEP e BrasilAPI serão preservados. Somente ajustes necessários para o novo contrato serão realizados.

## Integração contínua

O workflow será executado em pushes e pull requests destinados à branch `master`.

Etapas:

1. Checkout.
2. Java 21.
3. Cache de dependências Maven.
4. `clean verify` pelo Maven Wrapper.
5. Verificação da existência do pacote do Azure Functions.

O build não usará credenciais Azure porque testes e empacotamento não devem iniciar conexões reais.

## Documentação e publicação

O README final conterá:

- Objetivo e diferenciais.
- Fluxo de cache e fallback.
- Tecnologias realmente presentes.
- Requisitos e variáveis de ambiente corretas.
- Execução como Spring Boot.
- Testes e CI.
- Empacotamento para Azure Functions.
- Exemplos de sucesso e erro.
- Limitação explícita de que não existe URL pública nesta entrega.

A postagem do LinkedIn só será finalizada depois que o workflow estiver aprovado. Ela poderá afirmar implementação de fallback, cache tolerante a falhas, validação, testes, CI e empacotamento Azure somente após essas evidências existirem no repositório.

## Fora do escopo

- Implantação e URL pública no Azure.
- Managed Identity e configuração de RBAC.
- Retry, circuit breaker e rate limiting.
- Swagger ou OpenAPI.
- Observabilidade externa com Application Insights.
- Testes de integração contra uma conta Azure real.
- Interface gráfica.

Esses itens podem ser adicionados posteriormente sem alterar o contrato definido nesta entrega.

## Critério de conclusão

O projeto estará pronto para a postagem quando:

- Todos os testes automatizados passarem no GitHub Actions.
- O Maven gerar o pacote esperado para Azure Functions.
- O workflow estiver aprovado na branch ou no pull request.
- O README refletir exatamente o comportamento implementado.
- Nenhuma credencial estiver versionada.
- O diff final não contiver funcionalidades não relacionadas.
