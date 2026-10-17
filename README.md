# VSCode Mobile — porte p/ celular ARM64 (Kotlin + Compose + Material 3)

UI refeita como o VS Code moderno (tokens Dark/Light Modern oficiais), adaptada a toque.

## Arquitetura da UI
```
ui/
  VsCodeApp.kt             # fiação: estado + Scaffold + drawer + sheets + dialogs
  theme/
    VsCodeColors.kt        # 50 tokens (DarkModern/LightModern exatos) + LocalVsCodeColors
    VsCodeTheme.kt         # M3 ColorScheme mapeado, Shapes 4–6dp, Spacing, estilo mono
    Codicon.kt             # fonte Codicons + 43 ícones + previews
  components/
    AdaptiveBottomBar.kt   # navegação/edição/terminal (AnimatedContent 200ms)
    TopBar.kt              # menu, título, busca
    EditorTabs.kt          # LazyRow, fechar 40×40dp, dot de dirty, borda-topo accent
    Breadcrumb.kt          # recolhível
    StatusBar.kt           # 24dp neutra com borda
    CommandPaletteSheet.kt # bottom sheet + fuzzy por subsequência
    ExplorerDrawer.kt      # árvore, Codicons por tipo, seleção list.*
    ProblemsSheet.kt       # problems/output/terminal p/ bottom sheet
    EditorSurface.kt       # linha atual, indent guides, brackets, gutter, pinça
```
Lógica intacta: `editor/SyntaxHighlighter`, `files/FileUtils`, `termux/*`.

## Workbench (migração do VS Code real)
Núcleo espelhado do `microsoft/vscode` (clone sparse em `vscode-upstream/`):
- `ui/workbench/ViewRegistry.kt` — containers/views/welcome (`views.ts`)
- `ui/workbench/CommandRegistry.kt` — comandos com IDs reais (`Action2`)
- `ui/workbench/QuickAccess.kt` — providers por prefixo (`>`, `''`)
- `ui/workbench/BuiltinPlugins.kt` — 5 containers, 10 views, 19 comandos
  `workbench.*` (explorer, search, scm, debug, extensions, terminal…)
- Bottom bar com 5 destinos vindos do registro; paleta via providers

## LSP — diagnósticos JavaScript (L1)
- `lsp/JsonRpc.kt` — JSON-RPC 2.0 (`Content-Length` em bytes, correlação de ids)
- `lsp/LspClient.kt` — initialize/didOpen/didChange/didClose, `publishDiagnostics`
- `ui/state/LanguageServerManager.kt` — conexão por linguagem via socket
  localhost (JS → porta 2087), debounce 500ms, retry com backoff de 60s
- Servidor roda onde há runtime (Termux); sem servidor = Snackbar (nunca
  Toast de cancelamento). Requer permissão `INTERNET`
- Teste manual no Termux: `pkg install nodejs` →
  `npm i -g typescript-language-server` →
  `typescript-language-server --socket 2087` → abra um `.js` no app

## Arquivos locais (SAF)
- Paleta: `> File: Open…` (arquivo avulso) e `> File: Open Folder…`
- A pasta **anexa no Explorer junto do workspace** (multi-root, como
  `Add Folder to Workspace`): raiz WORKSPACE + raiz LOCAL com badge, cada uma
  com botões próprios de novo arquivo/pasta; ✕ na raiz LOCAL desanexa
- Permissão persistida e restaurada; salvar escreve de volta na Uri; "Abrir no
  Termux" usa o workspace (SAF não é visível para outro app)
- Se o seletor não abrir: Toast de aviso + tentativa via intent crua
- Diagnóstico de listagem: Toast com o erro exato ou "pasta vazia, toque em ↻"

## Terminal com PTY de verdade
Aba TERMINAL com sessões em abas (`Terminal 1, 2…`, `+`), todas reais:
- `libtermux.so` compilado do fonte do Termux via ndk-build (forkpty real)
- Código `com.termux.terminal` + `com.termux.view` vendorizado (GPL-3.0, ver
  LICENSE); `TerminalView` embutido via `AndroidView`
- `PtySession` + `TerminalScreen`: vim, prompts, Ctrl+C, setas, Tab do shell,
  seleção por long-press, paleta do tema aplicada ao emulador
- Barra de teclas embaixo (Esc, Tab, Ctrl grudento com C/D/Z, setas) escrevendo
  bytes de controle direto na PTY; `A−`/`A+` no modo terminal da bottom bar
- Teto de 8 sessões — cada uma custa 2 threads, 1 processo e o buffer do
  emulador
- Alpine também abre em PTY (apk interativo funciona)
- O PTY nasce no primeiro `updateSize` da view (grade 0×0 quebraria o forkpty)

