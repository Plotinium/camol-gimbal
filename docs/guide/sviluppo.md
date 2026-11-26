# Compilare e provare Camol Gimbal

I comandi si lanciano in PowerShell dentro la cartella del progetto. Serve Java 21, già presente sul
PC (verificato il 2026-11-26). La prima volta scaricano ciò che manca (NeoForge, Camol, Create,
FTB Chunks) dai loro archivi ufficiali.

| Comando | Cosa fa | Cosa tocca |
|---|---|---|
| `.\gradlew.bat build` | Crea il file della mod in `build\libs\` | solo la cartella `build\` |
| `.\gradlew.bat runGameTestServer` | Lancia le prove automatiche e si chiude | un mondo di prova in `run\gametest\`, rifatto ogni volta |
| `.\gradlew.bat runClient` | Avvia il gioco con la mod, Camol, Create e FTB Chunks | i mondi in `run\`, separati dalle tue istanze |

Per provare la mod in ATM10, copia il file `camolgimbal-1.21.1-<versione>.jar` da `build\libs\`
nella cartella `mods` dell'istanza. Per un server dedicato il file va sia nel server sia nel client.

La lista di cosa controllare in gioco è in [prove.md](prove.md).
