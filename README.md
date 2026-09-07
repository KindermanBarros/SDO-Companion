# SDO Companion

Aplicativo Android offline-first para jogadores e mestre da campanha. A interface usa papel, tinta, vinho e dourado para aproximar a ficha digital da linguagem editorial de uma ficha criada no Canva, sem sacrificar acessibilidade ou adaptação a telas pequenas.

## MVP

- login e cadastro por e-mail/senha com Firebase Authentication;
- perfil Jogador/Mestre carregado do Firestore;
- ficha baseada em `90 - Modelos/Modelo de Ficha.md`;
- identidade, recursos, atributos, conhecimentos, proteções, caminho, história e notas;
- persistência local com Room;
- sincronização com Cloud Firestore;
- autenticação por e-mail/senha preparada em `AuthRepository`;
- regras: jogador acessa a própria ficha; mestre acessa todas;
- CI com lint, testes, APK de debug e artifact no GitHub Actions.

## Configuração Firebase

1. Crie um projeto no Firebase e um app Android com package `com.kinderman.sdo`.
2. Ative Authentication > Email/password e Cloud Firestore.
3. Baixe `google-services.json` em `app/google-services.json` (o arquivo é ignorado pelo Git).
4. Publique `firebase/firestore.rules` e `firebase/firestore.indexes.json`.
5. Crie `users/{uid}` com `role: "MASTER"` apenas para a conta da mestre. Contas comuns devem usar `PLAYER`.
6. No GitHub, salve o conteúdo base64 do JSON no secret `GOOGLE_SERVICES_JSON`.

> Sem configuração Firebase, o app oferece um modo local de demonstração. Em builds configurados, a role vem exclusivamente de `users/{uid}.role`; as regras impedem que um jogador se promova.

## Executar

Abra a raiz do repositório no Android Studio, sincronize o Gradle e rode o módulo `app`. Sem `google-services.json`, o app continua utilizável localmente com Room.

## Próximas fatias

1. edição completa de poderes, inventário, corpo, órgãos, magias e condições;
2. campanhas e convites por código;
3. conflitos de edição e sincronização em tempo real;
4. testes de DAO, regras do Firestore e Compose UI.
