# Avaliação técnica — CasePayNow

Nome: Matheus Souza Garcez.

Início registrado: 30/09/2026, 14h09, America/Sao_Paulo.
Limite de entrega: 30/09/2026, 15h24, incluindo preparação do GitHub.
Tempo efetivamente utilizado: preencher ao concluir; não declarar 75 minutos se terminar antes.

Material fictício, desenvolvido localmente. Este documento está em elaboração.

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

### Código e execução

- [Implementação — src/LeadService.java](src/LeadService.java).
- [Testes — tests/LeadServiceTest.java](tests/LeadServiceTest.java).
- [Script de execução — run-tests.sh](run-tests.sh).

Na raiz do repositório, com Java e compilador de um JDK 17 ou superior:

```sh
sh run-tests.sh
```

O script compila e executa localmente, sem Maven, Gradle, bibliotecas externas, banco ou rede. Ele usa `javac` quando disponível; caso o executável esteja ausente, tenta o módulo `jdk.compiler` da instalação de Java. Compilação e execução devem usar a mesma versão de Java; o ambiente efetivamente verificado foi **OpenJDK 25.0.4**, com compilação via módulo e verificações `-Xlint:all -Werror`.

**Resultado observado:** `PASS: 63 tests; no network or external dependencies.` Os testes lançam `AssertionError` em falhas; não dependem de habilitar `assert` com `-ea`. O processo termina com código diferente de zero se a compilação ou um teste falhar.

### Contrato e decisões

`LeadService.updateLead(auth, body, leads)` corresponde à função `update_lead` solicitada, com nomenclatura convencional de método Java. O retorno é `Result(status, data_or_error)`: sucesso contém exclusivamente `id`, `status` e `version`; erro contém apenas um código genérico em `error`, sem dados do registro.

O contexto `Auth(userId, tenantId, role)` representa os campos confiáveis `user_id`, `tenant_id` e `role` fornecidos pelo servidor. A função não constrói esse contexto a partir do corpo. O armazenamento é um `Map<BigInteger, Lead>` mutável e confiável, com registros imutáveis; uma atualização substitui somente o registro autorizado, preservando tenant, atribuição e notas.

O argumento `body` representa JSON **já desserializado**: objetos viram `Map`, listas viram `List` e valores mantêm seus tipos. Não foi implementado parser de texto JSON, serialização HTTP ou servidor; os testes chamam a função diretamente com essas representações. Para `id` e `version`, são aceitos somente valores positivos dos tipos inteiros `Byte`, `Short`, `Integer`, `Long` e `BigInteger`; texto, booleanos e números decimais são rejeitados, sem coerção. `BigInteger` evita impor um limite artificial de 32/64 bits e impede overflow no incremento da versão; um futuro parser deve preservar a distinção entre tokens inteiros e decimais.

**Campos inesperados são rejeitados**, inclusive `role`, `tenant_id`, `owner`, `assigned_to` e `notes`. A função exige exatamente `id`, `status` e `version`, e não faz atribuição automática de campos ao registro.

| Ordem | Verificação | Resposta | Efeito no armazenamento |
| --- | --- | --- | --- |
| 1 | `auth` ausente | 401 / `unauthenticated` | Nenhum |
| 2 | Papel diferente de `agent` e `manager` | 403 / `forbidden` | Nenhum |
| 3 | Corpo, campos ou valores inválidos | 422 / `invalid_body` | Nenhum |
| 4 | Lead inexistente, tenant diferente ou agente não atribuído | 404 / `not_found` | Nenhum; sem dados do lead |
| 5 | Versão enviada diferente da armazenada | 409 / `version_conflict` | Nenhum |
| 6 | Estado atual diferente de `contact_lawyer` | 422 / `invalid_transition` | Nenhum |
| 7 | Atualização válida e autorizada | 200 / `{id, status, version}` | Altera status e incrementa versão em 1 |

Essa ordem faz parte do contrato: um replay com versão antiga retorna 409, mesmo que a primeira atualização já tenha tornado o estado terminal. Uma tentativa de alterar o estado terminal com a versão atual retorna 422.

### Cobertura dos testes

Cada caso independente cria fixtures novas para 101, 102 e 201. Todos verificam a resposta completa e o mapa inteiro de registros; os casos com sucesso conferem que os campos não relacionados e os outros leads continuam iguais. Os casos de replay e de estado terminal mantêm propositalmente a mesma fixture entre a primeira escrita e a tentativa seguinte.

| Requisito do enunciado | Cenário executado |
| --- | --- |
| 1. Sem autenticação | 401 e nenhum registro alterado |
| 2. Agente U-A atualiza 101 | 200, status atualizado e versão 2 |
| 3. U-A tenta atualizar 102 | 404, sem dados e sem mutação |
| 4. Manager T-A tenta 201 de T-B | 404, sem dados e sem mutação |
| 5. Manager T-A atualiza 102 | 200 e somente a alteração permitida |
| 6. Destino desconhecido | 422 e armazenamento intacto |
| 7. Replay da versão original | Primeira escrita 200; repetição 409 sem segunda escrita |
| 8. Papel desconhecido e autenticação forjada no corpo | 403 para papel desconhecido; 422 para campos extras que tentam elevar o agente ou trocar o tenant |

