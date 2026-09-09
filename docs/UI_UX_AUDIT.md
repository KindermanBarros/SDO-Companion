# Revisão UI/UX

Implementação baseada nos relatos UI-001 a UI-019. A validação visual em aparelho ainda precisa
ser feita; build, lint e testes não substituem a verificação de layouts.

| Registros | Alteração |
| --- | --- |
| UI-001, UI-002 | Tokens resolvidos pelo tema ativo; superfícies de modais e shapes centralizados |
| UI-003, UI-004 | Seções recolhíveis com estado salvo; auditoria opcional e persistida em Configurações |
| UI-005 | Menor intervalo entre filtros; detalhes do catálogo roláveis |
| UI-006 | Menu de acesso direto a qualquer página da ficha |
| UI-007, UI-017 | Ações “+ CAMPANHA” e “CÓDIGO” |
| UI-008 | Rótulos de abas do Historiador ajustados ao espaço, sem quebrar palavras |
| UI-009 | Remoção do parágrafo explicativo do cabeçalho da biblioteca |
| UI-010 | Estado de salvamento consultável por ícone e toast |
| UI-011 | Atalhos por ícones com descrições acessíveis |
| UI-012 | Estados vazios curtos e sem decoração excessiva; cabeçalhos limitados à largura disponível |
| UI-013 | Tema Aperture White com azul Portal |
| UI-014 | Exclusão online, confirmada, com fichas preservadas |
| UI-015 | Remoção do card A11Y.CONTRACT |
| UI-016 | Seleção de ficha na sessão usando TechPanel |
| UI-018 | “+ Turno”, “+ Cena”, “+ Descanso”, preservando os comandos internos |
| UI-019 | Campos compactos com padding interno menor, sem eliminar intervalos externos |

## Exclusão de campanhas

- Somente responsável ou administrador.
- Ação depende de conexão e campanha já sincronizada.
- Um batch atômico desvincula as fichas e grava DELETED. As fichas não são apagadas.
- Campanha desaparece das listas, não oferece restauração, e convites deixam de ser válidos.
- O tombstone é intencional: preserva referências históricas e impede ressurreição por clientes antigos.
- Mais de 450 fichas exigem operação administrativa separada; o app não executa remoção parcial.
- As novas regras precisam estar publicadas para permitir a desvinculação atômica pela responsável.

## Verificação manual no aparelho

1. Trocar todos os temas com ficha, modal e barras abertos; conferir contraste e ausência de cores anteriores.
2. Conferir largura compacta, teclado aberto e texto ampliado; nenhum controle deve cortar conteúdo.
3. Expandir/recolher seções, navegar entre páginas e voltar; os dados não podem mudar.
4. Alternar auditoria e reiniciar o app; preferência deve persistir.
5. Conferir os ícones usando leitor de tela e consultar o toast de salvamento.
6. Excluir campanha ativa e arquivada; cancelar deve preservar tudo, confirmar deve preservar as fichas.
7. Tentar exclusão offline e como jogador: falhar sem apagar conteúdo.
