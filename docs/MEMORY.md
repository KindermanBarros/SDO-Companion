# Project Memory & Task History (`MEMORY.md`)

Este arquivo único (`docs/MEMORY.md`) centraliza todo o histórico de tarefas, estados em andamento (`InProgress` / `InDevReview`), entregas concluídas (`Done`) e decisões persistentes do SDO Companion.
**Regra estrita**: Nunca criar arquivos separados em `.docs/HistoryRecords/`, `.docs/OngoingTasks/` ou `.docs/CompletedTasks/`. Toda atualização de histórico ou resumo de tarefa vai exclusivamente neste arquivo.

---

## Tasks em Andamento / Revisão (`InProgress` / `InDevReview`)

| ID / Task | Estado | Data (America/La_Paz) | Resumo, Entregas & Evidências |
|---|---|---|---|
| **BACKLOG-001 / CAMPAIGN-PLAN-001** — Diário compartilhado da campanha | `InDevReview` | 2026-11-14 08:37 | Domínio/política (`CampaignJournalEntry`, `CampaignJournalRepository`), repositório Firestore (`campaignJournal`), regras (`firestore.rules`), `JournalViewModel`, `JournalScreen` (Standard azul/ciano + Cybergrunge + IME + conflito/offline/delete/discard), navegação `SdoApp`/`DashboardScreen` e testes. Passaram: `:app:assembleDebug`, `:app:compileDebugAndroidTestKotlin`, `:app:testDebugUnitTest` (257/257), `:app:lintDebug`, Kover `59.71%`, `test:rules` (30/30 no Firestore Emulator), `JournalJourneyTest` (2/2 no emulador API 37 após Espresso `3.7.0` e fix `matchParentSize()` em `SdoScreenMasthead`). Limitação: `ktlintCheck`/`detekt` com pendências de baseline + estilo nos novos arquivos. Ver [change record](changes/2026-11-14-shared-campaign-journal.md). |
| **Correção de contraste na engrenagem inicial** | `InDevReview` | 2026-11-14 01:50 | Corrigido `contentColor`/`tint` do botão de Configurações na tela inicial no modo Standard ([StandardControls.kt](../app/src/main/java/com/kinderman/sdo/ui/standard/components/StandardControls.kt), [DashboardScreen.kt](../app/src/main/java/com/kinderman/sdo/presentation/dashboard/DashboardScreen.kt)), preservando Cybergrunge. Commit `f5e0ba2`, `:app:assembleDebug` PASS. |
| **Consolidação `Memory.md` & Regras** | `InDevReview` | 2026-11-14 10:40 | Consolidados histórico e estados de tarefa em `docs/MEMORY.md`; removidos arquivos separados em `.docs/HistoryRecords/`, `.docs/OngoingTasks/` e `.docs/CompletedTasks/`; atualizados `AGENTS.md`, `.docs/HISTORY.md`, `.docs/TASK_PLANNING.md`, regras e `validate-harness.ps1`. |
| **Sync automático ao abrir Painel do Mestre** | `InDevReview` | 2026-11-14 11:29 | Adicionado `LaunchedEffect(surface, appSession?.uid)` em [SdoApp.kt](../app/src/main/java/com/kinderman/sdo/presentation/SdoApp.kt) chamando `appViewModel.sync()` toda vez que `AppSurface.HISTORIAN` é aberto, garantindo fichas dos jogadores atualizadas. |

---

## Tasks Concluídas (`Done`)

| Data | Task | Resumo & Evidências |
|---|---|---|
| 2026-11-12 | **Setup & Baseline do Harness** | Criados `AGENTS.md`, `.docs/*` e inventário baseline (21 docs validados). |
| 2026-11-12 | **Alinhamento do Harness** | Alinhados 10 workflows, 4 perfis, 3 gates e `project-contract.md`; removidos inputs de setup; 9 casos contratuais PASS. |
| 2026-11-12 | **Remoção de Gates Obsoletos** | Removidos scripts/gates inexistentes; mantidos Gradle/native checks; 10 casos estruturais + 4 CI PASS. |
| 2026-11-12 | **Builds Autenticados em CI** | Removidos artefatos demo; habilitados APKs debug assinados e verificados (CI run `43355100969`). |
| 2026-11-13 | **Compact UI Polish (PR #142)** | UI compacta Standard/Cybergrunge sem reticências, quebra por palavra inteira, ação rápida do Mestre e ajustes de sessão (`53/53` Android, `248` JVM, `:app:lintDebug` PASS). Aceite explícito em 2026-11-13. |
| 2026-11-13 | **Limpeza de Workflow & `.scratch/`** | Criada política `.scratch/`, pre-commit hook, backlog vivo e agente `laziest_senior_developer`; reconciliadas worktrees (`376198d`, PR #142 rebased). Aceite explícito em 2026-11-13. |
| 2026-11-14 | **Publicação v0.51.1** | Publicado release v0.51.1 a partir de `6d85b83` via CI assinado (run `43540829981`). Aceite explícito em 2026-11-14. |
| 2026-11-14 | **Otimização Lean de Workflows & Docs** | Criada branch `feature/lean-workflows-and-tools`; compactados 73 arquivos do harness (`~6.250` linhas removidas); consolidado `docs/changes/` para 1 arquivo `.md` por mudança (`d4bf5a7`, `9aa3a6b`); `validate-harness.ps1` + 15 testes de contrato/hook PASS. Aceite explícito em 2026-11-14. |

---

## Contexto Acumulado & Decisões Persistentes

- **Arquivo único por mudança**: Especificações/decisões/planos de feature ficam em um único `docs/changes/YYYY-MM-DD-feature-name.md` (assets binários em `docs/changes/assets/<slug>/`).
- **Memória e histórico únicos**: Todo histórico de entregas e resumo de tarefas fica exclusivamente em `docs/MEMORY.md` (proibido criar arquivos em `.docs/HistoryRecords/`, `.docs/OngoingTasks/` ou `.docs/CompletedTasks/`).
- **Android 17 (API 37) Espresso**: Manter `androidx.test:runner:1.7.0` e `androidx.test.espresso:espresso-core:3.7.0` em `app/build.gradle.kts` para evitar `NoSuchMethodException: InputManager.getInstance`.
- **Cybergrunge Masthead**: `CyberGrungeInterference` dentro de `SdoScreenMasthead` deve usar `Modifier.matchParentSize()` para não expandir `Canvas(Modifier.fillMaxSize())` sobre toda a tela.
