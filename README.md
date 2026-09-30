# CasePayNow — Avaliação técnica Full Stack

**Candidato:** Matheus Souza Garcez.

Resolução da avaliação fictícia de fundamentos de CRM, segurança de aplicações e uso de IA. A implementação está em **Java**, executada localmente com dados fictícios, sem serviços reais.

> **Em elaboração:** A–C estão respondidas, e os 63 testes da implementação Java passaram. D–F e a revisão final ainda estão pendentes.

## Como ler a entrega

Comece por [QUESTOES.md](QUESTOES.md) para ler o enunciado organizado por seção. O documento [ENTREGA.md](ENTREGA.md) reúne as respostas na mesma ordem; as fontes ficam junto de cada resposta, com uma explicação do que sustentam. O código e os testes da seção C estão em arquivos próprios, relacionados abaixo.

| Seção | Conteúdo | Situação |
| --- | --- | --- |
| [A — Fundamentos full stack](ENTREGA.md#a-full-stack-foundations) | Formulário, cookies, sessões e SQL | Respondida, com fontes |
| [B — Revisão de código](ENTREGA.md#b-revisão-do-código-inseguro) | Cinco achados, correções e testes negativos propostos | Respondida, com fontes; testes não executados |
| [C — Implementação e testes](ENTREGA.md#c-implementação-testes-e-comportamento-do-navegador) | Atualização segura de lead em Java e comportamento proposto da interface | Implementada; 63 testes aprovados |
| [D — Plano de segurança](ENTREGA.md#d-plano-de-avaliação-autorizada-em-staging) | Escopo, PDFs privados e lembretes | Pendente |
| [E — IA e documentos](ENTREGA.md#e-segurança-de-ia-e-documentos) | Defesa contra instruções maliciosas em PDF | Pendente |
| [F — English handoff](ENTREGA.md#f-english-handoff) | Comunicação para um gestor não técnico | Pendente |

## Execução e testes

Pré-requisito: **JDK 17 ou superior**, com compilação e execução na mesma versão. Verificado em **OpenJDK 25.0.4**, no Linux; não há dependências externas para baixar. Na raiz da pasta, execute:

```sh
sh run-tests.sh
```

O script usa `javac` ou, se o executável estiver ausente, o módulo `jdk.compiler`. Um runtime sem compilador não é suficiente. A saída detalha os cenários e termina com:

```text
PASS: 63 tests; no network or external dependencies.
```

Falhas geram código de saída diferente de zero. Os arquivos compilados ficam em `build/`, ignorado pelo Git. Não é necessário habilitar assertions da JVM, instalar framework ou iniciar servidor.

| Arquivo | Finalidade |
| --- | --- |
| [QUESTOES.md](QUESTOES.md) | Enunciado e regras, separados por seção |
| [ENTREGA.md](ENTREGA.md) | Respostas, decisões, referências e declaração de IA |
| [src/LeadService.java](src/LeadService.java) | Função `updateLead` e tipos de autenticação, registro e resultado |
| [tests/LeadServiceTest.java](tests/LeadServiceTest.java) | Testes executáveis com fixtures fictícias e verificação de resposta e estado |
| [run-tests.sh](run-tests.sh) | Compilação e execução em um comando |

Os testes cobrem os oito requisitos da seção C, além de tipos inválidos, campos extras, estados terminais, ordem das validações e inteiros grandes. O contrato recebe JSON já desserializado em objetos Java; não há parser HTTP, banco, login ou página web. A descrição do frontend e a solução de concorrência em banco são propostas documentadas, não implementações testadas.

## Escopo e limites

- Apenas dados fictícios e execução local.
- Nenhum teste contra sites, CRMs, e-mail, SMS ou integrações reais.
- Consultas a documentação pública servem de fundamentação; não comprovam que uma implementação foi testada.
- A seção D descreverá testes futuros sujeitos a autorização; não representa testes realizados.

## Tempo e transparência

Início registrado: **30/09/2026 às 14h09**, horário de Brasília. Limite: **15h24**, incluindo a preparação e o envio ao GitHub. O tempo real e os itens não concluídos serão registrados no encerramento.

O uso de IA, a documentação consultada e as verificações estão na [declaração de ferramentas](ENTREGA.md#ferramentas-ia-e-verificação). A revisão pessoal do candidato deve ser registrada somente após ser realizada.

## Entrega ao avaliador

As respostas, código, testes e comando de execução devem ser enviados na mesma conversa do Indeed indicada no enunciado. Este repositório organiza o material e pode acompanhar a mensagem como link; um documento e uma pasta de código ou ZIP também são formatos aceitos.
