# Cosa provare in gioco

Ogni sezione dice cosa fare, cosa deve succedere e dove guardare se qualcosa non va. Si prova con
`.\gradlew.bat runClient` (vedi [sviluppo.md](sviluppo.md)), in un mondo creativo nuovo.

Il registro del gioco sta in `run\logs\latest.log`.

## Prima di tutto: il gioco parte

Il gioco di sviluppo arriva al menu principale senza errori della mod (verificato il 2026-11-26).
In un mondo non è ancora stata vista. Controlla che:

1. il gioco arrivi al menu principale e un mondo si apra;
2. in `latest.log` non ci siano righe ERROR o WARN che nominano `camolgimbal`.

Se il gioco si chiude o una di queste righe compare, fermati e manda il registro.

## Il Gimbal e i blocchi in trasparenza

### Preparazione

1. Metti in un banco da lavoro, griglia 3x3:
   - riga in alto: vuoto, lingotto d'oro, Camo Block;
   - riga in mezzo: vuoto, lingotto di ferro, lingotto d'oro;
   - riga in basso: lingotto di ferro, vuoto, vuoto.

   Deve uscire un Gimbal. Il Camo Block è l'oggetto di Camol, con il nome inglese: Camol non ha la
   traduzione italiana.
2. Guarda il Gimbal nella barra degli oggetti, nell'inventario e in mano: deve avere la sua
   immagine (un esserino lilla stretto in vita da tenaglie dorate, su un manico di ferro), non il
   cubo viola e nero dei file mancanti.
3. Metti una camo su qualche blocco con un Camo Block riempito (Camo Block più un blocco nel banco
   da lavoro, poi clic destro su un blocco).

### Trasparenza

| Cosa hai in mano | Cosa devi vedere sui blocchi con una camo |
|---|---|
| Niente, o un blocco qualsiasi | il loro aspetto finale, pieno |
| Gimbal nella mano principale | semitrasparenti, con il blocco vero visibile sotto |
| Gimbal nell'altra mano (tasto F per scambiare) | semitrasparenti |
| Camo Block nella mano principale | semitrasparenti, come sempre con Camol |
| Camo Block nell'altra mano, niente nella principale | pieni |

Il cambio deve avvenire appena cambi oggetto, su tutti i blocchi con una camo visibili, e restare
stabile: niente blocchi che lampeggiano o che restano in uno stato diverso dagli altri.

### Interruttore della trasparenza spento

Con il Gimbal in mano apri il menu (Shift e clic destro) e metti «Camo trasparenti» su No. Chiudi il
menu: ogni Gimbal tiene il proprio interruttore. Per le righe con due Gimbal, il secondo resta com'è
appena creato.

| Cosa hai in mano | Cosa devi vedere |
|---|---|
| Solo questo Gimbal, in una delle due mani | blocchi pieni |
| Questo Gimbal e, nell'altra mano, un Gimbal normale | semitrasparenti |
| Camo Block nella principale, questo Gimbal nell'altra | semitrasparenti |

### Con un altro giocatore (se puoi)

Su un server con due giocatori: se l'altro tiene in mano un Camo Block, la tua vista non deve
cambiare.

### Dove guardare

Cerca `camolgimbal` in `run\logs\latest.log`. Se compare la riga
`was not found or has changed: camouflaged blocks are not drawn see-through while a Gimbal is held`,
la versione di Camol installata ha cambiato il punto in cui la mod si aggancia: il Gimbal non rende
trasparenti i blocchi, mentre il Camo Block continua a funzionare. Vale lo stesso per ogni riga
`WARN` che nomina insieme `camolgimbal` e una classe di Camol (`cy.jdkdigital.camol`). Se righe
così non ci sono, l'aggancio è riuscito.

## Comandi

### Tasti

1. Apri Opzioni, Comandi, Assegnazione dei tasti (nomi del gioco in italiano, verificati il
   2026-11-26): c'è la sezione «Camol Gimbal» con «Menu del Gimbal» (Alt
   sinistro) e «Gimbal: più blocchi» (Ctrl sinistro).
