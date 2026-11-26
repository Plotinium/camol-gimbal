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

## Cambiare il disegno del Camol Gimbal

| File | Misura | Cos'è |
|---|---|---|
| `src\main\resources\assets\camolgimbal\textures\item\gimbal.png` | quadrato: 32x32 (va bene anche 16x16 o 64x64), sfondo trasparente | l'oggetto in gioco |
| `src\main\resources\camolgimbal_logo.png` | 512x512 | l'immagine nell'elenco delle mod |
| `docs\publish\icon-400.png` | 400x400 | l'icona per CurseForge |

I disegni da usare come riferimento o da cui ripartire stanno in `docs\art\`. In ogni cartella ci sono
`gimbal.png` (32x32, quello da modificare) e `preview.png` (ingrandito su grigio, solo da guardare);
dove c'è anche `draw.py`, è il programma che ha generato quel disegno e non serve per disegnare a mano.

| Cartella | Disegno |
|---|---|
| `docs\art\1-rod` | asta con anello aperto |
| `docs\art\2-tongs` | pinza con le due ganasce, non in linea |
| `docs\art\3-tongs-aligned` | pinza in linea, con la sola ganascia davanti |
| `docs\art\4-final` | pinza in linea, con l'attacco della ganascia dietro in vista a sinistra: **è quella usata dalla mod** |

Per usarne un altro nel gioco, copia il suo `gimbal.png` sopra il primo file della prima tabella. Le
due immagini più grandi vanno poi rifatte con lo stesso disegno.

Programmi per disegnare a pixel (prezzi non verificati): Piskel (gratuito, nel browser), Pixelorama
o LibreSprite (gratuiti), Aseprite (a pagamento), Blockbench (gratuito, mostra anche l'oggetto in
mano). Salva sempre in PNG con la trasparenza, sopra il file indicato.

Per vederlo in gioco chiudi il gioco e rilancia `.\gradlew.bat runClient`. A gioco aperto si può
provare `.\gradlew.bat processResources` e poi F3 insieme a T (non provato). Per un cambio di solo
disegno non servono le prove automatiche.

