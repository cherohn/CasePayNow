# Limitações e itens não implementados

**Matheus Souza Garcez**

Registrei aqui os limites da solução entregue. Não apresento a demonstração como sistema pronto para produção.

## Aplicação local

| Limitação | Consequência prática |
| --- | --- |
| Dados em memória, separados por sessão de navegador | Reiniciar o Java ou restaurar os dados perde as alterações. Não há banco nem persistência. |
| Perfis fictícios selecionáveis na tela | O seletor permite testar agente, manager e acesso negado. Não é login real, não valida credenciais e não deve ser usado para controlar acesso em produção. |
| HTTP somente em `127.0.0.1:8080` | Não há TLS. O cookie de demo usa HttpOnly e SameSite=Strict, mas não Secure, porque a execução é HTTP local. |
| Sessões simplificadas | Não implementei expiração, logout, rotação em mudança de perfil ou gestão completa de sessões. O contexto existe no Java, mas sua escolha é livre na demo. |
| Processamento em uma thread | Não implementei concorrência de banco. O servidor serializa as requisições; a função isolada pressupõe uma thread. |
| Controles de teste na API | Restaurar fixtures, escolher perfil, provocar conflito e retornar 503 são recursos exclusivos da demo, não endpoints adequados a um sistema real. |
| Números do frontend usam Number do JavaScript | A interface foi feita para os IDs e versões pequenos das fixtures. A função Java suporta BigInteger, mas essa precisão arbitrária não está disponível no formulário. |

Se outra instância já estiver usando a porta 8080, uma nova inicialização falha com `Address already in use`. Nesse caso, uso a instância aberta ou a encerro antes de iniciar outra, conforme o README.

## O que foi e não foi validado

- **Confirmado:** compilação Java, build React, 63 testes da função e 9 testes Chromium contra a API Java local, usando dados fictícios. Os testes verificam erros, preservação de campos e estado armazenado; não demonstram ausência de toda vulnerabilidade possível.
- **Ambiente:** OpenJDK 25.0.4, Node.js 22.22.2 e Linux. Não executei a solução em outros JDKs, sistemas operacionais ou navegadores. O Playwright usou seu build de Chromium compatível de fallback para este sistema.
- **IntelliJ:** inclui configuração Maven para resolver Gson e as pastas de fontes. A validação confirmada foi por terminal e navegador automatizado, não pelo botão Run da IDE.
- **Compilador local:** não havia executável `javac`, mas o módulo `jdk.compiler` estava disponível. A tentativa inicial com `--release 17` falhou; usei a versão instalada. Um runtime sem compilador requer a instalação de um JDK completo.
- **Dependências:** os testes da função não precisam de rede. A primeira preparação do frontend e do servidor baixa dependências; o teste de navegador exige também instalar Chromium.

## Fora do exercício implementado

- Não corrigi nem executei o PHP original da seção B; seus testes negativos são propostas de revisão. Os testes C verificam a função Java e a integração local.
- Não implementei downloads privados, uploads, lembretes, envio de e-mail/SMS ou resumo por IA. D e E são planos para uma avaliação futura autorizada.
- Não executei testes de invasão ou integrações contra sistemas reais da empresa ou terceiros.
- Não publiquei site ou GitHub Pages.

## Revisão

D, E e F foram revisadas quanto aos itens do enunciado. As respostas, instruções e limitações estão incluídas; o tempo de preparação está registrado em ENTREGA.md. A revisão assistida e os testes executados não equivalem a uma auditoria de produção.
