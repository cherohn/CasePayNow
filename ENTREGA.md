# Avaliação técnica — CasePayNow

Nome: Matheus Souza Garcez.

Início registrado: 30/09/2026, 14h09, America/Sao_Paulo.
Limite de entrega: 30/09/2026, 15h24, incluindo preparação do GitHub.
Tempo utilizado até a revisão final: aproximadamente 63 minutos (14h09–15h12), incluindo desenvolvimento, testes e preparação dos commits. Limite total: 75 minutos; o envio final ao GitHub consta no histórico do repositório.

Material fictício, desenvolvido e validado localmente com assistência de IA.

As fontes de cada resposta sustentam os conceitos técnicos; as decisões propostas para o CRM são escolhas de projeto aplicadas ao enunciado. Consulta documental não representa execução de testes ou validação em produção.

## A. Full-stack foundations

### A1. Um formulário que preserva o trabalho

- Ao enviar, mantenho os valores no formulário, valido campos e formatos no navegador para dar retorno imediato e desabilito novos envios enquanto a requisição está em andamento. Envio os dados por HTTPS, incluindo a escolha explícita de consentimento; a ausência de consentimento não deve ser convertida em autorização pelo servidor.
- A API é a autoridade de validação: verifica tipos, limites de tamanho, nome, presença de pelo menos telefone ou e-mail válido e as regras de consentimento. A validação do navegador melhora a experiência, mas pode ser contornada. O servidor normaliza os contatos conforme regras definidas e grava o lead e o registro de consentimento em uma transação, usando consultas parametrizadas.
- Para erros de validação, a API retorna 422 com erros por campo; o navegador preserva todos os valores, destaca os campos afetados e permite corrigir e reenviar. Não exibo mensagens internas do banco ou detalhes de exceções.
- Em timeout ou falha de rede, preservo o formulário e aviso que não foi possível confirmar o resultado: a gravação pode ter ocorrido mesmo sem resposta. Reabilito a tentativa, sem afirmar que o lead não foi salvo.
- Para evitar duplicidade nessa tentativa, gero uma chave de idempotência por envio lógico e reutilizo a mesma chave ao repetir o mesmo conteúdo. O servidor associa chave, escopo e hash do conteúdo ao resultado, com unicidade e gravação atômica; uma repetição retorna o resultado anterior, e a mesma chave com conteúdo diferente é rejeitada. Isso resolve repetições da mesma operação; possíveis duplicidades de contatos em envios distintos exigem uma regra de negócio própria.
- Somente depois da confirmação de persistência, por exemplo 201 com o identificador, mostro sucesso e limpo o formulário. Um novo envio lógico recebe outra chave de idempotência.

**Fontes e relação com a resposta:**

