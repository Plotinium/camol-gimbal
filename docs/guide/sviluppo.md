# Compilare e provare Camol Gimbal

I comandi si lanciano in PowerShell dentro la cartella del progetto. Serve Java 21, già presente sul
PC (verificato il 2026-11-26). La prima volta scaricano ciò che manca (NeoForge, Camol, Create,
FTB Chunks) dai loro archivi ufficiali.

| Comando | Cosa fa | Cosa tocca |
|---|---|---|
| `.\gradlew.bat build` | Crea il file della mod in `build\libs\` | solo la cartella `build\` |
| `.\gradlew.bat runGameTestServer` | Lancia le prove automatiche e si chiude | un mondo di prova in `run\gametest\`, rifatto ogni volta |
| `.\gradlew.bat runClient` | Avvia il gioco con la mod, Camol, Create e FTB Chunks | i mondi in `run\`, separati dalle tue istanze |
| `.\gradlew.bat visualCheck` | Fa la verifica visiva: fotografa menu, etichetta e anteprima | solo la cartella `build\visual\` |

Per provare la mod in ATM10, copia il file `camolgimbal-1.21.1-<versione>.jar` da `build\libs\`
nella cartella `mods` dell'istanza. Per un server dedicato il file va sia nel server sia nel client.

## Verifica visiva

`.\gradlew.bat visualCheck` apre da solo una finestra di Minecraft, crea un mondo di prova piatto, mette
alcuni blocchi con una camo, ti dà un Gimbal, apre una schermata dopo l'altra, salva una foto di
ciascuna e chiude il gioco. Dura circa tre minuti (misurato il 2026-11-26): mentre la finestra è in
primo piano non usare mouse e tastiera, o le foto cambiano. Le foto sono a tre grandezze dello schermo
(i file finiscono in `-427`, `-640` e `-1280`) e stanno in `build\visual\screenshots\`; ogni esecuzione le
cancella e le rifà. Per averle in inglese si aggiunge `-PvisualLanguage=en_us`. Alla fine il comando
scrive `visual check: ok`; se qualcosa non torna scrive cosa, e il registro è
`build\visual\logs\latest.log`. Se il gioco si blocca si chiude da solo dopo sei minuti. Non tocca
`run\` né le tue istanze, e non fa parte del file della mod.

La lista di cosa controllare in gioco è in [prove.md](prove.md).