2. I due tasti possono comparire in rosso, perché sono gli stessi di altri comandi (per esempio lo
   scatto): è voluto, funzionano tutti insieme. Tenendo Ctrl mentre cammini, il giocatore scatta
   anche.

### Copiare e togliere (clic sinistro)

In sopravvivenza e poi in creativa, con il Gimbal nella mano principale:

1. Clic sinistro su un blocco con una camo: il blocco non si rompe e non si incrina, il braccio si
   muove una volta. Tieni premuto qualche secondo: non succede altro.
2. Clic destro su una pietra: prende la copia appena fatta.
3. Clic sinistro su un blocco senza camo: compare che non c'è niente da copiare, e il blocco non si
   rompe, anche in creativa.
4. Shift e clic sinistro sul blocco con la camo: la camo sparisce; in sopravvivenza cade il suo Camo
   Block, in creativa no.
5. Apri l'inventario tenendo premuto il tasto sinistro, chiudilo senza lasciarlo: non deve partire una
   copia.
6. Clic sinistro su un mob o un animale: lo colpisci come con la mano.
7. In sopravvivenza, clic sinistro nel vuoto e subito dopo su un blocco camuffato: la copia riesce.
8. Premi il tasto sinistro nel vuoto e, tenendolo premuto, sposta il mirino su un blocco camuffato:
   non succede niente.

### Applicare (clic destro)

1. In sopravvivenza, con in inventario un Camo Block vuoto e delle assi di quercia, copia una camo di
   assi e fai clic destro su una pietra: diventa una camo di assi, e spariscono un Camo Block e
   un'asse.
2. Senza niente in inventario, clic destro su un'altra pietra: non cambia niente, e sulla barra in basso
   compare cosa manca.
3. Clic destro su una leva: la leva prende la camo e non si abbassa. Vale con il Gimbal nella mano
   principale; con il Gimbal nell'altra mano la leva si abbassa, ed è voluto: dall'altra mano viene
   prima l'azione del blocco.
4. In creativa, tieni Ctrl e fai clic destro su una fila di pietre staccata dal terreno (niente altra
   pietra che la tocca): prendono tutte la camo, fino a 64.
5. Nella prova con `.\gradlew.bat runClient` la spugna non accetta camo: lo vuole una prova
   automatica, non è un difetto.

### Etichetta

1. Con il Gimbal in mano, guarda un blocco con una camo: sotto il mirino compaiono il blocco, il tipo
   (Camo Block o Shape Retaining Camo Block) e i suoi stati, per esempio «Direzione: Nord».
2. Guarda un blocco senza camo, o metti via il Gimbal: l'etichetta sparisce.
3. Premi F1, poi apri l'inventario: in tutti e due i casi l'etichetta non si vede.
4. Compaiono solo gli stati che cambiano l'aspetto o la forma. Una camo di foglie non ha voci sotto il
   tipo; una camo di porta non mostra «Alimentato»; una camo di scale non mostra la voce dell'acqua.

## Il menu

Si prova prima in finestra. La finestra di `runClient` appena aperta, con Opzioni, Grafica, «Scala»
su automatica, è il caso più stretto: ruota e pannello non si devono sovrapporre, e niente deve
uscire dallo schermo. Poi ingrandisci la finestra e prova anche lo schermo intero: ridimensionando con
il menu aperto, tutto si ridispone. Controlla anche che le scritte lunghe sui tasti del pannello
(scorrono dentro il tasto) si leggano in italiano e in inglese.

### Aprire e chiudere

Prepara una camo di scale di quercia e tieni il Gimbal nella mano principale.

