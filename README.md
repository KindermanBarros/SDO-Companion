# SDO Companion

Aplicativo Android companion para **Silêncio dos Oráculos**. O projeto reúne fichas, campanhas, conteúdo canônico e operações de sessão em uma experiência offline-first para jogadores e Historiadores.

## Sobre o projeto

Durante uma campanha de RPG, fichas, anotações, regras e controles de sessão costumam ficar separados. O SDO Companion centraliza esse fluxo:

- personagens podem existir dentro ou fora de campanhas;
- a mesma conta pode ser Jogador em uma campanha e Historiador em outra;
- fichas permanecem disponíveis sem conexão;
- alterações locais são sincronizadas com o Firebase quando há internet;
- conflitos entre versões local e remota podem ser resolvidos por campo;
- catálogos canônicos ajudam no preenchimento sem impedir conteúdo personalizado.

A administração global é reservada à conta configurada como administradora. As demais permissões pertencem ao contexto de cada campanha.

## Funcionalidades

### Para jogadores

- criação e edição de fichas completas;
- raça, sub-raça, atributos, recursos, conhecimentos e proteções;
- Caminho, Poderes, Magias, Runas e Cinzas;
- inventário, equipamentos, corpo, órgãos, condições e anotações;
- cálculos automáticos com ajustes manuais;
- Modo Sessão para consultas e ações rápidas;
- campanhas opcionais com entrada por código;
- recebimento de conteúdo enviado pelo Historiador.

### Para Historiadores

- campanhas e participantes organizados em um painel;
- consulta e edição autorizada das fichas da campanha;
- aplicação de dano, cura, condições e alterações de recursos;
- bloqueio de fichas;
- biblioteca de conteúdo da campanha;
- entregas de conteúdo para jogadores;
- histórico de operações com autor, alvo, valores e motivo.

### Recursos gerais

- login com Google pelo Firebase Authentication;
- persistência local com Room;
- sincronização com Cloud Firestore;
- resolução de conflitos por campo;
- catálogos pesquisáveis e criação manual livre;
- múltiplos temas, escala de texto e densidade de interface;
- modo local de demonstração quando o Firebase não está configurado.

## Tecnologias

| Área | Tecnologia |
| --- | --- |
| Plataforma | Android 11+ |
| Linguagem | Kotlin |
| Interface | Jetpack Compose e Material 3 |
| Estado | ViewModel, StateFlow e Compose State |
| Assincronismo | Kotlin Coroutines |
| Persistência local | Room |
| Autenticação | Firebase Authentication e Credential Manager |
| Sincronização | Cloud Firestore |
| Serialização | Kotlinx Serialization |
| Build | Gradle 9.7.1 e JDK 25 |
| Integração contínua | GitHub Actions |

## Arquitetura

O aplicativo usa separação inspirada em Clean Architecture. A dependência aponta da interface e da infraestrutura para os contratos e modelos do domínio.

```text
┌────────────────────────────────────────────────────────────┐
│ presentation/                                              │
│ Compose Screens → AppViewModel/AuthViewModel → UI State    │
└───────────────────────────┬────────────────────────────────┘
                            │ usa
┌───────────────────────────▼────────────────────────────────┐
│ domain/                                                    │
│ Modelos → Políticas → Casos de uso → Interfaces Repository │
└───────────────────────────▲────────────────────────────────┘
                            │ implementa
┌───────────────────────────┴────────────────────────────────┐
│ data/                                                      │
│ Room/DAO → Repositórios offline-first → Firebase           │
└────────────────────────────────────────────────────────────┘
```

### Camada de domínio

Local: `app/src/main/java/com/kinderman/sdo/domain`

Contém regras independentes da interface:

- `model/`: personagens, campanhas, recursos, proteções, operações e conflitos;
- `catalog/`: conteúdo canônico, raças, Caminhos, conhecimentos e criação de itens;
- `policy/`: regras de autorização para leitura, edição, bloqueio e exclusão;
- `repository/`: contratos usados pelo restante da aplicação;
- `usecase/`: operações de negócio reutilizáveis.

O domínio não depende de telas Compose ou de detalhes visuais.

### Camada de dados

Local: `app/src/main/java/com/kinderman/sdo/data`

Implementa os contratos do domínio:

