# Cosa provare in gioco

Ogni sezione dice cosa fare, cosa deve succedere e dove guardare se qualcosa non va. Si prova con
`.\gradlew.bat runClient` (vedi [sviluppo.md](sviluppo.md)), in un mondo creativo nuovo.

Il registro del gioco sta in `run\logs\latest.log`.

## Il Gimbal e i blocchi in trasparenza

### Preparazione

1. Metti in un banco da lavoro, griglia 3x3:
   - riga in alto: vuoto, lingotto d'oro, Camo Block;
   - riga in mezzo: vuoto, lingotto di ferro, lingotto d'oro;
   - riga in basso: lingotto di ferro, vuoto, vuoto.

   Deve uscire un Gimbal. Il Camo Block è l'oggetto di Camol, con il nome inglese: Camol non ha la
   traduzione italiana.
2. Guarda il Gimbal nella barra degli oggetti, nell'inventario e in mano: deve avere la sua
   immagine, non il cubo viola e nero dei file mancanti.
3. Camuffa qualche blocco con un Camo Block riempito (Camo Block più un blocco nel banco da lavoro,
   poi clic destro su un blocco).

### Trasparenza

| Cosa hai in mano | Cosa devi vedere sui blocchi camuffati |
|---|---|
| Niente, o un blocco qualsiasi | il loro aspetto finale, pieno |
| Gimbal nella mano principale | semitrasparenti, con il blocco vero visibile sotto |
| Gimbal nell'altra mano (tasto F per scambiare) | semitrasparenti |
| Camo Block nella mano principale | semitrasparenti, come sempre con Camol |
| Camo Block nell'altra mano, niente nella principale | pieni |

Il cambio deve avvenire appena cambi oggetto, su tutti i blocchi camuffati visibili, e restare
stabile: niente blocchi che lampeggiano o che restano in uno stato diverso dagli altri.

### Interruttore della trasparenza spento

Il menu non c'è ancora, quindi l'interruttore si spegne con un comando:

```
/give @s camolgimbal:gimbal[camolgimbal:settings={see_through:false}]
```

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

Il menu non c'è ancora: Shift e clic destro, e il tasto del menu, per ora non aprono niente.

### Tasti

1. Apri Opzioni, Comandi, Assegnazione dei tasti (nomi del gioco in italiano, verificati il
   2026-11-26): c'è la sezione «Camol Gimbal» con «Menu del Gimbal» (Alt
   sinistro) e «Gimbal: più blocchi» (Ctrl sinistro).
2. I due tasti possono comparire in rosso, perché sono gli stessi di altri comandi (lo scatto, il menu
   della chiave di Create): è voluto, funzionano tutti insieme. Tenendo Ctrl mentre cammini, il
   giocatore scatta anche.
3. Con il Gimbal in mano premi Alt sinistro e annota se si apre anche il menu della chiave di Create:
   serve a chi scrive il menu del Gimbal.

### Copiare e togliere (clic sinistro)

In sopravvivenza e poi in creativa, con il Gimbal nella mano principale:

1. Clic sinistro su un blocco camuffato: il blocco non si rompe e non si incrina, il braccio si muove
   una volta. Tieni premuto qualche secondo: non succede altro.
2. Clic destro su una pietra: prende la copia appena fatta.
3. Clic sinistro su un blocco senza camuffamento: compare che non c'è niente da copiare, e il blocco
   non si rompe, anche in creativa.
4. Shift e clic sinistro sul blocco camuffato: il camuffamento sparisce; in sopravvivenza cade il suo
   Camo Block, in creativa no.
5. Apri l'inventario tenendo premuto il tasto sinistro, chiudilo senza lasciarlo: non deve partire una
   copia.
6. Clic sinistro su un mob o un animale: lo colpisci come con la mano.

### Applicare (clic destro)

1. In sopravvivenza, con in inventario un Camo Block vuoto e delle assi di quercia, copia un
   camuffamento di assi e fai clic destro su una pietra: diventa un camuffamento di assi, e spariscono
   un Camo Block e un'asse.
2. Senza niente in inventario, clic destro su un'altra pietra: non cambia niente, e sulla barra in basso
   compare cosa manca.
3. Clic destro su una leva: la leva prende il camuffamento e non si abbassa. Vale con il Gimbal nella
   mano principale; con il Gimbal nell'altra mano e la principale vuota, la leva si abbassa.
4. In creativa, tieni Ctrl e fai clic destro su una fila di pietre staccata dal terreno (niente altra
   pietra che la tocca): si camuffano tutte, fino a 64.
5. Nella prova con `.\gradlew.bat runClient` la spugna non accetta camuffamenti: lo vuole una prova
   automatica, non è un difetto.

### Etichetta

1. Con il Gimbal in mano, guarda un blocco camuffato: sotto il mirino compaiono il blocco, il tipo
   (Camo Block o Shape Retaining Camo Block) e i suoi stati, per esempio «Direzione: Nord».
2. Guarda un blocco senza camuffamento, o metti via il Gimbal: l'etichetta sparisce.
3. Premi F1, poi apri l'inventario: in tutti e due i casi l'etichetta non si vede.
4. Su un blocco che sta in acqua, l'etichetta non mostra la voce dell'acqua.

## Camo che restano senza blocco

Ogni volta deve cadere un solo Camo Block del blocco camuffato, dove c'era il blocco.

1. Esplosione: camuffa della terra e falla esplodere con della TNT.
2. Pistone: camuffa un blocco davanti a un pistone e alimentalo. Il camo cade dove era il blocco; il
   blocco spinto arriva senza camuffamento.
3. Comando: `/setblock` con `minecraft:air` su un blocco camuffato.
4. Rottura a mano: rompi un blocco camuffato in sopravvivenza. Deve cadere un Camo Block solo, non due.
5. Configuratore di Mekanism (in ATM10): togli con il configuratore un blocco camuffato.
6. Chiave di Create: raccogli con Shift e clic destro della chiave un blocco camuffato.
7. Macchina di Create: monta un blocco camuffato su un pistone meccanico di Create e fallo muovere. Il
   camo cade dove era il blocco.
8. Un blocco che cambia, per esempio l'erba che diventa terra o un tronco scortecciato con l'ascia:
   il camuffamento resta.

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

1. Su un server dedicato, metti accanto due blocchi: uno camuffato con lo Shape Retaining Camo Block
   di Camol, uno con il Gimbal (copia il primo con clic sinistro, poi clic destro sul secondo).
2. Camminaci contro e saltaci sopra.

I due devono comportarsi allo stesso modo. Se nessuno dei due ha la forma del blocco camuffato, è un
difetto già noto di Camol sui server dedicati, non del Gimbal.

### Zone protette con FTB Chunks

1. Con due giocatori di squadre diverse, il primo reclama un chunk e camuffa un blocco al suo interno.
2. Il secondo, con il Gimbal, prova a togliere il camuffamento (Shift e clic sinistro), a copiarlo
   (clic sinistro) e ad applicarci sopra una copia (clic destro).

Nessuna delle tre azioni deve riuscire, e sulla barra in basso compare «Non puoi modificare questo
blocco» (o non compare niente, se è FTB Chunks a fermare il clic destro). Fuori dal chunk reclamato
le stesse azioni riescono.
