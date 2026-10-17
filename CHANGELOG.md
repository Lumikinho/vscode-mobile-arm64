# Changelog

Todas as mudanças relevantes deste projeto.

O formato segue [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/) e
o versionamento é [SemVer](https://semver.org/lang/pt-BR/).

## [Não publicado]

### Adicionado

- Workflow do GitHub Actions (`.github/workflows/build.yml`): build do APK
  `arm64-v8a` em JDK 17 + NDK 28.2.13676358, com upload do artefato.
- `CHANGELOG.md` e `.gitattributes` (normalização de line ending para build
  reprodutível).

## [1.1.0] — 2026-10-16

### Corrigido

- **Terminal: trocar de sessão não trocava a sessão.** O attach da PTY era
  `LaunchedEffect(view, generation)`; como duas sessões novas têm
  `generation == 0`, a troca pelo `SessionRow` não re-executava o efeito. A
  sessão nova nunca era criada, a view continuava exibindo a antiga e as teclas
  da barra escreviam numa sessão inexistente.
- **Vazamento no cache do explorer.** `ExplorerViewModel` guardava as árvores
  SAF num `Map` sem eviction — trocar de pasta N vezes mantinha N árvores
  completas na heap. Virou cache de tamanho 1, limpo também no `clear()`.
- **Scope do LSP nunca cancelado.** `LanguageServerManager` criava um
  `CoroutineScope` próprio sem `close()`, deixando o reader bloqueado em
  `input.read()` e os sockets vivos além da Activity. O `stopAll()` foi
  removido: ele lançava no mesmo scope que o `close()` cancela, então o
  shutdown nunca chegava ao servidor.
- **Alocação O(n²) na leitura do JSON-RPC.** `readMessage()` chamava
  `toByteArray()` a cada byte lido do header.
- **Tarball do Alpine sobrevivia à falha.** O `.tar.gz` (~10 MB) só era apagado
  no caminho feliz; se a verificação do sha256 falhasse, ficava em disco.
  Movido para `finally`.
- **Referência retida à `TerminalView`.** `TermMods` guardava a view para o
  `onSingleTapUp` sem nunca soltar; a view segura a `TerminalSession` anexada.
- **`lineColOf`/`cursorLineCol` copiando o documento.** Usavam
  `text.take(caret)`, alocando o prefixo inteiro do arquivo a cada tecla só
  para exibir "Ln 12, Col 4". Substituídos por varredura com `indexOf`.
- **Caminho lento no SAF.** `safTree` usava `DocumentFile.listFiles()`, que faz
  uma query ao provider por arquivo, e o fallback rápido nunca era alcançado
  porque `listFiles()` não lança exceção. Agora `rawTreeList` é o padrão.
- **`newTarget` não definido nas views legadas.** O `+` da sidebar herdava a
  raiz `local` de uma navegação anterior e criava o arquivo na pasta errada.
- **Latch de Alt sem efeito.** Acendia na barra inferior e era descartado sem
  mudar nada; agora move o cursor por palavra nas setas.
- **Fonte do terminal sem controle.** Ao remover o terminal simples, o único
  controle de tamanho desapareceu e `setTerminalFont()` ficou sem chamador.
  Voltou como `A−`/`A+` na barra adaptativa, com limites compartilhados.

### Adicionado

- **Virtualização do editor.** Gutter e camada de realces só compõem a faixa
  visível mais 8 linhas de overscan. Antes eram N `Text` + N `Box` remontados a
  cada tecla, com N = número de linhas do arquivo (até 3000 pelo cutoff).
- **Problems clicável.** Itens abrem o arquivo e posicionam o cursor na linha,
  com o nome do arquivo visível na linha. Quando o documento não está aberto,
  procura no workspace; se não achar, avisa em vez de falhar quieto.
- **Ctrl/Alt/Shift do teclado físico no terminal.** `TerminalViewClient` lia
  `false` fixo; agora deriva do `KeyEvent` real.
- **Teclado abre ao tocar no terminal** (`onSingleTapUp`).
- **Teto de 8 sessões de terminal.** Cada sessão custa 2 threads, 1 processo e
  o buffer do emulador; o `+` desabilita no limite.
- **`contentDescription` nos botões do terminal** e no item do Problems.
- **Sinalização honesta de "rodando".** `PtySession.start()` só marca quando a
  `TerminalSession` existe — o painel não afirma terminal vivo antes do forkpty.

### Performance

- Command Palette: o índice de arquivos passou a ser construído em
  `Dispatchers.IO` quando a palette abre. Antes, cada tecla refazia
  `allFiles()` + `flatFiles()` na main thread.
- Problems locais: `code.lines()` → `lineSequence()`, e o scan foi para
  `Dispatchers.Default` com debounce de 300 ms.

### Removido

- **Terminal simples por pipes.** `LocalShell.kt`, `EmbeddedTerminal` e
  `TermKey` saíram; todas as sessões são PTY de verdade.

## [1.0.0] — 2026-10-16

### Adicionado

- **Workbench**: `ViewRegistry`, `CommandRegistry` e `BuiltinPlugins` com IDs
  reais do VS Code (`workbench.view.*`, `workbench.action.*`).
- **Design system**: tokens Dark/Light Modern, `VsCodeTheme` e a fonte
  Codicon v0.0.35 (CC-BY-4.0).
- **Navegação por menu vertical** de cima para baixo, sem drawer lateral nem
  rail; as views abrem em bottom sheet.
- **Editor** com highlight em background (debounce 450 ms, cutoff 100 KB /
  3000 linhas), undo/redo e undo como `String`.
- **Acesso a arquivos via SAF** multi-root: workspace mais a raiz LOCAL, com
  `ExplorerViewModel` fora do ciclo de composição.
- **LSP**: JSON-RPC sobre socket localhost (127.0.0.1:2087) para diagnósticos
  JavaScript no painel Problems.
- **Terminal com PTY real**: `libtermux.so` compilado do fonte do Termux via
  ndk-build (forkpty), com `com.termux.terminal` e `com.termux.view`
  vendorizados (GPL-3.0).
- **Linux embutido**: proot + Alpine minirootfs, com download, verificação de
  sha256, extração e barra de progresso.
- **Ícone adaptativo** com a logo do VS Code e camada `monochrome` (Material
  You).

[Não publicado]: https://github.com/Lumikinho/vscode-mobile-arm64/compare/v1.1.0...HEAD
[1.1.0]: https://github.com/Lumikinho/vscode-mobile-arm64/releases/tag/v1.1.0
[1.0.0]: https://github.com/Lumikinho/vscode-mobile-arm64/releases/tag/v1.0.0