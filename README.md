# CasePayNow — Avaliação Full Stack

**Matheus Souza Garcez**

Respostas A–F e uma aplicação local com **React + Java** para demonstrar a atualização segura de leads. O formulário chama a API Java, que executa a mesma função dos testes. Todos os dados são fictícios; nada foi publicado no GitHub Pages.

**Verificado:** 63 testes da função Java e 9 testes de navegador passaram. Os limites estão em [LIMITACOES.md](LIMITACOES.md).

## Ler a avaliação

| Documento | Conteúdo |
| --- | --- |
| [QUESTOES.md](QUESTOES.md) | Enunciado organizado por seção |
| [ENTREGA.md](ENTREGA.md) | Respostas A–F, fontes e declaração de uso de IA |
| [LIMITACOES.md](LIMITACOES.md) | O que a solução faz, o que não faz e o que não foi validado |

A e B trazem fundamentos e revisão de código. C contém a implementação, os testes e o comportamento da interface. D e E são planos de segurança, sem testes em sistemas reais. F é o relato em inglês.

## Baixar

```sh
git clone https://github.com/cherohn/CasePayNow.git
cd CasePayNow
```

Ou use **Code → Download ZIP** no GitHub e extraia a pasta. Se o projeto já estiver aberto na sua máquina, não precisa clonar de novo.

## Abrir a aplicação completa

Ambiente verificado: **Linux, OpenJDK 25.0.4 e Node.js 22.22.2**. A função Java usa recursos de Java 17; a demo requer também **Node.js 22.12 ou superior**, npm, curl e sha256sum. É necessário um JDK com compilador, não apenas um runtime sem `jdk.compiler`.

Na raiz do projeto:

```sh
sh codigo/run-demo.sh
```

Depois abra **http://127.0.0.1:8080** no navegador. Mantenha o terminal aberto; use `Ctrl+C` para encerrar.

Na primeira execução, o script instala as dependências npm, baixa Gson 2.14.0 do Maven Central e verifica seu SHA-256. Em seguida, compila React e Java e inicia o servidor. As versões npm estão fixadas no arquivo de lock; a instalação inicial precisa de internet, mas a aplicação em execução usa somente a máquina local. O script de início completo foi verificado no Linux.

Se aparecer `java.net.BindException: Address already in use`, a porta 8080 está ocupada: isso acontece ao iniciar uma segunda instância. Se for a própria demo, abra o endereço já em execução ou encerre a instância anterior com `Ctrl+C` no terminal onde ela roda; depois repita o comando. Não é necessário recompilar ou reinstalar dependências para corrigir um conflito de porta. Use exatamente `127.0.0.1:8080`: o servidor verifica esse host e não está configurado para outros endereços.

## Validar pela tela

1. Com **Agente U-A · T-A**, salve o lead **101**. O retorno deve ser 200 e a versão deve passar a 2. O estado se torna terminal e o botão de salvar fica desabilitado.
2. Clique em **Restaurar dados**, informe o ID **102** e tente salvar com o mesmo agente. A resposta deve ser 404, sem alterar o registro.
3. Escolha **Manager · T-A**, selecione o lead **102** e salve. Essa atualização deve ser permitida. O ID **201**, de T-B, deve continuar negado para esse manager.
4. Restaure os dados, selecione um lead acessível e abra **Conferir erro e conflito**. Clique em **Simular outra edição no servidor** e depois salve: deve aparecer 409, sem apagar sua escolha ou reenviar automaticamente.
5. Para testar falha do servidor, restaure os dados, marque **Simular erro 503 antes de salvar** e envie. Os campos devem permanecer preenchidos, e o estado confirmado não muda.

Os controles de perfil, restauração e falhas existem somente para esta demonstração. O histórico mostra respostas reais da API Java; as regras de permissão e de transição não estão duplicadas em JavaScript.

## Executar os testes

### Função Java — sem dependências externas

```sh
sh codigo/java/run-tests.sh
```

Resultado esperado:

```text
PASS: 63 tests; no network or external dependencies.
```

