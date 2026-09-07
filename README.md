# SDO Companion

Aplicativo Android offline-first para jogadores e mestre da campanha. A interface
combina Cyberpunk, Sci-Fi HUD e Acid Graphics em uma FUI operacional com grid
técnico, telemetria e geometria neo-brutalista.

As regras visuais, tokens e componentes estão documentados no
[`DESIGN_GUIDE.md`](DESIGN_GUIDE.md).

## MVP

- login único com Google via Firebase Authentication e Credential Manager;
- perfil Jogador/Mestre carregado do Firestore;
- ficha baseada em `90 - Modelos/Modelo de Ficha.md`;
- identidade, recursos, atributos, conhecimentos, proteções, caminho, história e notas;
- persistência local com Room;
- sincronização com Cloud Firestore;
- regras: jogador acessa a própria ficha; mestre acessa todas;
- CI com lint, testes e APK de release assinado como artifact no GitHub Actions.

## Configuração Firebase

1. Crie um projeto no Firebase e um app Android com package `com.kinderman.sdo`.
2. Ative Authentication > Google e Cloud Firestore.
3. Baixe `google-services.json` em `app/google-services.json` (o arquivo é ignorado pelo Git).
4. Publique `firebase/firestore.rules` e `firebase/firestore.indexes.json`.
5. Crie `users/{uid}` com `role: "MASTER"` apenas para a conta da mestre. Contas comuns devem usar
   `PLAYER`.
6. No GitHub, salve o JSON puro ou em Base64 no secret `GOOGLE_SERVICES_JSON`.
7. Configure os secrets de assinatura `SDO_KEYSTORE_BASE64`,
   `SDO_KEYSTORE_PASSWORD`, `SDO_KEY_ALIAS` e `SDO_KEY_PASSWORD`.

O keystore de release é exclusivo do SDO Companion e nunca deve ser commitado.

> Sem configuração Firebase, o app oferece um modo local de demonstração. Em builds configurados, a
> role vem exclusivamente de `users/{uid}.role`; as regras impedem que um jogador se promova.

## Executar

Abra a raiz do repositório no Android Studio, sincronize o Gradle e rode o módulo `app`. Sem
`google-services.json`, o app continua utilizável localmente com Room.

## Próximas fatias

1. edição completa de poderes, inventário, corpo, órgãos, magias e condições;
2. campanhas e convites por código;
3. conflitos de edição e sincronização em tempo real;
4. testes de DAO, regras do Firestore e Compose UI.
