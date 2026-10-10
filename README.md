# API Hotel

API REST para gerenciamento de hóspedes, quartos, reservas e registros de pagamentos. Desenvolvida com Java e Spring Boot, utiliza PostgreSQL para persistência e autenticação JWT para controlar o acesso às operações.

O projeto disponibiliza cinco operações de CRUD para cada recurso, além de login, cancelamento de reservas e estorno de pagamentos: **23 operações HTTP**.

## Sumário

- [Funcionalidades](#funcionalidades)
- [Tecnologias e arquitetura](#tecnologias-e-arquitetura)
- [Requisitos](#requisitos)
- [Configuração](#configuração)
- [Execução](#execução)
- [Autenticação e autorização](#autenticação-e-autorização)
- [Endpoints](#endpoints)
- [Filtros e paginação](#filtros-e-paginação)
- [Regras de negócio](#regras-de-negócio)
- [Respostas de erro](#respostas-de-erro)
- [Documentação da API](#documentação-da-api)
- [Testes](#testes)
- [Atualização de bancos existentes](#atualização-de-bancos-existentes)
- [Contribuição](#contribuição)
- [Exemplos por operação](#exemplos-por-operação)

## Funcionalidades

- Cadastro, consulta, atualização e exclusão de hóspedes e quartos.
- Criação e manutenção de reservas com validação de datas, capacidade e disponibilidade.
- Cálculo do valor da hospedagem e preservação da diária contratada.
- Registro de pagamentos parciais ou integrais, com controle do saldo da reserva.
- Cancelamento de reservas e registro de estorno de pagamentos.
- Exclusão lógica de reservas e pagamentos para preservar o histórico.
- Filtros, paginação e ordenação das consultas.
- Autenticação JWT e autorização por perfil.
- Respostas de erro padronizadas e contrato OpenAPI.

Pagamentos e estornos representam **registros internos**. A API não movimenta dinheiro nem realiza integração com adquirentes, bancos ou gateways. Check-in/check-out e gerenciamento de usuários por endpoints não integram a versão atual.

## Tecnologias e arquitetura

| Tecnologia | Uso |
| --- | --- |
| Java 25 | Linguagem e ambiente de execução |
| Spring Boot 4.1.1 | Inicialização e configuração da aplicação |
| Spring Web MVC | Endpoints REST |
| Spring Data JPA / Hibernate | Persistência e consultas |
| Jakarta Validation | Validação dos dados de entrada |
| Spring Security | Autenticação e autorização |
| JWT / Nimbus JOSE | Emissão e validação de tokens |
| PostgreSQL | Banco de dados da aplicação |
| Maven Wrapper | Compilação, execução e testes |
| JUnit, MockMvc e H2 | Testes de serviços e do contrato HTTP |
| Maven Failsafe / PostgreSQL | Testes de integração concorrentes |

O fluxo principal de uma requisição é **Controller → Service → Repository → Banco de dados**. DTOs definem os contratos de entrada e saída; mappers convertem os dados sem expor diretamente as entidades de persistência.

| Pacote em `src/main/java/com/esos/apiHotel` | Responsabilidade |
| --- | --- |
| `controller` | Endpoints HTTP e parâmetros de consulta |
| `service` | Regras de negócio e transações |
| `repository` | Consultas e persistência |
| `entity` | Entidades e relacionamentos |
| `dto/request` | Contratos de entrada e validações |
| `dto/response` | Contratos de saída |
| `mapper` | Conversão de entidades em DTOs |
| `enums` | Valores permitidos de status e forma de pagamento |
| `exception` | Exceções de negócio e tratamento de erros |
| `config` | Configuração de segurança |
| `security` | Autenticação e emissão de JWT |

Uma reserva referencia um hóspede e um quarto. Um pagamento referencia uma reserva. As operações de alteração são transacionais; bloqueios pessimistas coordenam a disponibilidade dos quartos e o saldo das reservas. Reservas e pagamentos também utilizam versionamento otimista.

## Requisitos

- **JDK 25**, com `JAVA_HOME` configurado.
- PostgreSQL acessível, com banco e usuário previamente criados.
- Acesso ao repositório de dependências Maven na primeira execução.
- Docker não é obrigatório.

O Maven Wrapper está incluído no repositório; não é necessário instalar Maven separadamente. Os comandos devem ser executados na pasta que contém `pom.xml`.

## Configuração

### 1. Obter o projeto

Clone este repositório pela URL disponibilizada no GitHub da organização e abra a pasta do projeto no editor de sua preferência.

### 2. Criar o arquivo de configuração local

No PowerShell:

```powershell
Copy-Item .env.example .env
```

No Linux/macOS:

```bash
cp .env.example .env
```

Se já existir um `.env`, preserve os valores atuais e acrescente apenas as variáveis necessárias. O arquivo deve permanecer na raiz do projeto, ao lado de `pom.xml`. Não versione credenciais ou chaves privadas.

### 3. Definir as variáveis

| Variável | Descrição | Padrão / exigência |
| --- | --- | --- |
| `DB_URL` | URL JDBC do PostgreSQL | Obrigatória; exemplo: `jdbc:postgresql://localhost:5432/hotel_db` |
| `DB_USER` | Usuário do banco | Obrigatória |
| `DB_PASSWORD` | Senha do banco | Definir conforme a configuração do PostgreSQL |
| `SERVER_PORT` | Porta HTTP | `8080` |
| `JWT_SECRET` | Chave de assinatura em Base64 | Obrigatória; pelo menos 32 bytes aleatórios antes da codificação |
| `JWT_TOKEN_SECONDS` | Validade do token em segundos | `900`; permitido de 60 a 3600 |
| `ADMIN_USER` | Usuário do perfil ADMIN | `admin` |
| `ADMIN_PASSWORD` | Senha do administrador | Obrigatória; 12 a 72 bytes UTF-8 |
| `RECEPCAO_USER` / `RECEPCAO_PASSWORD` | Credenciais do perfil RECEPCAO | Opcionais; preencher ambas para ativar |
| `FINANCEIRO_USER` / `FINANCEIRO_PASSWORD` | Credenciais do perfil FINANCEIRO | Opcionais; preencher ambas para ativar |
| `CONSULTA_USER` / `CONSULTA_PASSWORD` | Credenciais do perfil CONSULTA | Opcionais; preencher ambas para ativar |

As senhas dos perfis opcionais também devem conter de 12 a 72 bytes UTF-8. Não use os valores do perfil de testes no ambiente da aplicação.

### 4. Gerar a chave JWT

No PowerShell:

```powershell
$jwtBytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($jwtBytes)
[Convert]::ToBase64String($jwtBytes)
$rng.Dispose()
```

No Linux/macOS, com OpenSSL:

```bash
openssl rand -base64 32
```

Copie o resultado para `JWT_SECRET` no `.env`.

### 5. Preparar o PostgreSQL

Crie o banco indicado em `DB_URL` e conceda ao usuário configurado as permissões necessárias para acessar e criar as tabelas no schema utilizado. A aplicação não cria o banco de dados.

A configuração atual utiliza `spring.jpa.hibernate.ddl-auto: update` para criar ou atualizar as tabelas. Em ambientes de produção, adote migrações versionadas e um processo controlado de atualização do banco.

## Execução

No PowerShell ou terminal integrado do VS Code:

```powershell
.\mvnw.cmd spring-boot:run
```

No Linux/macOS:

```bash
chmod +x mvnw
./mvnw spring-boot:run
```

Com a porta padrão, a base da API é `http://localhost:8080/api/v1`. Para encerrar a aplicação no terminal, pressione **Ctrl+C**.

Para gerar o pacote:

```powershell
.\mvnw.cmd package
```

O comando executa os testes da fase `test` e gera o JAR em `target`. Execute o JAR a partir da raiz do projeto, onde o `.env` é carregado:

```powershell
java -jar .\target\apiHotel-0.0.1-SNAPSHOT.jar
```

O empacotamento não substitui a execução da suíte de concorrência com PostgreSQL, realizada na fase `verify`.

## Autenticação e autorização

### Login

Envie `POST /api/v1/auth/login`, com `Content-Type: application/json`:

```json
{
  "username": "admin",
  "password": "SENHA_CONFIGURADA_LOCALMENTE"
}
```

Resposta **200 OK**:

```json
{
  "accessToken": "<JWT_EMITIDO_PELO_LOGIN>",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

Os valores acima são ilustrativos. Utilize as credenciais configuradas no `.env`; o marcador do exemplo não é um token válido. O login retorna `Cache-Control: no-store`.

Nas demais chamadas da API, envie:

```http
Authorization: Bearer <JWT_EMITIDO_PELO_LOGIN>
```

Exemplo no PowerShell, sem exibir a senha digitada:

```powershell
$credencial = Get-Credential -Message "Informe as credenciais configuradas para a API"
$body = @{
    username = $credencial.UserName
    password = $credencial.GetNetworkCredential().Password
} | ConvertTo-Json

$auth = Invoke-RestMethod -Method Post `
    -Uri http://localhost:8080/api/v1/auth/login `
    -ContentType application/json -Body $body

$headers = @{ Authorization = "Bearer $($auth.accessToken)" }
Invoke-RestMethod -Uri http://localhost:8080/api/v1/hospedes -Headers $headers
```

### Perfis de acesso

| Operação | ADMIN | RECEPCAO | FINANCEIRO | CONSULTA |
| --- | --- | --- | --- | --- |
| Consultar hóspedes, quartos e reservas | Sim | Sim | Sim | Sim |
| Criar/atualizar hóspedes e reservas; cancelar reservas | Sim | Sim | Não | Não |
| Criar/atualizar quartos | Sim | Não | Não | Não |
| Consultar/criar/atualizar pagamentos; estornar pagamentos | Sim | Não | Sim | Não |
| Excluir recursos | Sim | Não | Não | Não |

Login e `GET /openapi.yaml` são públicos. As demais operações documentadas exigem JWT. Credenciais ausentes ou inválidas retornam **401**; falta de permissão retorna **403**.

Os usuários são definidos por configuração e mantidos em memória. As senhas são transformadas em BCrypt na inicialização. O JWT utiliza HS256 e valida assinatura, emissor e prazo. Não há sessão de servidor ou refresh token; após expirar, é necessário fazer login novamente.

Alterações de senha ou perfil entram em vigor após reiniciar a aplicação. Tokens já emitidos permanecem válidos até o vencimento ou a troca da chave de assinatura. Utilize HTTPS fora do ambiente local.

## Endpoints

Todos os caminhos abaixo têm o prefixo `/api/v1`.

| Recurso | Listar | Consultar | Criar | Atualizar | Excluir |
| --- | --- | --- | --- | --- | --- |
| Hóspedes | `GET /hospedes` | `GET /hospedes/{id}` | `POST /hospedes` | `PUT /hospedes/{id}` | `DELETE /hospedes/{id}` |
| Quartos | `GET /quartos` | `GET /quartos/{id}` | `POST /quartos` | `PUT /quartos/{id}` | `DELETE /quartos/{id}` |
| Reservas | `GET /reservas` | `GET /reservas/{id}` | `POST /reservas` | `PUT /reservas/{id}` | `DELETE /reservas/{id}` |
| Pagamentos | `GET /pagamentos` | `GET /pagamentos/{id}` | `POST /pagamentos` | `PUT /pagamentos/{id}` | `DELETE /pagamentos/{id}` |

| Operação adicional | Endpoint | Sucesso |
| --- | --- | --- |
| Login | `POST /auth/login` | 200 |
| Cancelar reserva | `PATCH /reservas/{id}/cancelar` | 200 |
| Estornar pagamento | `PATCH /pagamentos/{id}/estornar` | 200 |

GETs e PUTs bem-sucedidos retornam **200**. POSTs de criação retornam **201** com `Location`. DELETEs retornam **204**, sem corpo. PUT recebe todos os campos do contrato de entrada, não uma atualização parcial.

| Contrato de entrada | Campos |
| --- | --- |
| `HospedeRequest` | `nome`, `documento`, `email`, `telefone` |
| `QuartoRequest` | `numero`, `tipo`, `capacidade`, `valorDiaria`, `ativo` |
| `ReservaRequest` — POST | `hospedeId`, `quartoId`, `entrada`, `saida`, `quantidadeHospedes` |
| `ReservaAtualizacaoRequest` — PUT | Mesmos campos da criação, com regras temporais de atualização |
| `PagamentoRequest` — POST/PUT | `reservaId`, `valor`, `forma` |

Respostas retornam `id` e os campos do recurso. Reservas também retornam `valorTotal`, `valorDiariaContratada` e `status`; pagamentos retornam `status`. Relações são representadas por IDs, sem exposição dos objetos de persistência.

## Filtros e paginação

Todas as listagens aceitam:

| Parâmetro | Regra | Padrão |
| --- | --- | --- |
| `pagina` | Número da página, a partir de zero | 0 |
| `tamanho` | De 1 a 100 registros | 20 |
| `ordenarPor` | Campo permitido para o recurso | id |
| `direcao` | asc ou desc | asc |

O ID é utilizado como desempate nas demais ordenações. Filtros são combinados com AND.

| Recurso | Filtros | Ordenações permitidas |
| --- | --- | --- |
| Hóspedes | `nome`, `documento`, `email` | id, nome, documento, email |
| Quartos | `numero`, `tipo`, `ativo`, `capacidadeMinima` | id, numero, tipo, capacidade, valorDiaria |
| Reservas | `hospedeId`, `quartoId`, `status`, `entrada`, `saida` | id, entrada, saida, valorTotal, status |
| Pagamentos | `reservaId`, `status`, `forma` | id, valor, status, forma |

`nome` busca por trecho; `documento` e `numero` usam igualdade exata. `nome`, `email` e `tipo` não diferenciam maiúsculas/minúsculas. `entrada` e `saida` filtram por data exata em `YYYY-MM-DD`. `capacidadeMinima` exige valor positivo.

Exemplo:

```http
GET /api/v1/reservas?hospedeId=1&status=CONFIRMADA&ordenarPor=entrada&direcao=desc&pagina=0&tamanho=20
```

As respostas paginadas contêm `content`, `totalElements`, `totalPages`, `number`, `size` e metadados de paginação.

## Regras de negócio

### Hóspedes e quartos

- Documento do hóspede e número do quarto são únicos. Documento é um identificador genérico, sem validação específica de CPF.
- Hóspedes e quartos sem reservas vinculadas podem ser excluídos fisicamente. Qualquer histórico de reserva, inclusive cancelado ou excluído, impede essa exclusão.
- Reajustar a diária de um quarto não altera o preço de reservas existentes.
- Inativar um quarto exige ausência de reservas confirmadas com saída posterior a hoje.
- Reduzir a capacidade exige que todas as reservas confirmadas vigentes ou futuras caibam na nova capacidade.

### Reservas

- A entrada na criação não pode ser passada. A saída deve ser posterior à entrada, com duração de **1 a 365 noites**.
- O quarto deve estar ativo, ter capacidade suficiente e estar disponível no período. Uma reserva pode iniciar na mesma data de saída de outra.
- A diária aceita valores de **0,01 a 9.999.999.999,99**, com até duas casas decimais. O total calculado também não pode exceder **9.999.999.999,99**.
- A reserva guarda a diária contratada. Atualizações no mesmo quarto preservam essa diária; mudanças na duração recalculam o total pelo novo número de noites. Troca de quarto em reserva futura utiliza a diária vigente do destino.
- PUT exige reserva CONFIRMADA, sem pagamentos confirmados e com saída original posterior a hoje.
- Uma reserva em andamento — entrada anterior a hoje e saída posterior a hoje — pode manter a entrada passada. Quarto e entrada originais devem ser preservados; a saída proposta precisa ser posterior a hoje.
- Reservas encerradas não podem ser atualizadas. Reservas futuras não podem ter sua entrada antecipada para uma data passada.
- Cancelamento exige ausência de pagamentos confirmados e é idempotente enquanto o registro não estiver excluído.
- DELETE cancela e oculta a reserva, preservando o registro no banco. GET de excluído retorna 404.

As regras temporais utilizam a data local do servidor. Configure o fuso horário da JVM de forma consistente com a operação do hotel.

### Pagamentos

- Pagamentos parciais ou integrais são aceitos para reservas CONFIRMADAS, até o saldo restante.
- Pagamentos CONFIRMADOS não podem ser alterados ou excluídos antes do estorno.
- PUT de pagamento ESTORNADO permite corrigir `valor` e `forma`, mantendo a reserva vinculada e o status. Um novo recebimento exige outro registro de pagamento.
- Estorno é idempotente enquanto o registro não estiver excluído.
- DELETE oculta apenas pagamentos ESTORNADOS. O registro permanece no banco; GET de excluído retorna 404.

| Enum | Valores |
| --- | --- |
| Status de reserva | CONFIRMADA, CANCELADA |
| Status de pagamento | CONFIRMADO, ESTORNADO |
| Forma de pagamento | PIX, DINHEIRO, CARTAO_CREDITO, CARTAO_DEBITO, TRANSFERENCIA |

## Respostas de erro

Erros utilizam `application/problem+json` e a estrutura `ProblemDetail`:

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "Quarto indisponível no período."
}
```

| Código | Significado |
| --- | --- |
| 400 | Dados, parâmetros ou regras de entrada inválidos |
| 401 | Credenciais ou token ausentes/inválidos |
| 403 | Perfil sem permissão para a operação |
| 404 | Recurso ausente ou logicamente excluído |
| 405 | Método HTTP não suportado; inclui `Allow` |
| 409 | Conflito de dados, concorrência ou estado do recurso |
| 415 | Tipo de conteúdo não suportado |
| 500 | Falha interna; detalhes técnicos registrados somente no servidor |

Erros de validação de campos incluem `erros`, com `campo` e `mensagem`. O retorno pode conter `instance` e outros metadados. Respostas 401 incluem `WWW-Authenticate: Bearer`.

## Documentação da API

O contrato OpenAPI está em [openapi.yaml](src/main/resources/static/openapi.yaml). Com a aplicação iniciada na porta padrão, consulte [http://localhost:8080/openapi.yaml](http://localhost:8080/openapi.yaml).

A especificação contém schemas, parâmetros, códigos HTTP e exemplos de entrada/saída para as 23 operações. Pode ser importada em ferramentas compatíveis com OpenAPI, como Postman e Swagger Editor. A aplicação não inclui uma interface Swagger UI embarcada.

Os exemplos por operação também estão ao final deste README. Eles são ilustrativos e não representam relatórios de execução de testes.

## Testes

### Serviços e contrato HTTP

No PowerShell:

```powershell
.\mvnw.cmd test
```

No Linux/macOS:

```bash
./mvnw test
```

A suíte utiliza H2 e cobre regras de negócio, limite monetário, CRUDs, validação HTTP, autenticação, autorização, filtros, exclusão lógica e preservação do preço contratado. As operações dos serviços utilizam transações próprias, sem uma transação externa envolvendo toda a classe de teste.

### Concorrência com PostgreSQL

Crie um **banco exclusivo e descartável de testes** e configure as variáveis no mesmo terminal:

```powershell
$env:TEST_DB_URL = "jdbc:postgresql://localhost:5432/hotel_test"
$env:TEST_DB_USER = "USUARIO_DE_TESTE"
$env:TEST_DB_PASSWORD = "SENHA_DE_TESTE"

.\mvnw.cmd verify
```

No Linux/macOS:

```bash
export TEST_DB_URL='jdbc:postgresql://localhost:5432/hotel_test'
export TEST_DB_USER='USUARIO_DE_TESTE'
export TEST_DB_PASSWORD='SENHA_DE_TESTE'
./mvnw verify
```

**Essa suíte utiliza `create-drop` e apaga registros. Não utilize o banco da aplicação ou qualquer banco com dados que devam ser preservados.** As variáveis `TEST_DB_*` são lidas do ambiente do processo; não basta deixá-las apenas no `.env` da aplicação.

`ConcorrenciaPostgresIT` testa duas reservas simultâneas para o mesmo período e dois pagamentos que, juntos, excederiam o saldo. Em cada caso, somente uma operação deve confirmar.

Sem uma URL PostgreSQL em `TEST_DB_URL`, a suíte é ignorada. Para comprovar a validação, confirme que seus dois testes foram executados, sem falhas e sem serem ignorados.

Relatórios são gerados em:

- `target/surefire-reports`: testes da fase `test`.
- `target/failsafe-reports`: testes de integração da fase `verify`.

A existência das classes de teste não representa aprovação da suíte; considere os resultados dos relatórios da execução.

## Atualização de bancos existentes

O script [adequacao-historico.sql](database/adequacao-historico.sql) oferece uma atualização idempotente para bancos de versões anteriores que já possuam as tabelas da aplicação. Inclui as colunas de exclusão lógica, versionamento e diária contratada.

Registros legados sem diária armazenada recuperam o preço a partir do total original dividido pelo número de noites, sem utilizar a tarifa atual do quarto. O script preenche a coluna para esses registros; atualizações realizadas pela aplicação também armazenam o valor recuperado.

Faça backup antes de alterar um banco com dados. O script não cria o banco nem substitui um processo de migrações versionadas.

## Contribuição

1. Crie uma branch para a alteração.
2. Preserve a separação entre controllers, serviços, repositórios e DTOs.
3. Atualize os testes pertinentes ao comportamento alterado.
4. Mantenha este README e o contrato OpenAPI consistentes com a implementação.
5. Execute `test` e, para alterações de persistência ou concorrência, `verify` com PostgreSQL de testes.
6. Abra um pull request descrevendo a mudança e os resultados da validação.

Não inclua `.env`, credenciais, chaves JWT ou dados pessoais reais em commits e exemplos.

## Exemplos por operação

Os exemplos representam cenários independentes e usam IDs ilustrativos. Ajuste as datas para a execução local. Todas as chamadas da API, exceto login, devem incluir `Authorization: Bearer <JWT>`; POST e PUT enviam `Content-Type: application/json`.

Os valores monetários são números JSON. Os exemplos de página podem ser acompanhados de metadados adicionais do Spring Data. Expanda a operação desejada para consultar os corpos e códigos HTTP.

<details>
<summary><strong>GET /api/v1/hospedes</strong></summary>

**Corpo enviado:** nenhum.

Exemplo de consulta: `/api/v1/hospedes?pagina=0&tamanho=20&ordenarPor=id&direcao=asc`.

**Resposta HTTP 200:**

```json
{
  "content": [
    {
      "nome": "Alan Maier",
      "documento": "DOC-123",
      "email": "alan@example.com",
      "telefone": "47999999999",
      "id": 1
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "sort": {
      "empty": false,
      "sorted": true,
      "unsorted": false
    },
    "offset": 0,
    "paged": true,
    "unpaged": false
  },
  "totalElements": 1,
  "totalPages": 1,
  "last": true,
  "size": 20,
  "number": 0,
  "sort": {
    "empty": false,
    "sorted": true,
    "unsorted": false
  },
  "numberOfElements": 1,
  "first": true,
  "empty": false
}
```

</details>

<details>
<summary><strong>POST /api/v1/hospedes</strong></summary>

**JSON enviado:**

```json
{
  "nome": "Alan Maier",
  "documento": "DOC-123",
  "email": "alan@example.com",
  "telefone": "47999999999"
}
```

**Resposta HTTP 201:**

```json
{
  "nome": "Alan Maier",
  "documento": "DOC-123",
  "email": "alan@example.com",
  "telefone": "47999999999",
  "id": 1
}
```

Cabeçalho: `Location: /api/v1/hospedes/1`.

</details>

<details>
<summary><strong>GET /api/v1/hospedes/{id}</strong></summary>

**Corpo enviado:** nenhum.

**Resposta HTTP 200:**

```json
{
  "nome": "Alan Maier",
  "documento": "DOC-123",
  "email": "alan@example.com",
  "telefone": "47999999999",
  "id": 1
}
```

</details>

<details>
<summary><strong>PUT /api/v1/hospedes/{id}</strong></summary>

**JSON enviado:**

```json
{
  "nome": "Alan Maier",
  "documento": "DOC-123",
  "email": "alan@example.com",
  "telefone": "47988888888"
}
```

**Resposta HTTP 200:**

```json
{
  "nome": "Alan Maier",
  "documento": "DOC-123",
  "email": "alan@example.com",
  "telefone": "47988888888",
  "id": 1
}
```

</details>

<details>
<summary><strong>DELETE /api/v1/hospedes/{id}</strong></summary>

**Corpo enviado:** nenhum.

**Resposta HTTP 204:**

Sem corpo de resposta.

</details>

<details>
<summary><strong>GET /api/v1/quartos</strong></summary>

**Corpo enviado:** nenhum.

Exemplo de consulta: `/api/v1/quartos?pagina=0&tamanho=20&ordenarPor=id&direcao=asc`.

**Resposta HTTP 200:**

```json
{
  "content": [
    {
      "numero": "101",
      "tipo": "CASAL",
      "capacidade": 2,
      "valorDiaria": 150.0,
      "ativo": true,
      "id": 1
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "sort": {
      "empty": false,
      "sorted": true,
      "unsorted": false
    },
    "offset": 0,
    "paged": true,
    "unpaged": false
  },
  "totalElements": 1,
  "totalPages": 1,
  "last": true,
  "size": 20,
  "number": 0,
  "sort": {
    "empty": false,
    "sorted": true,
    "unsorted": false
  },
  "numberOfElements": 1,
  "first": true,
  "empty": false
}
```

</details>

<details>
<summary><strong>POST /api/v1/quartos</strong></summary>

**JSON enviado:**

```json
{
  "numero": "101",
  "tipo": "CASAL",
  "capacidade": 2,
  "valorDiaria": 150.0,
  "ativo": true
}
```

**Resposta HTTP 201:**

```json
{
  "numero": "101",
  "tipo": "CASAL",
  "capacidade": 2,
  "valorDiaria": 150.0,
  "ativo": true,
  "id": 1
}
```

Cabeçalho: `Location: /api/v1/quartos/1`.

</details>

<details>
<summary><strong>GET /api/v1/quartos/{id}</strong></summary>

**Corpo enviado:** nenhum.

**Resposta HTTP 200:**

```json
{
  "numero": "101",
  "tipo": "CASAL",
  "capacidade": 2,
  "valorDiaria": 150.0,
  "ativo": true,
  "id": 1
}
```

</details>

<details>
<summary><strong>PUT /api/v1/quartos/{id}</strong></summary>

**JSON enviado:**

```json
{
  "numero": "101",
  "tipo": "CASAL",
  "capacidade": 2,
  "valorDiaria": 200.0,
  "ativo": true
}
```

**Resposta HTTP 200:**

```json
{
  "numero": "101",
  "tipo": "CASAL",
  "capacidade": 2,
  "valorDiaria": 200.0,
  "ativo": true,
  "id": 1
}
```

</details>

<details>
<summary><strong>DELETE /api/v1/quartos/{id}</strong></summary>

**Corpo enviado:** nenhum.

**Resposta HTTP 204:**

Sem corpo de resposta.

</details>

<details>
<summary><strong>GET /api/v1/reservas</strong></summary>

**Corpo enviado:** nenhum.

Exemplo de consulta: `/api/v1/reservas?pagina=0&tamanho=20&ordenarPor=id&direcao=asc`.

**Resposta HTTP 200:**

```json
{
  "content": [
    {
      "hospedeId": 1,
      "quartoId": 1,
      "entrada": "2030-01-10",
      "saida": "2030-01-12",
      "quantidadeHospedes": 1,
      "id": 1,
      "valorTotal": 300.0,
      "valorDiariaContratada": 150.0,
      "status": "CONFIRMADA"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "sort": {
      "empty": false,
      "sorted": true,
      "unsorted": false
    },
    "offset": 0,
    "paged": true,
    "unpaged": false
  },
  "totalElements": 1,
  "totalPages": 1,
  "last": true,
  "size": 20,
  "number": 0,
  "sort": {
    "empty": false,
    "sorted": true,
    "unsorted": false
  },
  "numberOfElements": 1,
  "first": true,
  "empty": false
}
```

</details>

<details>
<summary><strong>POST /api/v1/reservas</strong></summary>

**JSON enviado:**

```json
{
  "hospedeId": 1,
  "quartoId": 1,
  "entrada": "2030-01-10",
  "saida": "2030-01-12",
  "quantidadeHospedes": 1
}
```

**Resposta HTTP 201:**

```json
{
  "hospedeId": 1,
  "quartoId": 1,
  "entrada": "2030-01-10",
  "saida": "2030-01-12",
  "quantidadeHospedes": 1,
  "id": 1,
  "valorTotal": 300.0,
  "valorDiariaContratada": 150.0,
  "status": "CONFIRMADA"
}
```

Cabeçalho: `Location: /api/v1/reservas/1`.

</details>

<details>
<summary><strong>GET /api/v1/reservas/{id}</strong></summary>

**Corpo enviado:** nenhum.

**Resposta HTTP 200:**

```json
{
  "hospedeId": 1,
  "quartoId": 1,
  "entrada": "2030-01-10",
  "saida": "2030-01-12",
  "quantidadeHospedes": 1,
  "id": 1,
  "valorTotal": 300.0,
  "valorDiariaContratada": 150.0,
  "status": "CONFIRMADA"
}
```

</details>

<details>
<summary><strong>PUT /api/v1/reservas/{id}</strong></summary>

**JSON enviado:**

```json
{
  "hospedeId": 1,
  "quartoId": 1,
  "entrada": "2030-01-10",
  "saida": "2030-01-12",
  "quantidadeHospedes": 2
}
```

A reserva precisa estar CONFIRMADA e sem pagamentos confirmados. No exemplo, a atualização preserva a diária contratada de 150 e o total de 300.

**Resposta HTTP 200:**

```json
{
  "hospedeId": 1,
  "quartoId": 1,
  "entrada": "2030-01-10",
  "saida": "2030-01-12",
  "quantidadeHospedes": 2,
  "id": 1,
  "valorTotal": 300.0,
  "valorDiariaContratada": 150.0,
  "status": "CONFIRMADA"
}
```

</details>

<details>
<summary><strong>DELETE /api/v1/reservas/{id}</strong></summary>

**Corpo enviado:** nenhum.

**Resposta HTTP 204:**

Sem corpo de resposta.

</details>

<details>
<summary><strong>PATCH /api/v1/reservas/{id}/cancelar</strong></summary>

**Corpo enviado:** nenhum.

**Resposta HTTP 200:**

```json
{
  "hospedeId": 1,
  "quartoId": 1,
  "entrada": "2030-01-10",
  "saida": "2030-01-12",
  "quantidadeHospedes": 1,
  "id": 1,
  "valorTotal": 300.0,
  "valorDiariaContratada": 150.0,
  "status": "CANCELADA"
}
```

</details>

<details>
<summary><strong>GET /api/v1/pagamentos</strong></summary>

**Corpo enviado:** nenhum.

Exemplo de consulta: `/api/v1/pagamentos?pagina=0&tamanho=20&ordenarPor=id&direcao=asc`.

**Resposta HTTP 200:**

```json
{
  "content": [
    {
      "reservaId": 1,
      "valor": 300.0,
      "forma": "PIX",
      "id": 1,
      "status": "CONFIRMADO"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "sort": {
      "empty": false,
      "sorted": true,
      "unsorted": false
    },
    "offset": 0,
    "paged": true,
    "unpaged": false
  },
  "totalElements": 1,
  "totalPages": 1,
  "last": true,
  "size": 20,
  "number": 0,
  "sort": {
    "empty": false,
    "sorted": true,
    "unsorted": false
  },
  "numberOfElements": 1,
  "first": true,
  "empty": false
}
```

</details>

<details>
<summary><strong>POST /api/v1/pagamentos</strong></summary>

**JSON enviado:**

```json
{
  "reservaId": 1,
  "valor": 300.0,
  "forma": "PIX"
}
```

**Resposta HTTP 201:**

```json
{
  "reservaId": 1,
  "valor": 300.0,
  "forma": "PIX",
  "id": 1,
  "status": "CONFIRMADO"
}
```

Cabeçalho: `Location: /api/v1/pagamentos/1`.

</details>

<details>
<summary><strong>GET /api/v1/pagamentos/{id}</strong></summary>

**Corpo enviado:** nenhum.

**Resposta HTTP 200:**

```json
{
  "reservaId": 1,
  "valor": 300.0,
  "forma": "PIX",
  "id": 1,
  "status": "CONFIRMADO"
}
```

</details>

<details>
<summary><strong>PUT /api/v1/pagamentos/{id}</strong></summary>

**JSON enviado:**

```json
{
  "reservaId": 1,
  "valor": 250.0,
  "forma": "PIX"
}
```

O pagamento precisa estar ESTORNADO; o PUT mantém esse status e o vínculo com a reserva.

**Resposta HTTP 200:**

```json
{
  "reservaId": 1,
  "valor": 250.0,
  "forma": "PIX",
  "id": 1,
  "status": "ESTORNADO"
}
```

</details>

<details>
<summary><strong>DELETE /api/v1/pagamentos/{id}</strong></summary>

**Corpo enviado:** nenhum.

**Resposta HTTP 204:**

Sem corpo de resposta.

</details>

<details>
<summary><strong>PATCH /api/v1/pagamentos/{id}/estornar</strong></summary>

**Corpo enviado:** nenhum.

**Resposta HTTP 200:**

```json
{
  "reservaId": 1,
  "valor": 300.0,
  "forma": "PIX",
  "id": 1,
  "status": "ESTORNADO"
}
```

</details>

<details>
<summary><strong>POST /api/v1/auth/login</strong></summary>

**JSON enviado:**

```json
{
  "username": "admin",
  "password": "senha-local-exemplo-123"
}
```

**Resposta HTTP 200:**

```json
{
  "accessToken": "<JWT_EMITIDO_PELO_LOGIN>",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

`accessToken` é um marcador ilustrativo; o login real emite o JWT.

</details>