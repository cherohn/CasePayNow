# Avaliação técnica — CasePayNow

Nome: a preencher pelo candidato.

Início registrado: 30/09/2026, 14h09, America/Sao_Paulo.
Limite de entrega: 30/09/2026, 15h24, incluindo preparação do GitHub.
Tempo efetivamente utilizado: preencher ao concluir; não declarar 75 minutos se terminar antes.

Material fictício, desenvolvido localmente. Este documento está em elaboração.

## A. Full-stack foundations

### A1. Um formulário que preserva o trabalho

- Ao enviar, mantenho os valores no formulário, valido campos e formatos no navegador para dar retorno imediato e desabilito novos envios enquanto a requisição está em andamento. Envio os dados por HTTPS, incluindo a escolha explícita de consentimento; a ausência de consentimento não deve ser convertida em autorização pelo servidor.
- A API é a autoridade de validação: verifica tipos, limites de tamanho, nome, presença de pelo menos telefone ou e-mail válido e as regras de consentimento. A validação do navegador melhora a experiência, mas pode ser contornada. O servidor normaliza os contatos conforme regras definidas e grava o lead e o registro de consentimento em uma transação, usando consultas parametrizadas.
- Para erros de validação, a API retorna 422 com erros por campo; o navegador preserva todos os valores, destaca os campos afetados e permite corrigir e reenviar. Não exibo mensagens internas do banco ou detalhes de exceções.
- Em timeout ou falha de rede, preservo o formulário e aviso que não foi possível confirmar o resultado: a gravação pode ter ocorrido mesmo sem resposta. Reabilito a tentativa, sem afirmar que o lead não foi salvo.
- Para evitar duplicidade nessa tentativa, gero uma chave de idempotência por envio lógico e reutilizo a mesma chave ao repetir o mesmo conteúdo. O servidor associa chave, escopo e hash do conteúdo ao resultado, com unicidade e gravação atômica; uma repetição retorna o resultado anterior, e a mesma chave com conteúdo diferente é rejeitada. Isso resolve repetições da mesma operação; possíveis duplicidades de contatos em envios distintos exigem uma regra de negócio própria.
- Somente depois da confirmação de persistência, por exemplo 201 com o identificador, mostro sucesso e limpo o formulário. Um novo envio lógico recebe outra chave de idempotência.

### A2. Cookies e segurança no navegador

Assumo frontend e API na mesma origem, exclusivamente por HTTPS. `Secure` restringe o envio do cookie a conexões seguras; `HttpOnly` impede que JavaScript leia o cookie; `SameSite` limita quando o navegador envia cookies em requisições entre sites, conforme o modo escolhido. Site e origem são conceitos diferentes: subdomínios podem ser do mesmo site e de origens diferentes.

`HttpOnly` não elimina XSS: um script injetado ainda pode executar ações autenticadas e ler dados acessíveis à página. `SameSite` é uma defesa adicional e não substitui todas as proteções contra CSRF; para operações de escrita com sessão em cookie, uso método apropriado, token CSRF e validação de origem. CORS controla quais origens podem ler respostas no navegador; não autoriza acesso a leads e não substitui verificações de tenant, papel e atribuição no servidor.

### A3. Sessões e logout

Após login ou aumento de privilégios, gero um novo identificador de sessão imprevisível e invalido o anterior, prevenindo fixação de sessão e reutilização do identificador antigo. O logout invalida a sessão no servidor e remove o cookie com os mesmos atributos de domínio e caminho usados na criação. O servidor aplica expiração por inatividade e duração máxima, rejeitando identificadores expirados mesmo que o navegador ainda mantenha o cookie.

Uma sessão opaca, mantida no servidor, facilita revogação imediata e atualização de permissões, ao custo de consultar e operar um armazenamento de sessões. Um JWT de longa duração em armazenamento acessível ao JavaScript pode ser roubado por XSS e costuma continuar válido até expirar; revogação antecipada exige estado adicional ou outro mecanismo. Para este CRM, prefiro uma sessão opaca em cookie `Secure`, `HttpOnly` e `SameSite`, acompanhada de proteção contra CSRF.

### A4. SQL e crescimento da caixa de entrada

Uso parâmetros vinculados pelo driver; `:tenant_id` recebe exclusivamente o tenant do contexto autenticado no servidor. Assumo `received_at` não nulo para as mensagens exibidas.

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

## B. Revisão do código inseguro

Pendente.

## C. Implementação, testes e comportamento do navegador

Pendente.

## D. Plano de avaliação autorizada em staging

Pendente.

## E. Segurança de IA e documentos

Pendente.

## F. English handoff

Pending.

## Ferramentas, IA e verificação

- Assistente de IA: Codex, utilizado para organizar a entrega e auxiliar na redação e implementação. Completar a declaração com o trabalho efetivamente realizado.
- Verificações realizadas pelo candidato: preencher apenas após executar/revisar pessoalmente.
- Sugestão gerada que foi conferida ou corrigida: preencher com um exemplo real ao concluir.
- Documentação consultada: registrar apenas fontes efetivamente consultadas.
- Itens não concluídos: atualizar no encerramento; as seções pendentes acima ainda não estão prontas para envio.