- [OWASP — Input Validation](https://cheatsheetseries.owasp.org/cheatsheets/Input_Validation_Cheat_Sheet.html): validação obrigatória no servidor e possibilidade de contornar a validação no cliente.
- [Stripe — Idempotent requests](https://docs.stripe.com/api/idempotent_requests): exemplo documentado de reutilização de chave em retries, recuperação do resultado e rejeição de parâmetros diferentes. Uso como referência do padrão; não há integração com Stripe. Escopo, hash e transação são decisões propostas para implementá-lo neste cenário.
- [MDN — 422 Unprocessable Content](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Status/422) e [201 Created](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Status/201): significado dos códigos de validação semântica e criação bem-sucedida. Preservar campos e apresentar mensagens é a estratégia de interface proposta.
- [OWASP — SQL Injection Prevention](https://cheatsheetseries.owasp.org/cheatsheets/SQL_Injection_Prevention_Cheat_Sheet.html): uso de consultas parametrizadas para separar instruções e dados.

### A2. Cookies e segurança no navegador

Assumo frontend e API na mesma origem, exclusivamente por HTTPS. `Secure` restringe o envio do cookie a conexões seguras; `HttpOnly` impede que JavaScript leia o cookie; `SameSite` limita quando o navegador envia cookies em requisições entre sites, conforme o modo escolhido. Site e origem são conceitos diferentes: subdomínios podem ser do mesmo site e de origens diferentes.

`HttpOnly` não elimina XSS: um script injetado ainda pode executar ações autenticadas e ler dados acessíveis à página. `SameSite` é uma defesa adicional e não substitui todas as proteções contra CSRF; para operações de escrita com sessão em cookie, uso método apropriado, token CSRF e validação de origem. CORS controla quais origens podem ler respostas no navegador; não autoriza acesso a leads e não substitui verificações de tenant, papel e atribuição no servidor.

**Fontes e relação com a resposta:**

- [MDN — Set-Cookie](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/Set-Cookie): comportamento de `Secure`, `HttpOnly` e `SameSite`, incluindo envio de cookies em requisições iniciadas por JavaScript mesmo com `HttpOnly`.
- [OWASP — CSRF Prevention](https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html): limites de `SameSite`, tokens CSRF e verificação de origem.
- [MDN — CORS](https://developer.mozilla.org/en-US/docs/Web/HTTP/Guides/CORS) e [OWASP — REST Security](https://cheatsheetseries.owasp.org/cheatsheets/REST_Security_Cheat_Sheet.html): controle de acesso do navegador entre origens e necessidade independente de autorização por endpoint.

### A3. Sessões e logout

Após login ou aumento de privilégios, gero um novo identificador de sessão imprevisível e invalido o anterior, prevenindo fixação de sessão e reutilização do identificador antigo. O logout invalida a sessão no servidor e remove o cookie com os mesmos atributos de domínio e caminho usados na criação. O servidor aplica expiração por inatividade e duração máxima, rejeitando identificadores expirados mesmo que o navegador ainda mantenha o cookie.

Uma sessão opaca, mantida no servidor, facilita revogação imediata e atualização de permissões, ao custo de consultar e operar um armazenamento de sessões. Um JWT de longa duração em armazenamento acessível ao JavaScript pode ser roubado por XSS e costuma continuar válido até expirar; revogação antecipada exige estado adicional ou outro mecanismo. Para este CRM, prefiro uma sessão opaca em cookie `Secure`, `HttpOnly` e `SameSite`, acompanhada de proteção contra CSRF.

**Fontes e relação com a resposta:**

- [OWASP — Session Management](https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html): renovação do identificador em mudanças de privilégio, invalidação no logout e limites de inatividade e duração absoluta.
- [OWASP — REST Security, JWT](https://cheatsheetseries.owasp.org/cheatsheets/REST_Security_Cheat_Sheet.html#jwt): divergência entre a validade do token e o estado atual da sessão, além de revogação por denylist. A preferência por sessão opaca é a decisão proposta para este CRM.

### A4. SQL e crescimento da caixa de entrada

Uso parâmetros vinculados pelo driver; `:tenant_id` recebe exclusivamente o tenant do contexto autenticado no servidor. Assumo `received_at` não nulo para as mensagens exibidas.

Os nomes `:tenant_id`, `:last_received_at` e `:last_id` representam parâmetros nomeados de uma camada de acesso a dados. Em Java com JDBC, usaria `?` e vincularia cada valor com `PreparedStatement`, sem concatenar entradas ao SQL.

```sql
SELECT id, subject, received_at
FROM messages
WHERE tenant_id = :tenant_id
ORDER BY received_at DESC, id DESC
LIMIT 25;
```

O identificador desempata mensagens com o mesmo horário. Um índice útil é:

```sql
CREATE INDEX idx_messages_tenant_received_id
ON messages (tenant_id, received_at DESC, id DESC);
```

Para páginas seguintes, uso paginação por cursor com o horário e o ID da última mensagem recebida, evitando o custo crescente de grandes `OFFSET`:

```sql
SELECT id, subject, received_at
FROM messages
WHERE tenant_id = :tenant_id
  AND (
    received_at < :last_received_at
    OR (received_at = :last_received_at AND id < :last_id)
  )
ORDER BY received_at DESC, id DESC
LIMIT 25;
```

Valido os tipos do cursor e mantenho o tenant vindo da autenticação em todas as páginas. O cursor nunca concede acesso. A ordenação pressupõe que o horário de recebimento seja estável; ela não promete uma fotografia imutável da caixa durante alterações concorrentes.

**Fontes e relação com a resposta:**

- [OWASP — SQL Injection Prevention, prepared statements](https://cheatsheetseries.owasp.org/cheatsheets/SQL_Injection_Prevention_Cheat_Sheet.html#defense-option-1-use-of-prepared-statements-with-parameterized-queries): vinculação de parâmetros, inclusive exemplo com Java e `PreparedStatement`.
- [PostgreSQL — LIMIT and OFFSET](https://www.postgresql.org/docs/18/queries-limit.html): necessidade de ordenação única para resultados previsíveis e custo das linhas descartadas por `OFFSET`.
- [PostgreSQL — Multicolumn Indexes](https://www.postgresql.org/docs/18/indexes-multicolumn.html) e [Indexes and ORDER BY](https://www.postgresql.org/docs/18/indexes-ordering.html): índice B-tree composto, filtro pela coluna inicial e atendimento à ordenação com limite. A consulta por cursor acima aplica esses princípios; não foi executada nem teve seu plano de execução medido nesta etapa.

## B. Revisão do código inseguro

Revisão estática do trecho fornecido, sem executá-lo contra qualquer site. Os testes negativos abaixo são propostas de verificação da correção em ambiente local com dados fictícios; ainda não foram executados. Os códigos HTTP indicados descrevem o comportamento esperado da versão corrigida.

### Cinco achados

| Localização | Risco e impacto | Correção concreta | Teste negativo proposto |
| --- | --- | --- | --- |
| **B1 — Linhas 05–12: SQL injection** | `id` e `status` entram por concatenação nas consultas. Uma entrada pode alterar a condição do SQL e fazer a leitura ou a atualização atingir registros além do pretendido, dentro dos privilégios da conexão com o banco. | Parametrizar tanto o `SELECT` quanto o `UPDATE`, vinculando todos os valores; em Java, usar `PreparedStatement`. Validar o ID como inteiro positivo e o status por lista permitida, sem depender dessa validação como substituta da parametrização. | Com autenticação válida, enviar o ID textual `101 OR 1=1`. Esperar 422, nenhuma informação de lead e nenhuma alteração em qualquer registro. Na revisão da correção, conferir também que as duas consultas vinculam parâmetros; a rejeição dessa entrada, sozinha, não comprova parametrização. |
| **B2 — Linhas 02–12: falta de autorização por registro** | A sessão prova apenas que há um usuário autenticado. As consultas não verificam tenant, papel ou atribuição: um usuário pode tentar ler ou alterar outro lead mudando o ID. | Aplicar autorização no servidor antes de ler dados ou atualizar: negar papéis desconhecidos; limitar a leitura à política de tenant e papel; na escrita, permitir somente o agente atribuído ou um manager do mesmo tenant. Incluir essas restrições na operação de banco e usar apenas identidade confiável da sessão. Retornar 404 sem dados para lead inexistente ou inacessível. | Em casos independentes, manager de T-A tenta ler e atualizar o lead 201 de T-B: esperar 404, sem dados e sem mutação. Agente U-A tenta atualizar 102, atribuído a U-B: esperar 404 e estado preservado. Papel desconhecido deve receber 403 sem acesso. |
| **B3 — Linhas 06–08: escrita via GET e ausência de defesa CSRF no trecho** | Uma URL de consulta altera o estado. Se o navegador enviar a sessão em uma requisição induzida por outro site, a ação pode ocorrer sem intenção do usuário; uma navegação de nível superior pode enviar cookies `SameSite=Lax`. O trecho não mostra verificação de token ou origem. | Manter GET sem efeitos de escrita e mover a atualização para um endpoint POST/PATCH. Exigir token CSRF válido associado à sessão e verificar a origem; configurar `SameSite` como defesa adicional. Trocar apenas o método não resolve CSRF. | Tentar a URL antiga com `status`: o GET não deve alterar nada. Separadamente, enviar uma escrita com sessão válida e token CSRF ausente ou inválido: esperar 403, sem mutação. Executar apenas em um wrapper local de teste, sem destinatários ou serviços reais. |
| **B4 — Linhas 06–08: transição de estado sem validação** | Qualquer texto é gravado como status e não há verificação do estado atual. Isso permite estados inexistentes, saltos no fluxo ou alteração de estados terminais, comprometendo o processo de financiamento. | Aceitar somente os destinos exatos `sent_to_funder` e `cannot_fund`, exclusivamente quando o estado atual for `contact_lawyer`. Validar e atualizar atomicamente; no banco, condicionar a escrita ao estado esperado para evitar uma mudança entre a checagem e o UPDATE. | Com usuário autorizado e fixtures independentes, tentar destino `approved`: esperar 422. Depois, partindo de `sent_to_funder`, tentar `cannot_fund`: esperar 422. Em ambos, conferir todos os registros antes e depois para comprovar ausência de mutação. |
| **B5 — Linhas 18–19: XSS armazenado, renderizado no DOM** | Notas não confiáveis, inclusive originadas de e-mail, são interpretadas como HTML por `innerHTML`. Uma nota maliciosa pode executar JavaScript na origem do CRM e agir com os privilégios do usuário que a visualizar. A serialização por `json_encode` não torna seguro esse destino no DOM. | Como não é necessário exibir HTML, substituir por `document.querySelector("#notes").textContent = lead.notes;`, validando que notas sejam texto. O conteúdo deve ser apresentado literalmente, sem interpretá-lo como marcação. | Em uma página local, usar a nota `<svg onload="window.__xssExecuted=true"></svg>`, com o marcador inicialmente falso. Esperar o texto literal em `#notes`, nenhum elemento `svg` criado e marcador ainda falso. O payload apenas altera um marcador local, sem comunicação externa. |

SQL injection em leitura e escrita compõe um único achado (B1). Tenant, papel e atribuição compõem a mesma falha de autorização (B2). B4 permanece um problema mesmo para um usuário autorizado enviando valores sem sintaxe SQL: trata-se da validade da transição de negócio.

### Prioridade de correção

1. **B1 — SQL injection:** pode mudar o significado das consultas, ampliar o conjunto de registros afetados e comprometer confidencialidade e integridade. Corrigiria as duas consultas antes de disponibilizar o endpoint.
2. **B2 — Autorização por registro:** mesmo com SQL parametrizado, trocar um ID válido ainda permitiria acesso indevido se a política não fosse aplicada. O isolamento entre tenants e a restrição de escrita por atribuição são requisitos centrais do CRM.

Essa ordem considera o alcance direto das operações no banco e a separação entre clientes. B3–B5 também precisam de correção antes de uso real; priorizar os dois primeiros não torna os demais aceitáveis.

### Fontes e relação com a resposta

- **B1:** [OWASP — SQL Injection Prevention](https://cheatsheetseries.owasp.org/cheatsheets/SQL_Injection_Prevention_Cheat_Sheet.html): separação de SQL e valores por consultas parametrizadas, com exemplo de `PreparedStatement` em Java.
- **B2:** [OWASP — Authorization](https://cheatsheetseries.owasp.org/cheatsheets/Authorization_Cheat_Sheet.html): negar por padrão, verificar permissões em cada requisição e evitar acesso indevido por manipulação de identificadores. As regras específicas de tenant, manager e agente vêm do enunciado.
- **B3:** [OWASP — CSRF Prevention](https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html): evitar GET para alterações, validar tokens e considerar as limitações de `SameSite`. Não presumo que uma defesa externa exista ou esteja ausente; o achado se limita ao trecho apresentado.
- **B4:** [OWASP — REST Security, workflow state validation](https://cheatsheetseries.owasp.org/cheatsheets/REST_Security_Cheat_Sheet.html#validate-workflow-state-on-the-server-side): validar estados e impedir execução de etapas fora da ordem no servidor. Os estados permitidos são definidos pelo exercício.
- **B5:** [OWASP — DOM based XSS Prevention](https://cheatsheetseries.owasp.org/cheatsheets/DOM_based_XSS_Prevention_Cheat_Sheet.html): uso de `textContent` para inserir texto não confiável, evitando destinos que interpretam HTML, como `innerHTML`.

## C. Implementação, testes e comportamento do navegador

### Implementação

A função está em [codigo/java/src/LeadService.java](codigo/java/src/LeadService.java). Ela recebe a autenticação do servidor, o corpo já convertido de JSON para objetos Java e um mapa de leads em memória.

A função principal usa somente Java, sem framework ou banco. A função verifica autenticação, papel, campos, acesso ao lead, versão e estado atual, nessa ordem. Campos extras são rejeitados; o corpo não pode mudar tenant, papel ou responsável. IDs e versões precisam ser inteiros positivos, sem converter strings, booleanos ou decimais.

No sucesso, altera apenas status e versão e retorna `{id, status, version}`. Nos erros, não altera nenhum registro nem retorna dados do lead. Uso `BigInteger` para os inteiros e registros imutáveis para preservar os demais campos.

### Testes executáveis

Arquivo: [codigo/java/tests/LeadServiceTest.java](codigo/java/tests/LeadServiceTest.java). Para executar na raiz da pasta:

```sh
sh codigo/java/run-tests.sh
```

**Resultado verificado em OpenJDK 25.0.4: 63 testes passaram.** Cada caso independente recria as fixtures e confere a resposta completa e o estado de todos os registros. Os cenários de replay mantêm a primeira alteração para verificar que a segunda tentativa não escreve novamente.

| Caso exigido | Resultado conferido |
| --- | --- |
| Sem autenticação | 401, sem alteração |
| U-A atualiza seu lead 101 | 200, status alterado e versão 2 |
| U-A tenta atualizar 102 de U-B | 404, sem dados e sem alteração |
| Manager T-A tenta atualizar 201 de T-B | 404, sem dados e sem alteração |
| Manager T-A atualiza 102 | 200, preservando os outros campos e registros |
| Status desconhecido | 422, sem alteração |
| Repetição da versão original | 409, sem segunda escrita |
| Papel desconhecido ou elevação pelo corpo | 403 para papel desconhecido; 422 para campos extras |

Os outros casos verificam tipos inválidos, campos ausentes, estados terminais, ordem das validações e inteiros grandes. Os testes falham com código de saída diferente de zero e não precisam da opção `-ea`.

### Comportamento do frontend

Implementei também uma interface local em React, JavaScript, HTML e CSS, disponível em [codigo/frontend](codigo/frontend/src/main.jsx). O [DemoServer.java](codigo/java/src/DemoServer.java) recebe as requisições e chama a mesma função Java testada; a interface não reimplementa as regras no navegador. Estes são os comportamentos do formulário:

- Antes do envio, verifico `submitting`. Se estiver ativo, ignoro o segundo clique; caso contrário, ativo a flag e desabilito o botão imediatamente.
- Guardo os valores preenchidos e envio apenas `{id, status, version}`, com a versão que o usuário carregou. Uso o método de escrita e a proteção CSRF do wrapper.
- Enquanto aguardo, mostro “Salvando…”. O status confirmado na tela e a versão local continuam iguais.
- Em timeout ou falha de rede, mantenho o formulário e aviso que não foi possível confirmar a atualização; em erro do servidor, informo a indisponibilidade. Uma nova tentativa usa a mesma versão, pois a primeira pode ter sido gravada.
- Se receber 409, preservo a edição e consulto o estado atual para mostrar o conflito. Não troco a versão nem reenvio automaticamente: o usuário precisa revisar. Se o lead já estiver em estado terminal, não permito outra transição; se a consulta falhar, mantenho a edição e o aviso.
- Em 422, mostro o erro para correção. Em 401, aviso que não há sessão e permito escolher um perfil fictício sem apagar o rascunho; em 403/404, informo que não foi possível acessar o registro.
- Só após um 200 válido atualizo a tela com o status e a versão retornados. No `finally`, retiro `submitting` e restauro os controles, exceto os de transição quando o lead estiver em estado terminal.

### Integração local e validação no navegador

Para abrir a interface conectada ao Java, na raiz do projeto:

```sh
sh codigo/run-demo.sh
```

Depois, acesse `http://127.0.0.1:8080`. O script compila React e Java e inicia o servidor local; os pré-requisitos e os downloads da primeira execução estão no README. A demonstração usa perfis fictícios predefinidos, dados em memória por sessão e controles para restaurar os registros, provocar conflito e simular erro 503.

A API usa Gson para ler JSON, preservando a distinção entre inteiro, decimal, string e booleano antes de chamar `updateLead`. O servidor atende somente em loopback, verifica o método de escrita, o token CSRF e a origem das escritas, e filtra a leitura dos leads pelo perfil. É uma demonstração HTTP local: não implementei login real, TLS ou gestão completa de sessões de produção.

**Resultado da validação adicional: 9 testes de navegador passaram**, usando Chromium e a API Java real. Os cenários cobrem sucesso, acesso negado por responsável/tenant, permissão do manager, conflito sem reenvio automático, erro 503, falha de rede, envio duplicado, perfis sem acesso, tipos JSON, CSRF e layout móvel. Os testes estão em [demo.spec.js](codigo/frontend/tests/demo.spec.js); a falha de rede é simulada no navegador, e o erro 503 é um controle explícito da demo.

### Nota de produção

No banco, usaria um `UPDATE` atômico condicionado ao ID, tenant, permissão, versão enviada e estado `contact_lawyer`, incrementando a versão na mesma operação. Duas requisições com versão 1 não conseguiriam atualizar a mesma versão: depois da primeira, a condição da segunda não corresponderia mais. Se nenhuma linha fosse alterada, faria uma leitura autorizada e consistente para identificar o erro na ordem exigida, sem expor registros de outros tenants.

### Limitações e fontes

Registrei as limitações encontradas e os itens fora do escopo em [LIMITACOES.md](LIMITACOES.md).

- [Enunciado C](QUESTOES.md#c-implement-a-safe-lead-update): regras e critérios seguidos.
- [Oracle — javac](https://docs.oracle.com/en/java/javase/21/docs/specs/man/javac.html): compilação e opções do script.
- [Oracle — BigInteger](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/BigInteger.html): inteiros sem o limite fixo de `long`.
- [PostgreSQL — Transaction Isolation](https://www.postgresql.org/docs/18/transaction-iso.html#XACT-READ-COMMITTED): atualização concorrente e reavaliação da condição; referência para a nota de produção, não um teste realizado.

## D. Plano de avaliação autorizada em staging

Este é um **plano futuro**, não uma avaliação executada. Nenhum endpoint, documento ou provedor real foi acessado para testar segurança.

### D1. Escopo e evidências

Antes de iniciar, obteria autorização escrita do responsável pelo ambiente, identificando hosts e endpoints de staging, período, técnicas permitidas, limites de volume, ações proibidas e contato para interrupção. O escopo excluiria produção e terceiros; e-mail/SMS usariam somente mocks, com bloqueio de saída para provedores reais.

Solicitaria contas fictícias de agente e manager em dois tenants, um papel sem permissão, leads e PDFs sintéticos conhecidos, além de possibilidade de restaurar as fixtures. Definiria a interrupção imediata ao encontrar dados reais, atingir um ativo fora do escopo, gerar envio externo inesperado, observar degradação do serviço ou receber ordem do responsável; preservaria apenas evidência mínima e comunicaria o ocorrido, sem ampliar a exploração.

Um achado conteria título, ambiente e versão, horário, papel/tenant fictícios, pré-condições, passos mínimos de reprodução, resultado esperado e observado, impacto, severidade justificada, evidência sanitizada, correção sugerida e procedimento de reteste. Capturas e logs usariam IDs de fixtures e identificadores de correlação; removeria cookies, cabeçalhos de autorização, tokens, URLs assinadas, credenciais e conteúdo desnecessário de documentos, com acesso restrito e prazo de descarte para as evidências.

**Fonte:** [OWASP — Logging](https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html) fundamenta o cuidado com dados sensíveis e o contexto necessário nos eventos. O escopo e as condições de parada acima são a proposta operacional para este exercício.

### D2. Download de PDF privado

Prepararia o lead 101/PDF-A em T-A e o lead 201/PDF-B em T-B, ambos com conteúdo fictício e hashes conhecidos. O contrato de teste assumiria download autenticado pelo backend, que verifica a associação entre documento, lead e tenant antes de entregar bytes.

| Caso | Ação autorizada em staging | Resposta e evidência esperadas |
| --- | --- | --- |
| Download permitido | Conta com acesso ao lead 101 solicita seu PDF-A | 200, `Content-Type: application/pdf`, `Content-Disposition: attachment` e bytes com o hash da fixture PDF-A. Registrar ID fictício, status, hash e evento de autorização, sem conteúdo do documento ou credenciais. |
| Acesso entre tenants negado | Mantendo a sessão de T-A, trocar o identificador solicitado pelo PDF-B conhecido de T-B | 404 genérico, equivalente a documento inexistente, sem bytes do PDF, metadados privados ou redirecionamento/URL assinada. Registrar requisição sanitizada, resposta e evento de negação; confirmar que os registros e arquivos permanecem intactos. |

Inspecionaria também duas proteções adicionais:

1. **Validação de upload:** lista permitida de PDF, limite de tamanho e verificação do tipo real/conteúdo, sem confiar apenas na extensão ou no `Content-Type` informado pelo cliente; processamento isolado e quarentena conforme a política definida.
2. **Armazenamento privado:** arquivos fora do diretório público, nomes/chaves gerados pelo servidor e caminho não controlado pelo cliente. Uma URL direta não deve contornar a autorização; se houver links assinados, verificar escopo e validade curta, tratando o link como credencial temporária.

**Fontes:** [OWASP WSTG — Testing for IDOR](https://wstg.owasp.org/v4.2/4-Web_Application_Security_Testing/05-Authorization_Testing/04-Testing_for_Insecure_Direct_Object_References/) orienta testes com usuários e objetos de permissões distintas; [OWASP — File Upload](https://cheatsheetseries.owasp.org/cheatsheets/File_Upload_Cheat_Sheet.html) fundamenta validação, limites e armazenamento protegido. Os IDs, respostas e evidências acima definem o teste proposto.

### D3. Lembretes e duplicidade

Usaria um relógio injetável, armazenamento de teste e um provedor simulado, sem rede nem destinatários reais. Criaria a solicitação fictícia R-1 no instante `t0`, com vencimento em `t0 + 48h`, associando o lembrete a uma chave estável, por exemplo `(tenant, lead, request_id, reminder_type)`; uma nova solicitação deve ter outra chave.

| Cenário com fixtures independentes | Execução planejada | Assertivas |
| --- | --- | --- |
| Ainda não venceu | Avançar até `t0 + 48h - 1s` e executar o worker | Zero chamadas ao mock; lembrete ainda pendente. |
| Cancelamento antes do vencimento | Fazer o lead sair de `contact_lawyer`, cancelar R-1, avançar até 48h e entregar o job duas vezes | Zero chamadas ao mock; estado cancelado preservado, sem reagendamento. |
| Vencido e elegível, com duplicata | Manter o lead em `contact_lawyer`, avançar até 48h e entregar duas cópias do mesmo job | Um envio aceito para a chave e um registro durável de conclusão; a duplicata não produz nova chamada após a confirmação. |
| Dois workers concorrentes | Sincronizar dois workers no mesmo lembrete vencido | Apenas um consegue reservar atomicamente o lembrete; o outro não envia. |
| Aceite seguido de timeout | O mock aceita o envio, mas simula perda da resposta antes de a aplicação gravar sucesso; então repetir o job | Tentativas usam a mesma chave; o mock idempotente registra apenas um envio aceito. Pode haver mais de uma chamada, mas não mais de um efeito. |
| Cancelamento durante o processamento | Pausar o worker após carregar o job, confirmar o cancelamento e retomar antes da decisão final de envio | A verificação atômica de elegibilidade impede o envio; zero chamadas ao mock. |

A correção proposta combina estado persistente, unicidade da chave, reserva atômica e revalidação do lead/cancelamento no ponto de decisão de envio; o job não é autoridade sobre o estado atual. Cancelamento e reserva precisam ser coordenados para definir qual operação venceu: não é possível prometer desfazer uma mensagem já aceita pelo provedor. Sem idempotência ou consulta de resultado no provedor, o intervalo entre aceite externo e confirmação local deixa resultado incerto; eu documentaria essa limitação e usaria reconciliação, sem alegar garantia de envio único apenas por uma flag no banco.

**Fontes:** [Stripe — Idempotent requests](https://docs.stripe.com/api/idempotent_requests) exemplifica chave estável e recuperação do resultado em retries; é uma referência conceitual, não uma integração de mensagens. [PostgreSQL — Transaction Isolation](https://www.postgresql.org/docs/18/transaction-iso.html#XACT-READ-COMMITTED) fundamenta a coordenação de alterações concorrentes. O relógio, mocks e cenários são o plano de teste proposto; não foram implementados nesta avaliação.

## E. Segurança de IA e documentos

O texto do PDF é uma **injeção indireta de prompt**: conteúdo externo tenta se passar por instrução confiável, induzindo acesso a outros documentos, envio indevido e exposição de segredo. Ele deve ser tratado como dado a resumir, sem autoridade para mudar permissões ou executar ações.

Para esse assistente de resumo, forneceria apenas o documento autorizado do tenant atual, sem credenciais no contexto e sem ferramentas de envio ou acesso livre a arquivos/rede. Autorização e bloqueio de ações ficariam no servidor, independentes da resposta do modelo; separaria instruções de conteúdo, validaria a estrutura da saída e exibiria o resumo como texto. Se a saída tentar solicitar uma ação proibida, a aplicação a bloquearia e registraria um evento sanitizado.

Testaria localmente com o PDF fictício, variantes da instrução e um modelo simulado que solicita envio ou retorna comandos maliciosos. Verificaria zero envios, zero leitura de documentos de outro tenant e ausência de um segredo-canário fictício nas saídas/logs; esse marcador ficaria apenas no servidor de teste, fora do contexto do modelo. Incluiria um PDF benigno para confirmar que o fluxo normal de resumo continua funcionando. Nenhum segredo real ou destinatário real seria usado. O mock verifica os controles da aplicação; não comprova resistência de um modelo real à injeção.

**Dizer ao modelo “não faça isso” não basta:** instruções são uma camada adicional; isolamento, permissões mínimas e controles externos ao modelo limitam os efeitos mesmo quando ele falha.

**Fonte:** [OWASP — LLM Prompt Injection Prevention](https://cheatsheetseries.owasp.org/cheatsheets/LLM_Prompt_Injection_Prevention_Cheat_Sheet.html). Esta defesa e seus testes são propostas, não uma integração de IA implementada ou validada contra um modelo real.

## F. English handoff

I found that the sample code lets a signed-in employee access a lead without checking which organization owns it or who may change it. This could expose another customer's information or allow an unauthorized change to a funding decision. With AI assistance, I implemented the access checks in Java and connected a local React form to that code. I reviewed the written answers and sources, while the AI assistant ran 63 Java tests and 9 browser tests, all of which passed. This remains a local exercise with fake data; real authentication, private document downloads, and production integrations still need implementation and authorized testing.


## Ferramentas, IA e verificação

- **Uso de IA:** usei Codex para organizar a entrega, consultar documentação e auxiliar na redação, no código Java/React e nos testes. Pedi também uma revisão independente das respostas D–F por outro agente de IA.
- **Minha conferência:** li o material e abri as fontes disponíveis até minha confirmação na conversa, sem encontrar divergências aparentes. Depois executei `codigo/run-demo.sh` no meu terminal: o build React, a verificação do Gson e a compilação Java concluíram; a tentativa de iniciar outra instância encontrou a porta 8080 ocupada pela demo já aberta pelo assistente. Esse resultado não substitui os testes automatizados descritos abaixo.
- **Execução dos testes:** o assistente compilou o código e executou os 63 testes Java e os 9 testes de navegador contra a API local. Todos passaram. Esses resultados são de execução automatizada com assistência de IA; não os apresento como comandos que executei pessoalmente.
- **Sugestão corrigida:** a primeira versão do script gerada pela IA usava `--release 17`, opção que falhou na instalação local. Durante a validação assistida, o script foi ajustado para compilar com a versão instalada e os testes passaram em OpenJDK 25.0.4. Não considero isso uma validação em JDK 17.
- **Ferramentas:** Java/OpenJDK 25.0.4, Node.js 22.22.2, npm, React, Vite, Gson, Playwright/Chromium, Git e GitHub CLI. As versões das dependências web estão fixadas em `package-lock.json`; Gson está fixado no script de execução. O IntelliJ foi solicitado para abrir a pasta; a execução confirmada foi pelo terminal e pelo navegador automatizado.
- **Documentação:** usei as referências OWASP, MDN, Stripe, PostgreSQL, Oracle/Java e JetBrains vinculadas nas respostas e no README, com consulta assistida em 30/09/2026. Para a integração, consultei também [React](https://react.dev/learn/build-a-react-app-from-scratch), [Vite](https://vite.dev/guide/), [Gson](https://github.com/google/gson), [HttpServer](https://docs.oracle.com/en/java/javase/21/docs/api/jdk.httpserver/com/sun/net/httpserver/HttpServer.html) e [Playwright](https://playwright.dev/docs/intro). Consultei a documentação do GitHub Pages para uma ideia de demo que descartei; nenhum site foi publicado.
- **Limites:** trabalhei com dados fictícios. Não testei sistemas reais da empresa. D e E são propostas; as limitações e os itens não implementados estão em [LIMITACOES.md](LIMITACOES.md).
- **Estado da entrega:** respostas A–F, código, testes e instruções estão incluídos. Os itens não implementados e limites de validação estão em [LIMITACOES.md](LIMITACOES.md); não afirmo ter entregue um sistema de produção.
