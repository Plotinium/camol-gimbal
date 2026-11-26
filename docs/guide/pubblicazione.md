# Pubblicare Camol Gimbal

Regole lette il 2026-11-26. I percorsi dei pulsanti sui due siti **non sono stati verificati**.

## Prima di tutto: contenuti generati da IA

Codice, immagine del Gimbal, icona e testi della pagina di questo progetto sono stati prodotti con
un'IA.

- **Modrinth** ([regole](https://modrinth.com/legal/rules), punto 6): un progetto composto
  principalmente o interamente da contenuti generati da IA **non può essere pubblicato**. Icona,
  galleria e descrizione non possono essere create o derivate da IA. Quando una parte sostanziale è
  generata da IA va attivata la dichiarazione "Contains AI-generated content". Così com'è, il progetto
  non rientra nelle regole.
- **CurseForge** ([regole](https://support.curseforge.com/support/solutions/articles/9000197279)):
  nella pagina letta chiede un avviso visibile sulle immagini modificate da IA che possono dare
  un'idea falsa della mod. Non è stata letta ogni altra regola del sito.

## Prima di caricare

1. Fai la parte "In ATM10" di [prove.md](prove.md); un server dedicato non è ancora stato provato. In
   alternativa pubblica la 0.1.0 come beta, dicendolo nella descrizione.
2. Crea il file: `.\gradlew.bat build`. Il file è `build\libs\camolgimbal-1.21.1-0.1.0.jar`.
3. Icona: CurseForge la chiede di 400x400 pixel, senza immagini protette da diritti.
   `docs\publish\icon.png` è 512x512 e va rifatta a quella misura.
4. Galleria: foto del gioco, per esempio quelle di `.\gradlew.bat visualCheck -PvisualLanguage=en_us`.

## Cosa scrivere

| Voce | Valore |
|---|---|
| Nome | Camol Gimbal |
| Riassunto (una frase) | A tool that turns and reshapes Camol camouflages. |
| Descrizione, in inglese | il testo di [description.md](../publish/description.md), senza "Page details" |
| Versione e loader | Minecraft 1.21.1, NeoForge |
| Ambiente | client e server |
| Licenza | MIT |
| Sorgente | <https://github.com/Plotinium/camol-gimbal> |
| Segnalazioni | <https://github.com/Plotinium/camol-gimbal/issues> |
| Note di versione 0.1.0 | First release. |
| Dipendenza obbligatoria | Camol |

- **CurseForge:** crea il progetto, compila la pagina, carica il file e indica Camol come dipendenza
  richiesta. La revisione è manuale.
- **Modrinth:** Camol non risulta su Modrinth (ricerca del 2026-11-26), quindi non si può scegliere
  come dipendenza: va scritta nella descrizione con il collegamento a CurseForge.
