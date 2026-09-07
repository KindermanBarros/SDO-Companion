# SDO Companion

Aplicativo Android offline-first para jogadores e mestre da campanha. A interface
combina Cyberpunk, Sci-Fi HUD e Acid Graphics em uma FUI operacional com grid
técnico, telemetria e geometria neo-brutalista.

As regras visuais, tokens e componentes estão documentados no
[`DESIGN_GUIDE.md`](DESIGN_GUIDE.md).

## Funcionalidades

- login único com Google via Firebase Authentication e Credential Manager;
- perfil Jogador/Mestre carregado do Firestore;
- ficha baseada no documento canônico `90 - Modelos/Modelo de Ficha.md` do repositório de SDO;
- todos os campos da ficha: identidade, recursos, traços, atributos, Conhecimentos Básicos e
  Especiais, Proteções, Caminho, Poderes, inventário, corpo, órgãos, magia e condições;
- navegação paginada em seis módulos, com abas, gesto horizontal e listas renderizadas sob demanda;
- tipografia empacotada: MB Forever Raw para assinatura metal e Oxanium para a interface HUD;
- persistência local com Room;
- sincronização com Cloud Firestore;
- loadings cibernéticos distintos para autenticação e sincronização de personagens;
- exclusão offline-first com sincronização da remoção;
- jogador edita as próprias fichas e pode ativar um bloqueio pessoal para impedir a própria exclusão;
- a Mestre/Historiador acessa e edita todas as fichas, aplica/remove o bloqueio de historiador e pode excluir qualquer personagem;
- bloqueios pessoais podem ser removidos pelo dono; bloqueios de historiador somente pelo historiador;
- CI com lint, testes e APK de release assinado como artifact no GitHub Actions.

## Arquitetura

O código segue Clean Architecture em camadas e separa regras de negócio de Android/Firebase:

```text
domain/
  model/       modelos canônicos da ficha
  policy/      autorização de leitura, edição, bloqueio e exclusão
  repository/  contratos de dados e autenticação
  usecase/     operações da aplicação
data/
  auth/        Google Sign-In e perfil Firebase
  local/       Room, DAO, conversores e migrações
  repository/  sincronização offline-first com Firestore
presentation/
  login/ dashboard/ character/  telas e estado de UI
ui/            tokens e componentes do design system
```

As telas dependem dos contratos do domínio. Regras de autorização são aplicadas no domínio,
novamente no repositório e, como última barreira, em `firebase/firestore.rules`.

## Configuração Firebase

1. Crie um projeto no Firebase e um app Android com package `com.kinderman.sdo`.
2. Ative Authentication > Google e Cloud Firestore.
3. Baixe `google-services.json` em `app/google-services.json` (o arquivo é ignorado pelo Git).
4. Publique `firebase/firestore.rules` e `firebase/firestore.indexes.json`. O primeiro login
   verificado de `kindbarros@gmail.com` cria ou
   corrige automaticamente o perfil para `role: "MASTER"`. As outras contas recebem `PLAYER`.
5. No GitHub, salve o JSON puro ou em Base64 no secret `GOOGLE_SERVICES_JSON`.
6. Configure os secrets de assinatura `SDO_KEYSTORE_BASE64`,
   `SDO_KEYSTORE_PASSWORD`, `SDO_KEY_ALIAS` e `SDO_KEY_PASSWORD`.

O keystore de release é exclusivo do SDO Companion e nunca deve ser commitado.

As licenças das fontes distribuídas no APK estão em [`licenses/fonts`](licenses/fonts).

> Sem configuração Firebase, o app oferece um modo local de demonstração. A autorização remota usa
> o e-mail verificado da conta bootstrap e `users/{uid}.role`; jogadores não podem se promover.

## Executar

Abra a raiz do repositório no Android Studio, sincronize o Gradle e rode o módulo `app`. Sem
`google-services.json`, o app continua utilizável localmente com Room.

## Manutenção de dependências

O Dependabot verifica Gradle e GitHub Actions semanalmente. Atualizações minor/patch são agrupadas;
majors permanecem isoladas para revisão e devem passar por lint, testes e build assinado antes do merge.

## Próximas fatias

1. campanhas e convites por código;
2. resolução explícita de conflitos de edição;
3. testes instrumentados do Room, regras do Firestore e Compose UI.
