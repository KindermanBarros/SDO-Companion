# UI contract - Shared campaign journal
- Status: redesigned references rendered; owner approval pending. [Approved spec](spec.md).
- Source: journal-v2-candidate.pen; viewport 430x932, pt-BR, font scale 1.0, comfortable density.
- Modes: Standard/Ciano industrial and Cybergrunge. Every state below has both variants.
- [Styleguide](../../../DESIGN_GUIDE.md); [Cybergrunge direction](../../interface-cybergrunge.md).
- Runtime reference unavailable: adb devices returned no connected devices. Source inspection is not runtime acceptance.

## Component mapping
| Component / source | Properties reproduced in Pen | Node naming |
|---|---|---|
| SdoTheme.kt typography | Standard Oxanium body 16/23, metadata 12/17, fixed masthead MB Forever Raw 34/44; Cyber Press Start 2P body 11/17, Acidic title 22/29, Drunk Fonts masthead 34/43 | body; metadata; masthead-title; entry-title |
| SdoScreenMasthead / CybergrungeAssets.kt | Standard eyebrow/title/metadata; Cyber cut panel, 62x88 signal graphic, bars and title stack | masthead |
| HudBackground / SdoTheme.kt | Standard 12dp grid, 48dp major grid using active surfaceVariant; Cyber diagonal signal, scanlines, ghost numerals, fractures and translucent blocks | background |
| TechPanel / SdoTheme.kt | Standard surface .96, accent .72 border, topEnd22/bottomStart14 cuts, 16dp inset, 10dp gap | entry-panel |
| CyberGrungePanel / CybergrungeComponents.kt | Actual TechPanel delegate: topEnd14/bottomStart14 cuts (explicit component override), 5dp left rail, 3dp top rail, chrome and contained interference, 18/13dp inset | entry-panel |
| SdoActionButton / SdoControlComponents.kt | Standard topEnd12/bottomStart12; primary filled cyan, secondary outlined; minimum48dp; uppercase monospaced12 | primary-action; secondary-action |
| CyberGrungeAction / CybergrungeComponents.kt | topEnd14/bottomStart14; primary #FF4D70 on #090608; secondary TerminalGreen #789A83 outline; uppercase Press Start2P11, small square marker | primary-action; secondary-action |
| SdoField / HudTextField / CyberGrungeField | Standard cut control and surfaceVariant; Cyber INPUT// label, terminal text, empty-field interference; no fake placeholder content | journal-title; journal-body |
Source files are under app/src/main/java/com/kinderman/sdo/ui/. Pen nodes retain component names and state IDs.
Custom decorative fonts must use the bundled files; fixed title artwork can be rendered directly from them if Pen lacks native font support. Functional text stays readable and traceable.
Guide/source discrepancies: guide's historical grid colors differ from current HudBackground; use active semantic surfaceVariant. TechPanel Cyber cuts are an explicit component override of SdoShapeTokens.panel; preserve that override.
Root CODE_STANDARDS forbids ellipsis, overriding historical guide text allowing it. Whole words, complete content and minimum48dp touch targets apply throughout.
Cybergrunge references depict one static interference phase; geometry/chrome/noise remain visible, motion alone is deferred to runtime.
No design-system exception or new dependencies. Existing palettes and component semantics remain.