| Cosa fai | Cosa deve succedere |
|---|---|
| Guardi la camo e tieni premuto Alt sinistro | si apre il menu con la ruota «Questa camo»; il mouse è al centro della ruota |
| Rilasci Alt senza muovere il mouse | il menu si chiude, la camo non cambia |
| Tieni Alt, porti il mouse su uno spicchio, rilasci Alt | la camo prende quel valore e il menu si chiude |
| Tieni Alt, clic sinistro su uno spicchio, e continui a tenere Alt per qualche secondo | la camo cambia, il menu si chiude e non si riapre finché non rilasci e premi di nuovo Alt |
| Tieni Alt, clic destro (poi rifai con Esc) | il menu si chiude senza cambiare niente e non si riapre finché Alt resta premuto; Esc non apre la pausa |
| Tocchi Alt velocemente | il menu lampeggia e si chiude, la camo non cambia |
| Tieni Alt, porti il mouse su uno spicchio e passi a un'altra finestra con Alt e Tab | tornando al gioco la camo non è cambiata |
| Shift e clic destro sulla camo | il menu si apre e resta aperto anche lasciando i tasti; la copia non viene applicata |
| Nel menu aperto così, premi e rilasci Alt | non succede niente |
| Nel menu aperto così, clic sinistro su uno spicchio e tieni premuto | la camo cambia e il menu si chiude; non parte una copia e il blocco non si incrina |
| Clic sinistro al centro della ruota | non succede niente |
| Clic destro per chiudere, e tieni premuto | si chiude, e il clic non applica niente al blocco davanti |
| Shift e clic destro in aria, o su un blocco senza camo | la ruota è «Copia salvata»; senza niente di copiato c'è solo il pannello |
| Clic sinistro su uno spicchio che ha il valore di adesso | il menu si chiude e non cambia niente |

Rifai le righe con Alt dopo aver spostato «Menu del Gimbal» su una lettera, poi su un tasto laterale
del mouse. Prova anche con il Gimbal nell'altra mano.

Il menu non mette in pausa il gioco, nemmeno in singolo: con un forno acceso davanti, la fiamma
continua a muoversi.

Se hai ATM10 o la chiave di Create: con il Gimbal in mano e il mouse su un meccanismo di Create, Alt
apre solo il menu del Gimbal. Con la chiave nella mano principale e il Gimbal nell'altra, se la chiave
ha lo stesso tasto si apre il suo menu.

### La ruota

1. La rotella del mouse passa da uno stato all'altro (per le scale: direzione, metà, forma). Al centro
   si leggono il nome dello stato, il valore indicato dal mouse e a che punto sei (per esempio 2/3);
   sopra e sotto, in grigio, lo stato precedente e il successivo.
2. Ogni spicchio mostra il blocco in 3D con quel valore, girato come lo vedi tu. L'anello chiaro segna
   il valore che il blocco ha adesso.
3. Prova blocchi diversi: un tronco (asse), una lastra, una botola (aperta), un recinto (i quattro
   collegamenti), il grano (crescita), il filo di redstone (segnale, 16 valori: gli spicchi restano
   leggibili), la pietra e il «Blocco sonoro» (compare «Niente da cambiare»). Il primo sguardo al filo di
   redstone, che ha molti stati, non deve bloccare il gioco.
4. Le porte non hanno la voce «Alimentato». Nessun blocco ha la voce dell'acqua. Segnala ogni voce che
   cambia qualcosa a vista e non compare, o che compare e non cambia niente.
5. Accendi della TNT sotto un blocco con una camo, guardalo e apri il menu con Shift e clic destro:
   quando il blocco esplode il menu si chiude da solo.
6. Cambia un valore sulla «Copia salvata», poi clic destro su una pietra: prende il valore scelto.
7. Premi F3 e T, aspetta, riapri il menu: le voci ci sono ancora e i blocchi si disegnano bene.

### Il pannello

1. In alto: il blocco copiato in 3D, il suo nome e il tipo di Camo Block. Senza copia: «Nessuna
   copia».
2. «Cancella copia»: la copia sparisce subito, con la sua ruota, e anche l'elenco delle spunte. Il
   tasto resta spento. Chiudi, copia di nuovo e riapri: le spunte ci sono tutte.
3. Passa il mouse su ogni voce: compare una breve spiegazione.
4. Cambia più voci in fretta, anche più volte la stessa: ogni voce mostra subito la scelta, non torna
   indietro e non lampeggia. Chiudi e riapri: le scelte sono rimaste. Esci dal mondo e rientra: sono
   ancora lì. Su un server, prova anche questo.