Cada caso verifica a resposta e o estado de todos os registros. O script usa `javac` ou o módulo `jdk.compiler`, compila em `codigo/java/build/` e falha com código de saída diferente de zero se houver erro. Não é necessário habilitar `-ea`.

Com `javac` disponível, a compilação manual da função e seus testes é:

```sh
cd codigo/java
mkdir -p build
javac -encoding UTF-8 -Xlint:all -Werror -d build src/LeadService.java tests/LeadServiceTest.java
java -cp build LeadServiceTest
```

### React + API Java — navegador

Na raiz do projeto:

```sh
cd codigo/frontend
npm ci
npx playwright install chromium
npm run test:browser
```

Resultado verificado: **9 passed**. O Playwright usa a demo já aberta em 8080 ou inicia uma instância local automaticamente. Os testes cobrem escrita no Java, permissões, conflito, preservação em falhas, duplo envio, tipos JSON, CSRF e largura de tela móvel. O download do navegador precisa de internet; nenhum sistema real é testado.

## Abrir no IntelliJ IDEA

1. Abra o **pom.xml da raiz como projeto Maven**. Ele declara Gson e as pastas de código/testes.
2. Se já abriu a pasta como projeto Java simples, clique com o botão direito em `pom.xml` → **Add as Maven Project**, ou use **Reload All Maven Projects** na janela Maven.
3. Configure um JDK compatível no projeto e aguarde a resolução de `com.google.code.gson:gson:2.14.0`. Os imports `com.google.gson` pertencem a essa biblioteca.
4. Abra **View → Tool Windows → Terminal** (`Alt+F12`) e rode `sh codigo/run-demo.sh` na raiz para abrir a aplicação completa. Para conferir apenas a função, rode `sh codigo/java/run-tests.sh`.

O `pom.xml` serve para a IDE reconhecer fontes e dependências; os resultados de teste documentados vêm dos comandos acima. **`mvn test` sozinho não executa o nosso runner de 63 casos**, que usa um `main` com verificações explícitas. A execução pela interface gráfica do IntelliJ não foi confirmada; o terminal e o Chromium automatizado foram usados na validação. Fonte: [JetBrains — Terminal integrado](https://www.jetbrains.com/help/idea/terminal-emulator.html).

## Código separado

| Caminho | Responsabilidade |
| --- | --- |
| [LeadService.java](codigo/java/src/LeadService.java) | Autenticação recebida, permissão, validação e atualização em memória |
| [DemoServer.java](codigo/java/src/DemoServer.java) | API HTTP local, JSON, sessão fictícia e arquivos do React |
| [LeadServiceTest.java](codigo/java/tests/LeadServiceTest.java) | 63 testes da função, com fixtures novas por cenário |
| [main.jsx](codigo/frontend/src/main.jsx) | Formulário React e tratamento das respostas |
| [style.css](codigo/frontend/src/style.css) | Estilos e adaptação para tela pequena |
| [demo.spec.js](codigo/frontend/tests/demo.spec.js) | 9 testes de navegador/integração |
| [run-demo.sh](codigo/run-demo.sh) | Prepara e inicia frontend e Java com um comando |
| [pom.xml](pom.xml) | Dependência Gson e configuração para importar na IDE |

Fluxo: **formulário React → POST JSON → DemoServer → LeadService → resposta HTTP → confirmação na tela**. O servidor Java também entrega os arquivos do frontend, mantendo tudo na mesma origem.

## Escopo, tempo e transparência

A demonstração é HTTP local, com perfis fictícios e dados em memória por sessão. Não implementei autenticação real, banco ou integrações de documentos/mensagens/IA. Os detalhes estão em [LIMITACOES.md](LIMITACOES.md).

Comecei em **30/09/2026 às 14h09**, horário de Brasília. O limite é **15h24**, incluindo GitHub. Até a revisão final às **15h12**, utilizei aproximadamente **63 minutos**; os commits registram o envio ao repositório. Declarei o uso de IA e distingui minha leitura das fontes da execução assistida dos testes em [ENTREGA.md](ENTREGA.md#ferramentas-ia-e-verificação).

O material deve acompanhar a resposta na mesma conversa do Indeed. Este repositório organiza as respostas e o código; não substitui a mensagem ao avaliador.