## Screen states
Every row has a stable state ID; fixture name is its lowercase ID plus -fixture, scoped to campaign/account/role.
Variants append -standard / -cyber; each design node is a top-level screen-frame. All rows use the source above, screen-frame role, and explicit node IDs/PNG links below. See [reference manifest](visual_references.json).
| State ID | Screen/state | Required regions and controls | Design nodes / PNGs |
|---|---|---|---|
| J01 | Loading | masthead; loading indicator; Back; create hidden | [cyber:E8aRpg](previews-v2/E8aRpg.png) / [standard:U1ZbdM](previews-v2/U1ZbdM.png) |
| J02 | Empty | empty message; Novo registro | [cyber:ZlOol](previews-v2/ZlOol.png) / [standard:Wrw1w](previews-v2/Wrw1w.png) |
| J03 | List | newest-first entries; Novo registro; read actions | [cyber:x1vLp](previews-v2/x1vLp.png) / [standard:WDRRo](previews-v2/WDRRo.png) |
| J04 | Cached offline | offline label; cached list; mutations disabled | [cyber:RxXrK](previews-v2/RxXrK.png) / [standard:H6j6bf](previews-v2/H6j6bf.png) |
| J05 | Uncached offline | unavailable message; Tentar novamente | [cyber:yB2lC](previews-v2/yB2lC.png) / [standard:tL8RE](previews-v2/tL8RE.png) |
| J06 | Fetch error | error message; Tentar novamente | [cyber:rWzEg](previews-v2/rWzEg.png) / [standard:XgeJ6](previews-v2/XgeJ6.png) |
| J07 | Archived list | archive status; entries readable; create disabled | [cyber:gGAGl](previews-v2/gGAGl.png) / [standard:wJpW8](previews-v2/wJpW8.png) |
| J08 | Access revoked | no entries/drafts; Voltar às campanhas | [cyber:NlO4O](previews-v2/NlO4O.png) / [standard:wqlxq](previews-v2/wqlxq.png) |
| J09 | Author detail | full body; author/date; Editar; Excluir | [cyber:nOFCg](previews-v2/nOFCg.png) / [standard:pD2K9](previews-v2/pD2K9.png) |
| J10 | Other member detail | full body; author/date; Back; no mutations | [cyber:G1aVU](previews-v2/G1aVU.png) / [standard:vmshx](previews-v2/vmshx.png) |
| J11 | GM detail | full body; moderation indication; Editar; Excluir | [cyber:R1Xniq](previews-v2/R1Xniq.png) / [standard:DXMpN](previews-v2/DXMpN.png) |
| J12 | Offline detail | offline label; full cached body; writes disabled | [cyber:sM56A](previews-v2/sM56A.png) / [standard:XDqdW](previews-v2/XDqdW.png) |
| J13 | New editor | empty title/body; counters; disabled Save; Back | [cyber:OQfC9](previews-v2/OQfC9.png) / [standard:H9kXf](previews-v2/H9kXf.png) |
| J14 | Edit editor | filled title/body; counters; Salvar alterações | [cyber:s4ADyR](previews-v2/s4ADyR.png) / [standard:ugsEZ](previews-v2/ugsEZ.png) |
| J15 | Validation | required-field message; empty fields; disabled Save | [cyber:xT4gy](previews-v2/xT4gy.png) / [standard:C1KJym](previews-v2/C1KJym.png) |
| J16 | Saving | pending confirmation; fields/Save locked | [cyber:M3iS8](previews-v2/M3iS8.png) / [standard:zKVic](previews-v2/zKVic.png) |
| J17 | Editor disconnected | preserved draft; disabled editing/Save; reconnect status | [cyber:iVies](previews-v2/iVies.png) / [standard:MpPJR](previews-v2/MpPJR.png) |
| J18 | Uncertain save | preserved draft; Verificar e tentar novamente | [cyber:zQjpG](previews-v2/zQjpG.png) / [standard:M3Wcms](previews-v2/M3Wcms.png) |
| J19 | Cross-account conflict | Sua versão; Versão atual; Recarregar; Substituir | [cyber:iAIjT](previews-v2/iAIjT.png) / [standard:LdDeb](previews-v2/LdDeb.png) |
| J20 | Delete confirmation | detail underneath; scrim; Cancelar; Excluir | [cyber:mmtJl](previews-v2/mmtJl.png) / [standard:qnShy](previews-v2/qnShy.png) |
| J21 | Discard confirmation | editor underneath; scrim; Continuar editando; Descartar | [cyber:CseTH](previews-v2/CseTH.png) / [standard:b1NPrO](previews-v2/b1NPrO.png) |
| J22 | Title IME | same new editor; title focused; system-ime; Save reachable | [cyber:ul9b5](previews-v2/ul9b5.png) / [standard:d63mV](previews-v2/d63mV.png) |
| J23 | Body IME | same filled draft; body focused; system-ime; Save reachable | [cyber:UkcUw](previews-v2/UkcUw.png) / [standard:t17BQj](previews-v2/t17BQj.png) |
| J24 | Campaign entry | expanded card; name/state/role/invite; Abrir diário | [cyber:UrBZJ](previews-v2/UrBZJ.png) / [standard:r47m6v](previews-v2/r47m6v.png) |
| J25 | Replace confirmation | comparison underneath; scrim; Cancelar; Substituir | [cyber:Yh4ph](previews-v2/Yh4ph.png) / [standard:BmbEL](previews-v2/BmbEL.png) |
| J26 | Archived detail | archive status; full body; mutations disabled | [cyber:lbqmK](previews-v2/lbqmK.png) / [standard:oNVPs](previews-v2/oNVPs.png) |

