# CasePayNow — Avaliação técnica Full Stack

**Candidato:** Matheus Souza Garcez.

Resolução da avaliação fictícia de fundamentos de CRM, segurança de aplicações e uso de IA. A implementação está em **Java**, executada localmente com dados fictícios, sem serviços reais.

> **Entrega em revisão:** A e B respondidas; C implementada, com 63 testes aprovados e frontend descrito em sete passos. D–F estão como rascunhos. A entrega ainda não está finalizada.

## Como ler a entrega

Comece por [QUESTOES.md](QUESTOES.md) para ler o enunciado organizado por seção. O documento [ENTREGA.md](ENTREGA.md) reúne as respostas na mesma ordem; as fontes ficam junto de cada resposta, com uma explicação do que sustentam. O código e os testes da seção C estão em arquivos próprios, relacionados abaixo.

| Seção | Conteúdo | Situação |
| --- | --- | --- |
| [A — Fundamentos full stack](ENTREGA.md#a-full-stack-foundations) | Formulário, cookies, sessões e SQL | Respondida, com fontes |
| [B — Revisão de código](ENTREGA.md#b-revisão-do-código-inseguro) | Cinco achados, correções e testes negativos propostos | Respondida, com fontes; testes não executados |
| [C — Implementação e testes](ENTREGA.md#c-implementação-testes-e-comportamento-do-navegador) | Atualização segura de lead em Java e comportamento proposto da interface | Implementada; 63 testes aprovados |
| [D — Plano de segurança](ENTREGA.md#d-plano-de-avaliação-autorizada-em-staging) | Escopo, PDFs privados e lembretes | Rascunho; testes não executados |
| [E — IA e documentos](ENTREGA.md#e-segurança-de-ia-e-documentos) | Defesa contra instruções maliciosas em PDF | Rascunho; proposta não implementada |
| [F — English handoff](ENTREGA.md#f-english-handoff) | Comunicação para um gestor não técnico | Rascunho em inglês |

## Execução e testes

Pré-requisito: **JDK 17 ou superior**, com compilação e execução na mesma versão. Verificado em **OpenJDK 25.0.4**, no Linux; não há dependências externas para baixar.

### Baixar o projeto

Em uma pasta onde ainda não exista este repositório:

```sh
git clone https://github.com/cherohn/CasePayNow.git
cd CasePayNow
```

Também é possível usar **Code → Download ZIP** no GitHub e extrair a pasta. Quem já está com esta pasta aberta não precisa clonar novamente.

### Rodar todos os testes

Na raiz do repositório:

```sh
sh codigo/java/run-tests.sh
```

O script usa `javac` ou, se o executável estiver ausente, o módulo `jdk.compiler`. Um runtime sem compilador não é suficiente. A saída detalha os cenários e termina com:

```text
PASS: 63 tests; no network or external dependencies.
```

Falhas geram código de saída diferente de zero. Os arquivos compilados ficam em `codigo/java/build/`, ignorado pelo Git. Não é necessário habilitar assertions da JVM, instalar framework ou iniciar servidor.

### Compilar e executar manualmente

Com `javac` disponível, estes são os comandos equivalentes, a partir da raiz:

```sh
cd codigo/java
mkdir -p build
javac -encoding UTF-8 -Xlint:all -Werror -d build src/LeadService.java tests/LeadServiceTest.java
java -cp build LeadServiceTest
```

O código também pode ser compilado em outros sistemas com um JDK compatível, mas a execução foi verificada apenas no Linux. Em Windows sem shell POSIX, crie `build` com `mkdir build` e execute os comandos `javac` e `java` acima no terminal.

### Usar o IntelliJ IDEA

1. Em **File → Open**, abra a pasta `CasePayNow` que contém este README.
2. Abra o terminal integrado em **View → Tool Windows → Terminal** (`Alt+F12` no Linux).
3. Confirme que o terminal está na raiz do projeto e execute `sh codigo/java/run-tests.sh`.
4. Confira a última linha: `PASS: 63 tests; no network or external dependencies.`

Esse caminho usa os mesmos comandos já verificados, sem depender da configuração de módulos da IDE. Para acompanhar a regra, abra [LeadService.java](codigo/java/src/LeadService.java); os casos executados estão em [LeadServiceTest.java](codigo/java/tests/LeadServiceTest.java). Fonte: [JetBrains — Terminal integrado](https://www.jetbrains.com/help/idea/terminal-emulator.html).

### O que observar na demonstração local

Os primeiros testes seguem a ordem dos oito requisitos do enunciado: sucesso do agente, bloqueio de outro responsável e de outro tenant, permissão do manager e rejeição de replay. Cada linha `OK` significa que a resposta **e o estado dos registros** foram conferidos; uma divergência interrompe a execução com erro. A execução começa com fixtures novas e não usa banco, navegador ou serviços externos.

Não há GitHub Pages nem site publicado. O frontend é a descrição de sete passos na seção C, no formato permitido pela avaliação.

## Organização dos arquivos

| Arquivo | Finalidade |
| --- | --- |
| [QUESTOES.md](QUESTOES.md) | Enunciado e regras, separados por seção |
| [ENTREGA.md](ENTREGA.md) | Respostas, decisões, referências e declaração de IA |
| [LIMITACOES.md](LIMITACOES.md) | Limitações encontradas, itens fora do escopo e pontos em aberto |
| [codigo/java/src/LeadService.java](codigo/java/src/LeadService.java) | Função `updateLead` e tipos de autenticação, registro e resultado |
| [codigo/java/tests/LeadServiceTest.java](codigo/java/tests/LeadServiceTest.java) | Testes executáveis com fixtures fictícias e verificação de resposta e estado |
| [codigo/java/run-tests.sh](codigo/java/run-tests.sh) | Compilação e execução em um comando |

Os testes cobrem os oito requisitos da seção C, além de tipos inválidos, campos extras, estados terminais, ordem das validações e inteiros grandes. O contrato recebe JSON já desserializado em objetos Java; não há parser HTTP, banco, login ou página web. A descrição do frontend e a solução de concorrência em banco são propostas documentadas, não implementações testadas.

## Escopo e limites

- Apenas dados fictícios e execução local.
- Nenhum teste contra sites, CRMs, e-mail, SMS ou integrações reais.
- Consultas a documentação pública servem de fundamentação; não comprovam que uma implementação foi testada.
- A seção D descreve testes futuros sujeitos a autorização; não representa testes realizados.

## Tempo e transparência

Início registrado: **30/09/2026 às 14h09**, horário de Brasília. Limite: **15h24**, incluindo a preparação e o envio ao GitHub. O tempo real e os itens não concluídos serão registrados no encerramento.

O uso de IA, a documentação consultada e as verificações estão na [declaração de ferramentas](ENTREGA.md#ferramentas-ia-e-verificação). Ela distingue a leitura e conferência de fontes feitas pelo candidato da execução dos testes feita pelo assistente.

## Entrega ao avaliador

As respostas, código, testes e comando de execução devem ser enviados na mesma conversa do Indeed indicada no enunciado. Este repositório organiza o material e pode acompanhar a mensagem como link; um documento e uma pasta de código ou ZIP também são formatos aceitos.
