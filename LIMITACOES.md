# Limitações e pontos em aberto

Registro do estado atual da avaliação de **Matheus Souza Garcez**. Estes pontos ficam documentados; não são uma lista de funcionalidades a implementar nesta entrega.

## Limitações encontradas na seção C

| Limitação | Consequência |
| --- | --- |
| Armazenamento apenas em memória, para uma thread | Os dados não persistem ao encerrar o processo. A função não garante segurança para chamadas simultâneas; o enunciado permite essa simplificação. |
| Corpo recebido como JSON já desserializado | Os testes verificam objetos Java, não texto JSON ou requisições HTTP. Um futuro parser precisa preservar os tipos e não converter strings/booleanos em inteiros. |
| Frontend descrito em sete passos | Não há página ou teste de navegador. Esse formato é permitido pelo enunciado; o comportamento não foi verificado em uma interface real. |
| Execução verificada somente em OpenJDK 25.0.4 no Linux | O código usa recursos disponíveis em Java 17, mas não foi executado em JDK 17, Windows ou macOS. |
| Instalação local sem executável `javac` | O script usa o módulo `jdk.compiler`, que estava disponível. Um runtime sem esse módulo exige um JDK completo. |
| Compilação local com `--release 17` falhou | O script foi ajustado para compilar com a versão instalada. Não foi instalada outra distribuição para ampliar a matriz de testes. |
| Validação feita pelo terminal | Os 63 testes passaram após a organização das pastas. Não foi validada uma configuração de execução gráfica do IntelliJ; o README orienta usar o terminal integrado com o mesmo comando. |

## Fora do escopo solicitado

- **HTTPS, sessão, método HTTP e CSRF:** assumidos como responsabilidade do wrapper, conforme o enunciado. Não foram implementados nem testados.
- **Banco e concorrência real:** há uma nota de produção na seção C, sem implementação de banco ou teste com dois processos.
- **PHP da seção B:** revisão estática, com correções e testes negativos propostos. O trecho não foi executado nem corrigido em um sistema real.
- **Downloads, lembretes e integração de IA:** D e E descrevem propostas de avaliação/defesa. Não há serviços externos ou testes de integração.

## Revisão da entrega

A entrega continua em revisão. D–F estão como rascunhos, e o tempo total será registrado no encerramento, respeitando o limite de 15h24 de 30/09/2026, horário de Brasília.