5. Tieni il menu aperto con Alt, cambia una voce con un clic, rilascia Alt con il mouse sopra il
   pannello: la voce è cambiata e la ruota non ha applicato niente.
6. Copia una camo di «Libreria cesellata»: nella finestra appena aperta l'elenco «Stati da incollare» non sta
   tutto nel pannello. Scorre con la rotella quando il mouse è sul pannello, con una barra a lato, e
   l'ultima voce si raggiunge.

### Ogni interruttore

Chiudi il menu e fai la prova indicata. Alla fine rimetti il valore di partenza.

| Interruttore | Cosa fare | Cosa deve succedere |
|---|---|---|
| Incolla il tipo: No | copia una camo messa con lo Shape Retaining Camo Block e incolla su una camo normale | la camo resta normale; con Sì diventa dell'altro tipo |
| Stati da incollare | copia scale rivolte a est, togli la spunta a «Direzione», incolla su una camo di scale rivolta a nord | resta a nord, le altre voci arrivano. Su un blocco senza camo la direzione è quella che avrebbe posando il blocco lì |
| Camo trasparenti: No | guarda le camo con il Gimbal in mano | si vedono piene (vedi la tabella sopra) |
| Direzione: Copiata | copia scale, girati, incolla | stessa direzione della copia |
| Direzione: Dallo sguardo | copia scale, girati di un quarto di giro, incolla | la direzione è ruotata di un quarto di giro rispetto alla copia |
| Direzione: Dalla faccia | copia un tronco, incolla su facce diverse | il tronco si orienta come se lo posassi su quella faccia |
| Forma: Collegati, Faccia, Riga, Colonna | con Ctrl, su un blocco di pietre 5 per 5 per 2 staccato dal resto | tutto il gruppo; solo il piano della faccia cliccata; una riga da sinistra a destra; una colonna dal basso in alto (in avanti sul pavimento) |
| Max: 8, 16, 32, 64 | con Ctrl su più di 64 pietre unite | cambiano al massimo tanti blocchi quanti indicati |
| Cambia blocco: Sì | in sopravvivenza, con una copia di assi, un Camo Block vuoto e un'asse, incolla su una camo di pietra | la camo diventa di assi, spendi il Camo Block vuoto e l'asse, torna il Camo Block di pietra; con No cambiano solo le voci in comune, gratis |
| Anteprima: No | guarda un blocco con il Gimbal e una copia salvata | niente anteprima |

## Anteprima

Con una copia salvata e «Anteprima» su Sì, guarda un blocco con il Gimbal nella mano principale.

1. Sul blocco compare il risultato in trasparenza. Clic destro: quello che ottieni è quello che hai
   visto. Prova scale, lastre, tronchi, botole, blocco d'erba o foglie (il colore è quello del posto).
2. Tieni Ctrl: l'anteprima copre tutti i blocchi della serie. Lascialo: torna a uno. Cambia forma e
   massimo dal menu e controlla che l'anteprima li segua.
3. Con «Dallo sguardo» girati, con «Dalla faccia» guarda facce diverse: l'anteprima cambia di
   conseguenza.
4. L'anteprima sparisce tenendo Shift, con il menu o l'inventario aperti, con F1, guardando il cielo,
   togliendo il Gimbal di mano e dopo «Cancella copia».
5. Su un blocco dove il clic non cambierebbe niente (la stessa camo con gli stessi valori) non compare
   niente.
6. Con il Gimbal nell'altra mano l'anteprima non c'è.
7. In sopravvivenza, con Ctrl su una fila di 5 pietre e solo 2 Camo Block pieni in inventario,
   l'anteprima copre 2 blocchi, e il clic ne cambia 2. Non tiene conto delle zone protette: può
   mostrare un blocco che al clic non cambia perché non è tuo.
