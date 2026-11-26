# Pubblicare Camol Gimbal

Si pubblica **dopo** aver provato la mod con la lista in [prove.md](prove.md). Lo fai tu, dai tuoi
account. I testi della pagina sono in [description.md](../publish/description.md), l'icona è
`docs\publish\icon.png`. I percorsi dei pannelli di CurseForge e Modrinth **non sono stati
verificati** sui siti attuali.

## Preparare il file

```
.\gradlew.bat build
```

Il file da caricare è `build\libs\camolgimbal-1.21.1-0.1.0.jar`.

## Cosa scrivere sui due siti

| Voce | Valore |
|---|---|
| Nome | Camol Gimbal |
| Riassunto | A tool that turns and reshapes Camol camouflages. |
| Descrizione | il testo di `description.md`, senza la parte "Page details" |
| Versione di Minecraft | 1.21.1 |
| Loader | NeoForge |
| Ambiente | client e server |
| Licenza | MIT |
| Dipendenza obbligatoria | Camol |

- **CurseForge:** crea un nuovo progetto di tipo mod, compila la pagina, poi carica il file nella
  sezione dei file e aggiungi Camol fra le relazioni come dipendenza richiesta.
- **Modrinth:** crea un nuovo progetto, compila la pagina, poi crea una versione con il file e
  aggiungi Camol fra le dipendenze. Se Camol non è su Modrinth, dillo nella descrizione con il
  collegamento a CurseForge.

Il codice sorgente su GitHub è facoltativo. Se lo pubblichi, i file di Camol non ci sono dentro: il
progetto li scarica da CurseForge quando compila.