## Linux embutido (proot + Alpine)
- `linux/LinuxManager.kt` — state machine, download com progresso, sha256
  oficial, extração tar.gz com symlinks, `resolv.conf`, `PROOT_TMPDIR`
- Por que proot: binários do Termux têm o prefixo `/data/data/com.termux`
  embutido e não executam sob outro pacote; o proot traduz os caminhos
- Paleta `> Linux: Setup Alpine` (baixa ~10MB uma vez) e `> Linux: Open Shell`;
  sessões Alpine nas abas do terminal (`apk` disponível)
- Pendente de validação no aparelho: execução do binário proot de terceiros
  (antigo — pode falhar em Android novo), ptrace em algumas ROMs

## Performance
- **Editor virtualizado**: gutter e camada de realces só compoem a faixa
  visível + 8 linhas de overscan. Sem isso eram N `Text` + N `Box` remontados a
  cada tecla, com N = número de linhas (até 3000 pelo cutoff de highlight)
- Highlight em `Dispatchers.Default` (debounce 450ms); arquivo grande
  (>100KB ou >3000 linhas) vira texto puro, como `editor.largeFileOptimizations`
- Problems locais em `lineSequence()` + `Dispatchers.Default` (debounce 300ms)
- Índice da Command Palette construído em IO quando a palette abre — digitar
  não refaz mais a varredura do workspace
- `lineColAt`/`cursorLineCol` sem `text.take()`: varrem até o cursor em vez de
  copiar o prefixo do documento
- SAF pela via `rawTreeList` (1 query por diretório), não `DocumentFile`
  (1 query por arquivo)
- undo como `String` (cap 50); I/O em `Dispatchers.IO` com guarda de geração;
  release com minify + shrink

## Temas
Dark Modern (`#1F1F1F`/`#181818`/borda `#2B2B2B`/accent `#0078D4`),
Light Modern, Monokai e Dracula (base escura). Status bar neutra com borda —
azul só em debugging/remoto, como no desktop.

## Ícones
Codicons (microsoft/vscode-codicons, **CC-BY-4.0**), `res/font/codicon.ttf` v0.0.35.
Ícone do app derivado do logo do VS Code (Microsoft); adaptativo com camada
`monochrome` (Material You: ativa em Papel de parede e estilo > Ícones temáticos).

## Build
```powershell
cd vscode-mobile-arm64
.\gradlew :app:assembleDebug
# APK em: app\build\outputs\apk\debug\app-debug.apk
```
Pré-requisitos: JDK 17+, Android SDK (`platforms;android-34`) e **NDK
28.2.13676358** (o `libtermux.so` é compilado nativamente por ndk-build).
Wrapper Gradle 9.7.1 incluso. ARM64 (`arm64-v8a`), minSdk 26.

### CI
`.github/workflows/build.yml` roda em todo push/PR na `main`:
JDK 17 (Temurin) → SDK + NDK 28.2.13676358 → `./gradlew :app:assembleDebug`,
com cache de Gradle e do build nativo, e upload do APK como artefato
(`vscode-mobile-arm64-debug`, 30 dias).

Detalhe: o input `packages` do `android-actions/setup-android@v3` tem default
`tools platform-tools`, e o pacote `tools` foi removido do repositório do
Google — o job precisa sobrescrever para `platform-tools`.

## Como testar
1. `adb install app\build\outputs\apk\debug\app-debug.apk` num ARM64 (Android 8+)
2. Hamburger abre o menu vertical (destinos + Terminal/Paleta/Pasta/Tema); cada
   view abre no bottom sheet; bottom bar continua como atalho rápido
3. Abra um arquivo, toque no editor (teclado abre → barra vira teclas, status some)
4. Pinça no editor muda a fonte (10–28sp); linha do cursor e brackets destacam
5. Troque de tema (paleta `> Preferences: Color Theme`); confira claro/escuro
6. Previews no Android Studio: componentes têm previews 360dp claro/escuro (+tablet)
7. Terminal: aba TERMINAL com prompt real; `+` cria sessão (até 8);
   "Termux ↗" abre o app externo (F-Droid)
8. Abra o Problems e toque num item — deve abrir o arquivo na linha

## Pendências conhecidas
- Views Search/Git/Run/Ext ainda usam a `SideBar` legada (só o Explorer migrou)
- Bracket matching ingênuo (ignora strings); quebra de linha longa desalinha gutter
- Pinça pode disputar gesto com seleção de texto em alguns aparelhos
- LSP só faz diagnósticos (L1): hover e completion ainda não implementados
- `WindowInsets.isImeVisible` exige opt-in `ExperimentalLayoutApi` (única exceção a APIs estáveis)
