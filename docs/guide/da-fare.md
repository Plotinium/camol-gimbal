# Cosa devi fare tu

Stato al 2026-11-26. Regole lette quel giorno; i percorsi dei pulsanti sui siti **non sono stati
verificati**.

## 1. Caricare su GitHub le modifiche

```
git push
```

Aggiorna il repository pubblico con la dichiarazione sull'IA, il nuovo disegno e questa lista.

## 2. Disegno e prove

- [ ] Guarda in gioco il disegno del Camol Gimbal. Per ritoccarlo, file e programmi sono in
      [sviluppo.md](sviluppo.md). Se in futuro vuoi Modrinth, parti da un foglio vuoto: un disegno
      adattato da questi resta derivato da IA.
- [ ] La parte "In ATM10" di [prove.md](prove.md).
- [ ] Un server dedicato (sezione "Da provare su server"). In alternativa pubblica la 0.1.0 come beta,
      dicendo nella descrizione che non è provata su server.

## 3. Scrivere all'autore di Camol

- [ ] Manda la [proposta](proposta.md). Nel testo c'è già la domanda sul disegno ispirato al suo:
      aggiungi quella sul nome "Camol Gimbal". Camol ha tutti i diritti riservati, e CurseForge chiede
      nome e icona distinti e senza immagini protette: **aspetta la risposta prima di pubblicare**.

## 4. CurseForge

- [ ] Crea il file: `.\gradlew.bat build`, poi prendi `build\libs\camolgimbal-1.21.1-0.1.0.jar`.
- [ ] Crea il progetto con: nome "Camol Gimbal"; riassunto "A tool that turns and reshapes Camol
      camouflages."; descrizione in inglese presa da [description.md](../publish/description.md)
      (senza "Page details", **con** la parte "AI disclosure"); licenza MIT; Minecraft 1.21.1,
      NeoForge; client e server; sorgente <https://github.com/Plotinium/camol-gimbal>.
- [ ] Icona: `docs\publish\icon-400.png`.
- [ ] Galleria: foto del gioco non ritoccate con IA, per esempio quelle di
      `.\gradlew.bat visualCheck -PvisualLanguage=en_us`.
- [ ] Carica il file, indica Camol come dipendenza richiesta, note di versione "First release."
- [ ] Invia in revisione.

Nelle [regole lette](https://support.curseforge.com/support/solutions/articles/9000197279) l'unico
obbligo trovato sull'IA è un avviso sulle immagini modificate con IA che possono dare un'idea falsa
della mod. La dichiarazione nella descrizione va oltre quell'obbligo. Non è stata letta ogni altra
regola del sito.

## 5. Modrinth

**Così com'è il progetto non rientra nelle regole**
([punto 6](https://modrinth.com/legal/rules)): sono vietati i progetti composti interamente o
principalmente da contenuti generati da IA, anche se dichiarato. Sono ammessi quelli aiutati dall'IA
in cui la persona ha contribuito "in modo primario e significativo"
([annuncio](https://modrinth.com/news/article/ai-policy-and-disclosures/)).

- [ ] Prima di caricare, chiedi all'assistenza di Modrinth, descrivendo le cose come stanno (codice,
      disegno e testi fatti da un'IA; tue idea, scelte e prove): se il tuo contributo conta come
      primario, e se le foto del gioco con il disegno fatto da IA sono ammesse in galleria.
- [ ] Se la risposta è sì, o dopo che hai rifatto di tua mano ciò che serve:
  - icona e disegno dell'oggetto disegnati da te, senza partire da quelli attuali;
  - descrizione scritta da te (quella in `docs\publish\` è generata da IA e non si può usare);
  - nelle impostazioni del progetto attiva la dichiarazione "Contains AI-generated content" e scegli
    codice, immagini e testi, per le parti che restano generate da IA;
  - Camol non risulta su Modrinth: mettilo nella descrizione con il collegamento a CurseForge.
- [ ] Se la risposta è no, pubblica solo dove le regole lo permettono.

## 6. Legge europea sull'IA (AI Act)

Lettura del testo ufficiale del Regolamento (UE) 2024/1689
([EUR-Lex](https://eur-lex.europa.eu/legal-content/EN/TXT/HTML/?uri=CELEX:32024R1689)), il
2026-11-26. **Non è un parere legale.**

- Gli obblighi di trasparenza (articolo 50) si applicano dal 2 agosto 2026.
- Marcare i contenuti generati spetta a chi **fornisce** il sistema di IA, non a chi lo usa per fare
  una mod.
- A chi usa un sistema di IA l'articolo chiede di dichiarare i "deep fake" (immagini, audio o video
  che sembrano veri e ritraggono persone, oggetti, luoghi o fatti esistenti) e i testi pubblicati per
  informare il pubblico su temi di interesse pubblico. La mod, il suo disegno e la sua pagina non
  sono né l'uno né l'altro.
- L'uso personale e non professionale è escluso dalla definizione di chi usa il sistema.
- La mod non usa IA mentre si gioca.

Da questa lettura **non risultano modifiche richieste** per questa mod. Una modifica successiva al
Regolamento (il "Digital Omnibus", di cui parlano fonti secondarie) non è stata letta sul testo
ufficiale.