8. Guardala di giorno, di notte, dentro l'acqua e attraverso il vetro, poi con Opzioni, Grafica,
   «Grafica» su «Sofisticata» e su «Favolosa!»: si deve vedere, senza sfarfallii. Con «Camo
   trasparenti» sia su Sì sia su No.
9. Con Ctrl su 64 blocchi il gioco non deve rallentare in modo visibile.

## Camo che restano senza blocco

Ogni volta deve cadere un solo Camo Block, quello della camo, dove c'era il blocco.

1. Esplosione: metti una camo su della terra e falla esplodere con della TNT.
2. Pistone: metti una camo su un blocco davanti a un pistone e alimentalo. La camo cade dove era il
   blocco; il blocco spinto arriva senza camo.
3. Comando: `/setblock` con `minecraft:air` su un blocco con una camo.
4. Rottura a mano: rompi un blocco con una camo in sopravvivenza. Deve cadere un Camo Block solo, non
   due.
5. Configuratore di Mekanism (in ATM10): togli con il configuratore un blocco con una camo.
6. Chiave di Create: raccogli con Shift e clic destro della chiave un blocco con una camo.
7. Macchina di Create: monta un blocco con una camo su un pistone meccanico di Create e fallo muovere.
   La camo cade dove era il blocco.
8. Un blocco che cambia, per esempio l'erba che diventa terra o un tronco scortecciato con l'ascia:
   la camo resta.

## I Camo Block seguono la faccia cliccata

Con un Camo Block di Camol riempito (non il Gimbal), in un punto con aria davanti alla faccia:

1. Un tronco messo sulla faccia di sopra di un blocco sta in piedi.
2. Una botola su un lato di un blocco: cliccando nella metà bassa resta in basso, nella metà alta in
   alto, e si apre verso la faccia cliccata.
3. L'impostazione sta nel file `config\camolgimbal-server.toml` dell'istanza (per un solo mondo, una
   copia in `serverconfig` dentro la cartella del mondo), alla voce `followClickedFace`. Mettila a
   `false`, rientra nel mondo e rifai le due prove: il tronco si mette coricato verso dove guardi, la
   botola fa come prima dell'addon.

## Da provare su server

### Camo con forma su server dedicato

1. Su un server dedicato, prepara due blocchi vicini: sul primo metti una camo con lo Shape Retaining
   Camo Block di Camol, sul secondo con il Gimbal (copia il primo con clic sinistro, poi clic destro
   sul secondo).
2. Camminaci contro e saltaci sopra.

I due devono comportarsi allo stesso modo. Se nessuno dei due ha la forma del blocco della camo, è un
difetto già noto di Camol sui server dedicati, non del Gimbal.

### Zone protette con FTB Chunks

1. Con due giocatori di squadre diverse, il primo reclama un chunk e mette una camo su un blocco al suo
   interno.
2. Il secondo, con il Gimbal, prova a togliere la camo (Shift e clic sinistro), a copiarla (clic
   sinistro), ad applicarci sopra una copia (clic destro) e a cambiarla dalla ruota del menu (Alt).

Nessuna delle quattro azioni deve riuscire, e sulla barra in basso compare «Non puoi modificare questo
blocco» (o non compare niente, se è FTB Chunks a fermare il clic destro). Fuori dal chunk reclamato
le stesse azioni riescono.

## In ATM10

La prova con `runClient` non ha le mod grafiche di ATM10 (Sodium, Iris) né le altre del pacchetto.
Copia il file della mod nell'istanza come dice [sviluppo.md](sviluppo.md), poi:

1. ripeti i controlli del primo avvio; il registro è `logs\latest.log` nella cartella dell'istanza;
2. ripeti la trasparenza, l'anteprima e il menu con le sue figure in 3D;
3. ripeti la trasparenza e l'anteprima con uno shader attivo, se li usi;
4. nei comandi del gioco, sotto Camol Gimbal, controlla che i due tasti siano ancora assegnati
   (ATM10 ha una mod che può togliere le assegnazioni);
5. con la chiave di Create sullo stesso tasto del menu, controlla quale menu si apre con il Gimbal in
   mano e quale con la chiave.

