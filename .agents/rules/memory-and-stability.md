# Memory & Artifact Stability Rules

## 1. Único Arquivo de Memória e Histórico (`docs/MEMORY.md`)
- `docs/MEMORY.md` é o **único** arquivo canônico de histórico, resumos de tarefas (`InProgress` / `InDevReview` / `Done`) e contexto acumulado do projeto.
- **Proibição de arquivos separados**: É expressamente proibido criar arquivos separados de histórico ou estado de tarefa em `.docs/HistoryRecords/`, `.docs/OngoingTasks/` ou `.docs/CompletedTasks/`.
- Todo agente deve consultar `docs/MEMORY.md` ao iniciar e atualizar o resumo da tarefa correspondente em `docs/MEMORY.md` ao entregar (`InDevReview`) ou concluir (`Done`).

## 2. Imutabilidade Lateral de Artefatos Gerados
- Artefatos previamente gerados (código de produção, testes, especificações e documentações alheias à tarefa) são protegidos contra modificações laterais.
- É proibido refatorar, editar ou deletar código/documentação alheia como efeito colateral de outra execução; relate apontamentos ao usuário em vez de alterar silenciosamente.