## Fixtures and exact behavior
Campaign “Crônicas de Valdora”; GM Bruno; members Ana/Lia; removed member Rui.
Entry A: “A chegada a Valdora”; Ana; 14/11/2026 09:30; “Chegamos a Valdora ao anoitecer. A ponte estava fechada.\n\nDecidimos procurar abrigo na antiga estação antes de seguir para o norte.”
Entry B: “O mapa da estação”; Lia; 13/11/2026 21:10; shown below A. Names and full title reflow without ellipses.
Status copy: “Carregando diário”; “O diário está vazio”; “Offline · somente leitura”; “Campanha arquivada · somente leitura”; “Você não tem mais acesso a este diário”.
Editor labels: “Título”, “Registro”; limits120/20000; count exact draft length. Invalid: “Informe o título e o texto do registro.”
Failure: “Não foi possível confirmar o salvamento. Seu rascunho está preservado.” Retry checks same operation identity before resubmission.
Disconnect: “Sem conexão. Seu rascunho está preservado. Reconecte-se para salvar.” No offline queue or confirmed-success styling.
Conflict: “Este registro foi alterado por outra pessoa.” Full scrollable Sua versão and Versão atual, labeled with last editor; explicit replacement confirmation.
Deletion: “Excluir este registro? Esta ação não pode ser desfeita.” Cancellation preserves the entry.
Discard: “Descartar alterações? Seu rascunho não foi salvo.” Continuing restores the editor unchanged.
Replace: “Substituir a versão atual? A versão comparada será substituída pelo seu texto.” A further revision returns to comparison.
Archived/removed/deleted access follows spec AC-002/008/012; GM moderation preserves original author.
Author removed from membership uses safe display metadata “Ex-participante”, never an account ID.
Same-account sessions converge automatically without conflict dialog. Ordinary remote updates refresh open lists/details.
Loading never simulates progress percentage. List and full-entry areas scroll and preserve position; long text uses whole-word wrapping.

## IME State Coverage
| Input tag | Closed | Open | Dismissed | Viewport/layout | Keyboard behavior | Required visible tags |
|---|---|---|---|---|---|---|
| journal-title | J13/J14 | J22 | J13/J14 | 430x932; resize+scroll | screen-no-toolbar | journal-title; journal-save; journal-back |
| journal-body | J14 | J23 | J14 | 430x932; resize+scroll | screen-no-toolbar | journal-body; journal-save; journal-back |
IME is illustrative OS chrome, not an SDO-themed keyboard. Back dismisses it before leaving the editor.
IME variants omit the nonessential masthead, retain campaign context in the toolbar, and keep the focused field and Save reachable with the full draft retained.
Focus order: Back, title, body, Save. TalkBack labels actions and announces status/errors; no status depends solely on color.
Rotation preserves drafts; logout/campaign changes clear restricted content and do not leak drafts.

## Navigation and verification
Dashboard campaign card > Abrir diário > list > full detail > edit/delete. Novo registro opens the editor.
Confirmed save/deletion returns to list; Back restores the originating campaign context. Unsaved leaving asks for discard.
Representative list frames inspected before expansion; all states exported. [Implementation/test plan](implementation_plan.md); [review board](review-board-v2.png).
Reference approval precedes app implementation per ux-design. No runtime acceptance or pixel-test success is claimed by Pen export.
