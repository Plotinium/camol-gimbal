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
