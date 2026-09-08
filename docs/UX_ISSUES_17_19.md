# Fundação de UX — issues #17, #18 e #19

Esta entrega prepara a navegação e os estados visuais das três issues abertas após as Fases 1 e 2.
Ela conecta somente ações que já possuem regra e persistência seguras. Auditoria, distribuição de
conteúdo e registro estruturado de usos continuam identificados como integrações de domínio futuras.

## Mapa de navegação

| Entrada | Objetivo | Saída segura |
| --- | --- | --- |
| Central → Sessão | operar uma ficha com pouca navegação | voltar ao painel ou abrir a ficha completa |
| Ficha → Sessão | manter o personagem atual em contexto | voltar ao painel ou abrir a ficha completa |
| Central → Mestre | observar campanhas e alertas | abrir a ficha ou a sessão daquele personagem |
| Central → Tema | ajustar apresentação local | voltar ao painel mantendo a seleção |

## #17 — Modo Sessão

- seletor explícito de personagem quando a entrada ocorre pelo painel;
- snapshot compacto de recursos, proteções, condições, poderes, magias, cinzas e runas;
- controles `−/+` com limites e salvamento no mesmo modelo offline-first da ficha;
- dano em quatro etapas: valor, região, P.L. local, prévia e confirmação;
- cura com prévia e confirmação;
- campanha arquivada sinalizada como somente leitura;
- densidade compacta configurável para telefones menores.

Ainda depende das regras de domínio da issue: histórico estruturado, idempotência e contadores de uso.
Por isso a biblioteca de habilidades é somente leitura nesta fundação.

## #18 — Painel do Historiador

- acesso apenas para administrador, responsável ou membro Historiador;
- agrupamento de fichas por campanha;
- recursos essenciais, estado de sincronização e alertas textuais por personagem;
- atalhos contextuais para Sessão e Ficha;
- biblioteca pesquisável do catálogo local, sem criar cópias no Firebase.

CRUD de biblioteca de campanha, distribuição para jogadores e trilha de auditoria serão conectados
quando os contratos de persistência correspondentes forem implementados.

## #19 — Aparência e acessibilidade

- três temas persistidos localmente: Neon operacional, Alto contraste e Arcano;
- texto padrão ou ampliado aplicado à tipografia global;
- densidade confortável ou compacta aplicada ao Modo Sessão;
- preferência imediata e independente de ficha/campanha/Firebase;
- estados críticos continuam combinando texto, ícone e cor.

## Estados de UX obrigatórios

| Estado | Tratamento |
| --- | --- |
| sem personagens | seletor informa que não há ficha disponível |
| sem campanha de Historiador | painel explica a permissão necessária |
| campanha arquivada | `ARCHIVE.READ_ONLY` e ações mutáveis desabilitadas |
| alteração local | `LOCAL_DELTA` ou `OFFLINE_READY`, sem bloquear a sessão |
| sincronizado | `SYNC_OK` |
| recurso no limite | controle correspondente é desabilitado |
| dano absorvido | prévia mostra resultado zero antes da confirmação |

