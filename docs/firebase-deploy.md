# Deploy e rollback das regras do Firebase

As regras e os índices do Firestore são validados no emulador antes de qualquer publicação.

## Publicação automática

- Pull requests apenas validam as regras.
- Alterações em `firebase/**`, `firebase.json` ou no workflow publicadas em `main` são validadas e enviadas ao projeto `sdo-companion`.
- Cada GitHub Release republica a configuração contida na tag da versão, mantendo as regras alinhadas à versão do app.
- Cada execução guarda por 90 dias um artefato com `firebase.json`, regras e índices validados.

O ambiente `firebase-production` usa, na ordem, o primeiro secret disponível entre:

1. `FIREBASE_SERVICE_ACCOUNT_SDO_COMPANION`
2. `FIREBASE_SERVICE_ACCOUNT`
3. `GOOGLE_APPLICATION_CREDENTIALS_JSON`

O valor deve ser o JSON integral de uma conta de serviço autorizada a publicar regras e índices no projeto.

## Rollback

1. Abra **Actions > Firebase rules deploy > Run workflow**.
2. Informe em `ref` a tag ou o SHA que contém a configuração estável, por exemplo `v0.24.0`.
3. Execute o workflow.
4. Confira no resumo da execução o projeto, a referência e o commit publicados.

O rollback restaura as regras e a declaração de índices daquela referência. Índices adicionais já construídos não são excluídos automaticamente, evitando remoções destrutivas durante uma recuperação.