Os demais casos cobrem campos ausentes, corpo nulo ou não objeto, valores de tipo incorreto, strings com espaços/capitalização diferente, ambos os destinos permitidos, ambos os estados terminais, ID inexistente, agente de outro tenant mesmo com atribuição coincidente, versão futura, precedência das validações e inteiros além do limite de `long`. Também verificam que a função não altera o próprio corpo recebido.

### Comportamento da interface — proposta, sem página implementada

- Ao clicar em enviar, verifico uma flag `submitting`; se já houver envio em andamento, ignoro o segundo clique. Defino a flag antes de iniciar qualquer operação assíncrona, desabilito o botão e preservo uma cópia dos campos e da versão exibida.
- Envio apenas `{id, status, version}`, usando a versão carregada do servidor, pelo método de escrita e com a proteção CSRF do wrapper. Papel, tenant e atribuição não são enviados como autoridade pelo navegador.
- Durante o envio, mostro progresso sem alterar o status confirmado do lead. Não limpo o formulário nem avanço a versão local antes de receber sucesso.
- Em falha de rede, timeout ou erro 5xx, preservo os valores e informo que não foi possível confirmar a atualização. A gravação pode ter ocorrido: uma nova tentativa conserva a versão original, permitindo ao servidor detectar um replay.
- Em 409, preservo a intenção digitada, aviso sobre o conflito e busco o estado atual em uma leitura autorizada. Mostro a diferença ao usuário; não substituo silenciosamente sua edição nem reenvio com uma versão mais nova. Se o estado atual já for terminal, não ofereço outra transição; uma falha nessa leitura mantém o aviso e os valores.
- Em 422, apresento o erro para correção; em 401, solicito nova autenticação sem perder o rascunho; em 403/404, informo indisponibilidade de acesso sem revelar dados do registro. Essas respostas não são tratadas como sucesso.
- Somente um 200 com o resultado esperado confirma a atualização: então aplico `id`, `status` e `version` devolvidos à visualização e informo sucesso. Em um bloco `finally`, removo a flag e restauro os controles aplicáveis, mantendo desabilitadas as transições de um lead terminal.

### Concorrência em produção

Em um banco real, executaria uma atualização condicional atômica com ID, tenant, autorização, `version = :submitted_version` e `status = 'contact_lawyer'` no filtro, incrementando a versão no mesmo `UPDATE`. Duas requisições com versão 1 não poderiam confirmar a mesma escrita: após a primeira, a condição de versão da segunda deixaria de corresponder; uma alternativa é bloquear a linha e validar/atualizar dentro da mesma transação. Se nenhuma linha for atualizada, faria uma leitura autorizada e consistente para classificar o erro na ordem exigida, sem revelar registros de outros tenants.

### Limitações e fontes

A implementação é apenas a função em memória, para execução por uma thread. HTTPS, método HTTP, autenticação de sessão e CSRF são responsabilidades do wrapper assumidas pelo enunciado; não foram implementadas ou testadas aqui. Não há persistência, teste de concorrência em banco, interface renderizada ou correção aplicada ao PHP da seção B.

- [Enunciado C](QUESTOES.md#c-implement-a-safe-lead-update): autoridade para regras de permissão, ordem das verificações e respostas; os testes seguem esse contrato.
- [Oracle — javac](https://docs.oracle.com/en/java/javase/21/docs/specs/man/javac.html): compilação de fontes e opções usadas no script. A documentação consultada descreve o comando; o resultado dos testes vem da execução local registrada acima.
- [Oracle — BigInteger](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/BigInteger.html): representação imutável de inteiros de precisão arbitrária, usada para IDs e versões sem overflow de `long`.
- [PostgreSQL — Transaction Isolation](https://www.postgresql.org/docs/18/transaction-iso.html#XACT-READ-COMMITTED): comportamento de atualizações concorrentes e reavaliação da condição de busca após outra atualização. Fundamenta a nota de produção, não um teste de banco realizado.

## D. Plano de avaliação autorizada em staging

Pendente.

## E. Segurança de IA e documentos

Pendente.

## F. English handoff

Pending.

## Ferramentas, IA e verificação

- Assistente de IA: Codex, utilizado para organizar a entrega, consultar documentação, auxiliar na redação, gerar a implementação Java e os testes e executá-los no ambiente local. Até esta etapa, a execução automatizada da seção C resultou em 63 testes aprovados; isso não equivale a revisão pessoal do candidato.
- Verificações realizadas pelo candidato: preencher apenas após executar/revisar pessoalmente.
- Sugestão gerada conferida e corrigida durante a assistência: a primeira versão do script usava `--release 17`; a instalação local respondeu `release version 17 not supported`. O assistente ajustou o script para compilar com a versão instalada e verificou a execução em OpenJDK 25.0.4; não foi alegado teste em JDK 17. O candidato ainda deve registrar o que conferiu pessoalmente.
- Documentação consultada pelo assistente: OWASP Cheat Sheet Series, MDN Web Docs, documentação de idempotência da Stripe, PostgreSQL e Java/Oracle, conforme links junto às respostas das seções A–C. Consulta em 30/09/2026, somente a páginas públicas de documentação; nenhuma chamada a APIs de negócio ou sistemas da empresa.
- Itens não concluídos: atualizar no encerramento; as seções pendentes acima ainda não estão prontas para envio.
