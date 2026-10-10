# API Hotel — Swagger / OpenAPI

## Instalação no projeto existente

Este pacote contém apenas os arquivos da integração. Com a aplicação parada, copie a pasta `src` deste pacote para a raiz do projeto `apiHotel`, mesclando as pastas e substituindo os três arquivos correspondentes. Preserve os demais arquivos do projeto.

Arquivos:
- `src/main/resources/static/openapi.yaml`: contrato OpenAPI 3.0.3, com os 23 endpoints, DTOs, exemplos, filtros, paginação, respostas de erro e Bearer JWT.
- `src/main/resources/static/swagger-ui.html`: interface interativa.
- `src/main/java/com/esos/apiHotel/config/SecurityConfig.java`: libera GET da interface e do contrato, mantendo as permissões dos endpoints.

A configuração de segurança foi baseada na versão `apiHotel(1).zip`. Se você alterou essa classe depois, preserve suas alterações e acrescente apenas `/swagger-ui.html` ao matcher GET que já libera `/openapi.yaml`.

Não é necessário alterar o `pom.xml`. A interface usa o contrato estático já existente. Quando um endpoint ou DTO mudar, atualize `openapi.yaml` junto com o código.

## Iniciar e acessar

Com as variáveis do servidor e o banco configurados conforme o README, execute no PowerShell, na raiz do projeto:

```powershell
.\mvnw.cmd spring-boot:run
```

Abra no navegador:

- Interface: http://localhost:8080/swagger-ui.html
- Contrato: http://localhost:8080/openapi.yaml

A interface exige internet para carregar CSS e JavaScript do Swagger UI pelo domínio `unpkg.com`, com versão fixada em 5.11.0. A especificação e as chamadas da API são fornecidas pelo seu servidor. O servidor usado nas chamadas acompanha o host da documentação; não fica preso a localhost. Não abra o HTML diretamente como arquivo.

## Login e uso

1. Expanda `auth` e POST `/api/v1/auth/login`.
2. Clique em **Try it out** e substitua username/password pelas credenciais configuradas no servidor.
3. Clique em **Execute**. Na resposta 200, copie somente o conteúdo de `accessToken`.
4. Clique em **Authorize**, cole o token sem aspas e sem o prefixo `Bearer`, confirme e feche a janela.
5. Expanda a operação desejada, clique em **Try it out**, preencha parâmetros ou JSON e clique em **Execute**.

O login não aplica o token automaticamente. Ao expirar ou recarregar a página, faça login e autorize novamente. Os IDs também não são capturados automaticamente: copie o `id` retornado ao criar um registro e utilize-o nas operações seguintes.

Para testar um fluxo, crie hóspede e quarto, depois uma reserva com esses IDs e, por fim, um pagamento vinculado à reserva. Use datas atuais/futuras de acordo com as regras, quarto ativo, capacidade suficiente e valor de pagamento dentro do saldo. Os IDs e datas dos exemplos são ilustrativos.

## Permissões e regras

Cada operação informa os perfis permitidos. ADMIN executa todas; RECEPCAO opera hóspedes/reservas; FINANCEIRO opera pagamentos; CONSULTA consulta hóspedes/quartos/reservas. Veja a descrição individual para as permissões de leitura. Excluir exige ADMIN.

Estorne pagamentos confirmados antes de atualizar/cancelar/excluir a reserva. Pagamento confirmado também exige estorno antes de PUT ou DELETE; após PUT, permanece ESTORNADO e vinculado à mesma reserva. Reserva em andamento não permite trocar quarto ou entrada; reserva encerrada não permite atualização. Hóspedes e quartos com histórico não podem ser excluídos. DELETE de reservas e pagamentos é lógico.

**Execute envia uma requisição real e pode alterar o banco de dados.**

## Diagnóstico

- 401: faça login e autorize com um token válido.
- 403: o perfil não tem permissão para a operação.
- 400: verifique JSON, datas, enums, parâmetros e campos obrigatórios.
- 404: ID inexistente ou recurso logicamente excluído.
- 409: consulte `detail`, pois uma regra de negócio, duplicidade ou concorrência impediu a operação.
- Tela sem carregar: verifique acesso à internet e ao domínio unpkg.com.

## Validação e manutenção

O pacote foi verificado quanto a sintaxe YAML, referências de schemas, IDs de operações únicos, presença dos parâmetros obrigatórios de caminho e sintaxe JavaScript. Não houve execução de chamadas HTTP contra sua instância nem execução da suíte Java neste ambiente.

Implementação baseada na instalação oficial do Swagger UI: https://swagger.io/docs/open-source-tools/swagger-ui/usage/installation/
