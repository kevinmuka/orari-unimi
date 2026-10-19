# Android 1.10.2

`versionCode 51`

Questa versione si installa sopra la 1.10.1 mantenendo preferiti, note,
promemoria e impostazioni.

## Orari e notifiche

- Le risposte incomplete o malformate di UNIMI non sostituiscono più un
  calendario o un elenco di appelli valido nella cache. Il controllo delle
  variazioni evita così avvisi di lezioni o appelli scomparsi per un errore
  temporaneo della sorgente.
- Un recupero valido aggiorna normalmente gli orari e le notifiche; se la
  sorgente fallisce, l'app conserva i dati precedenti secondo i limiti della
  cache già previsti.

## Navigazione

- Le pagine aperte da notifiche, preferiti, ricerca e calendari mantengono il
  corretto percorso di ritorno anche durante il caricamento o dopo una
  rotazione dello schermo.
- Se Android ricrea l'app mentre era in background, vengono ripristinati la
  pagina e il giorno o mese selezionato quando i dati sono ancora disponibili.
  Se un elemento non esiste più, l'app torna alla pagina che lo conteneva.

Verifiche automatiche: 68 test unitari per variante (debug e release), nessun
errore lint e build release riuscita. L'APK è firmato e la versione è stata
verificata. Non è stata eseguita una prova manuale su un dispositivo collegato.
