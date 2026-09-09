# SDO Companion

Companion Android para **Silêncio dos Oráculos**, criado para centralizar fichas, campanhas e operações de sessão em um único aplicativo.

O projeto atende jogadores e Historiadores sem exigir que uma conta pertença previamente a uma campanha. Cada pessoa pode manter fichas independentes e assumir papéis diferentes em campanhas diferentes.

## O que o aplicativo resolve

Durante uma campanha de RPG, informações importantes costumam ficar espalhadas entre fichas, anotações, documentos de regras e controles manuais. O SDO Companion reúne esse fluxo:

- criação e edição de personagens;
- consulta aos catálogos canônicos do sistema;
- organização de campanhas e participantes;
- acesso rápido aos dados usados durante uma sessão;
- ferramentas de acompanhamento para o Historiador;
- persistência local e sincronização entre dispositivos.

O aplicativo segue uma abordagem **offline-first**: alterações são salvas primeiro no aparelho com Room e sincronizadas com o Cloud Firestore quando há conexão.

## Experiência por perfil

### Jogador

O jogador pode:

- criar e manter fichas com ou sem campanha;
- preencher raça, sub-raça, atributos, recursos, conhecimentos e proteções;
- administrar Caminho, Poderes, Magias, Runas e Cinzas;
- registrar inventário, equipamentos, corpo, órgãos, condições e anotações;
- usar o Modo Sessão para consultar recursos e aplicar ações rápidas;
- receber conteúdo enviado pelo Historiador;
- resolver conflitos quando a versão local e a versão online mudaram.

### Historiador

Dentro das campanhas em que possui esse papel, o Historiador pode:

- acompanhar personagens e participantes;
- abrir fichas ou o Modo Sessão de cada personagem;
- aplicar dano, cura, condições e alterações de recursos;
- bloquear fichas quando necessário;
- consultar e distribuir conteúdo da biblioteca da campanha;
- acompanhar operações registradas durante a sessão.

### Administração

A administração global do aplicativo é reservada à conta configurada como administradora. Contas comuns não escolhem um tipo global: permissões de jogador e Historiador pertencem ao contexto de cada campanha.

## Principais recursos

- autenticação com Google pelo Firebase Authentication;
- fichas completas baseadas nas regras canônicas de SDO;
- campanhas opcionais com convite por código;
- catálogos pesquisáveis de Conhecimentos, Poderes, Magias, Runas e Cinzas;
- criação manual livre além dos conteúdos prontos;
- cálculos automáticos de recursos e proteções com ajustes manuais;
- inventário e construção de armas, armaduras e acessórios;
- Modo Sessão com ações rápidas e confirmação de dano ou cura;
- Painel do Historiador organizado por campanha;
- histórico de operações com autor, alvo, valores e motivo;
- sincronização offline-first com resolução de conflitos por campo;
- temas, densidade e preferências visuais armazenados no aparelho;
- modo de demonstração local quando o Firebase não está configurado.

## Tecnologias

| Área | Tecnologia |
| --- | --- |
| Plataforma | Android |
| Linguagem | Kotlin |
| Interface | Jetpack Compose e Material 3 |
| Persistência local | Room |
| Autenticação | Firebase Authentication e Credential Manager |
| Sincronização | Cloud Firestore |
| Concorrência | Kotlin Coroutines |
| Serialização | Kotlinx Serialization |
| Build e CI | Gradle e GitHub Actions |

## Organização do projeto

```text
app/src/main/java/com/kinderman/sdo/
├── domain/         Regras, modelos, políticas, catálogos e contratos
├── data/           Room, Firebase e repositórios offline-first
├── presentation/   Login, painel, ficha, sessão, Historiador e ajustes
└── ui/             Temas, componentes e comportamento responsivo

catalogs/           Conteúdo canônico distribuído com o aplicativo
firebase/           Regras, índices e testes do Cloud Firestore
docs/               Auditorias e documentação técnica complementar
licenses/           Licenças das fontes incluídas no APK
```

A autorização é verificada nas regras de domínio, nos repositórios e nas regras do Firestore. A interface não é tratada como barreira de segurança.

## Como executar

### Requisitos

- Android Studio compatível com o projeto;
- JDK 25;
- Android SDK 37;
- dispositivo ou emulador com Android 11 (API 30) ou superior.

### Execução local

1. Clone o repositório.
2. Abra a raiz no Android Studio.
3. Aguarde a sincronização do Gradle.
4. Execute o módulo `app`.

Sem um arquivo `google-services.json`, o aplicativo continua disponível em modo local de demonstração.

### Firebase opcional

Para habilitar autenticação e sincronização:

1. Crie um projeto no Firebase.
2. Registre um aplicativo Android com o package `com.kinderman.sdo`.
3. Ative Google em **Authentication** e crie o **Cloud Firestore**.
4. Coloque o arquivo `google-services.json` em `app/google-services.json`.
5. Publique `firebase/firestore.rules` e `firebase/firestore.indexes.json`.

As orientações de implantação estão em [docs/firebase-deploy.md](docs/firebase-deploy.md).

## Verificações

Para executar as verificações locais:

```bash
./gradlew testDebugUnitTest lintDebug
```

O GitHub Actions executa lint, testes e geração do APK. A distribuição assinada exige os secrets de Firebase e assinatura configurados no repositório.

## Documentação relacionada

- [Guia visual e componentes](DESIGN_GUIDE.md)
- [Catálogos do aplicativo](catalogs/README.md)
- [Implantação do Firebase](docs/firebase-deploy.md)
- [Auditoria atual do projeto](docs/CHECKLIST_AUDIT_2026_09.md)

## Status

O SDO Companion está em desenvolvimento ativo. Issues e pull requests são usadas para registrar bugs, evoluções de UX e adequações às regras canônicas de Silêncio dos Oráculos.
