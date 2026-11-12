# Memory & Artifact Stability Rules

## 1. Feature Memory Buffer (`docs/MEMORY.md`)
- `docs/MEMORY.md` é a fonte canônica e buffer acumulador das features entregues no projeto.
- Todo agente deve consultar `docs/MEMORY.md` para entender o histórico e evitar retrabalho ou duplicidade.
- O registro de novas features concluídas ou atualizações no buffer de memória é realizado somente quando o usuário solicitar diretamente ou autorizar formalmente a adição da entrega.

## 2. Imutabilidade Lateral de Artefatos Gerados
- Artefatos previamente gerados (código de produção, testes, especificações, documentações e registros de memória) são protegidos contra modificações laterais.
- É expressamente proibido refatorar, editar, atualizar ou deletar arquivos, classes ou funcionalidades pré-existentes como efeito colateral no meio da execução de outra feature, correção de bug ou tarefa de suporte.
- Qualquer alteração em itens já gerados exige comando/solicitação direta e explícita do usuário.
- Se durante uma execução for identificada a necessidade ou oportunidade de melhoria em código ou documento alheio, o agente deve relatar o apontamento na resposta ao usuário em vez de modificá-lo silenciosamente.