- `local/`: banco Room, entidades, DAOs, conversores e migrações;
- `repository/`: repositórios offline-first de fichas, campanhas, perfis e operações;
- `auth/`: autenticação Google e integração com Firebase Authentication.

A gravação principal acontece localmente. A sincronização remota atualiza o Firestore sem transformar a rede em requisito para editar uma ficha.

### Camada de apresentação

Local: `app/src/main/java/com/kinderman/sdo/presentation`

Reúne telas, estado e navegação:

- `login/`: autenticação e entrada no modo demo;
- `dashboard/`: personagens, campanhas e convites;
- `character/`: ficha paginada, catálogos e diálogos de edição;
- `session/`: Modo Sessão e operações rápidas;
- `historian/`: painel e ferramentas do Historiador;
- `settings/`: temas e preferências;
- `sync/`: resolução visual de conflitos;
- `SdoApp.kt`: composição principal e navegação entre superfícies;
- `AppViewModel.kt`: coordenação dos repositórios e estado da aplicação.

### Design system

Local: `app/src/main/java/com/kinderman/sdo/ui`

Concentra cores, tipografia, temas, componentes reutilizáveis, densidade e comportamento responsivo. O guia detalhado está em [DESIGN_GUIDE.md](DESIGN_GUIDE.md).

### Fluxo de dados

1. Uma ação acontece em uma tela Compose.
2. A tela chama o `AppViewModel`.
3. O ViewModel aplica a regra e chama um contrato do domínio.
4. O repositório grava a alteração no Room.
5. A nova versão local é emitida para a interface.
6. Quando permitido, a sincronização envia a alteração ao Firestore.
7. Divergências relevantes geram um conflito para resolução por campo.

### Segurança

As permissões são verificadas em mais de uma camada:

1. políticas no domínio;
2. validações nos repositórios;
3. regras em `firebase/firestore.rules`.

Ocultar um botão não é considerado uma barreira de segurança.

## Estrutura do repositório

```text
SDO-Companion/
├── app/                 Código e recursos do aplicativo Android
├── catalogs/            Catálogos JSON distribuídos com o APK
├── docs/                Auditorias e documentação complementar
├── firebase/            Regras, índices e testes do Firestore
├── gradle/              Wrapper e catálogo de dependências
├── licenses/            Licenças das fontes incluídas no aplicativo
├── tools/               Scripts de geração e validação dos catálogos
├── DESIGN_GUIDE.md      Guia visual do produto
├── firebase.json        Configuração do Firebase CLI
└── README.md            Visão geral e instruções do projeto
```

## Como rodar localmente

Há duas formas de executar:

- **modo demo**, sem credenciais Firebase;
- **modo completo**, conectado ao seu projeto Firebase.

Para conhecer ou desenvolver a interface, comece pelo modo demo.

### 1. Pré-requisitos

Instale:

- [Android Studio](https://developer.android.com/studio);
- JDK 25;
- Android SDK Platform 37;
- um emulador ou dispositivo com Android 11, API 30, ou superior;
- Git.

O wrapper baixa o Gradle 9.7.1 automaticamente. Não é necessário instalar o Gradle globalmente.

Confirme o Java no terminal:

```bash
java -version
```

A saída deve indicar Java 25. Se o terminal usar outra versão, configure `JAVA_HOME` para o JDK 25. No Android Studio, confira também **Settings > Build, Execution, Deployment > Build Tools > Gradle > Gradle JDK**.

### 2. Clonar o repositório

```bash
git clone https://github.com/KindermanBarros/SDO-Companion.git
cd SDO-Companion
```

### 3. Abrir no Android Studio

1. Selecione **Open**.
2. Escolha a pasta `SDO-Companion`.
3. Aguarde o Gradle Sync terminar.
4. Se o Android Studio solicitar componentes do SDK 37, aceite a instalação.
5. Não selecione somente a pasta `app`; abra a raiz do repositório.

### 4. Criar um dispositivo de teste

No Android Studio:

1. Abra **Tools > Device Manager**.
2. Clique em **Create Virtual Device**.
3. Escolha um aparelho.
4. Selecione uma imagem com API 30 ou superior.
5. Para testar login Google, prefira uma imagem que inclua Google Play.
6. Inicie o emulador.

Um aparelho físico também funciona. Ative as opções do desenvolvedor e a depuração USB antes de conectá-lo.

### 5. Rodar em modo demo

O modo demo não exige `google-services.json`.

1. Confirme que não existe `app/google-services.json`.
2. Selecione a configuração `app` na barra superior.
3. Selecione o emulador ou aparelho.
4. Clique em **Run**.
5. Na tela inicial, entre no modo de demonstração.

Pelo terminal, o APK debug pode ser gerado com:

```bash
./gradlew assembleDebug
```

No Windows:

```powershell
.\gradlew.bat assembleDebug
```

O arquivo será criado em:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Para instalar pelo terminal com ADB:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 6. Rodar com Firebase

Use esta etapa somente se precisar testar autenticação e sincronização.

1. Crie ou selecione um projeto no [Firebase Console](https://console.firebase.google.com/).
2. Adicione um aplicativo Android com package `com.kinderman.sdo`.
3. Ative **Authentication > Sign-in method > Google**.
4. Crie o banco em **Firestore Database**.
5. Baixe o `google-services.json`.
6. Salve-o exatamente em `app/google-services.json`.
7. No Firebase, cadastre as impressões SHA exigidas pelo login Google para o certificado usado no build.
8. Publique as regras e os índices presentes no repositório.
9. Sincronize o Gradle e execute novamente o módulo `app`.

O arquivo `google-services.json` é ignorado pelo Git e não deve ser commitado.

Para publicar regras e índices com o Firebase CLI:

```bash
npm install -g firebase-tools
firebase login
firebase use sdo-companion
firebase deploy --only firestore:rules,firestore:indexes
```

Antes de publicar em outro projeto, revise `firebase.json`, as regras e qualquer referência ao projeto de produção. Mais detalhes estão em [docs/firebase-deploy.md](docs/firebase-deploy.md).

### 7. Validar o projeto

Valide os catálogos canônicos:

```bash
python3 tools/generate_catalog.py --check
```

Execute os testes unitários:

```bash
./gradlew testDebugUnitTest
```

Execute o lint:

```bash
./gradlew lintDebug
```

Execute as duas verificações:

```bash
./gradlew testDebugUnitTest lintDebug
```

Os relatórios ficam normalmente em:

```text
app/build/reports/tests/
app/build/reports/lint-results-debug.html
```

### 8. Problemas comuns

#### Gradle está usando o Java errado

Selecione JDK 25 como Gradle JDK no Android Studio e confirme o `JAVA_HOME` no terminal.

#### O SDK 37 não foi encontrado

Abra **Tools > SDK Manager > SDK Platforms** e instale a plataforma Android correspondente ao API 37. Instale também as Build Tools solicitadas pelo Gradle.

#### O login Google falha

Confira:

- o package `com.kinderman.sdo`;
- a posição de `app/google-services.json`;
- o provedor Google ativado;
- as impressões SHA do certificado;
- se o emulador possui Google Play;
- se o arquivo pertence ao projeto Firebase correto.

#### O modo demo não aparece

Remova temporariamente `app/google-services.json`, faça **Build > Clean Project** e execute novamente. O modo demo é oferecido quando o Firebase não está configurado.

#### O aplicativo mantém dados de uma execução anterior

Desinstale o aplicativo do dispositivo ou limpe seus dados:

```bash
adb shell pm clear com.kinderman.sdo
```

Essa operação apaga o banco local daquele dispositivo.

## CI e releases

O workflow em `.github/workflows/sdo-companion-android.yml`:

1. valida os catálogos;
2. configura Java 25 e Gradle 9.7.1;
3. restaura os arquivos protegidos a partir dos secrets;
4. executa lint e testes;
5. monta e verifica o APK assinado;
6. publica o APK como artefato;
7. cria uma release quando o histórico de commits exige nova versão semântica.

Alterações exclusivamente no README não acionam o build Android, pois o workflow usa filtros de caminhos.

## Documentação relacionada

- [Guia visual e componentes](DESIGN_GUIDE.md)
- [Catálogos do aplicativo](catalogs/README.md)
- [Implantação e rollback do Firebase](docs/firebase-deploy.md)
- [Auditoria atual do projeto](docs/CHECKLIST_AUDIT_2026_09.md)
- [Implementação das fases 1 e 2](docs/PHASES_1_2_IMPLEMENTATION.md)

## Status

O SDO Companion está em desenvolvimento ativo. Issues registram bugs, decisões de UX e adequações às regras canônicas de Silêncio dos Oráculos.
