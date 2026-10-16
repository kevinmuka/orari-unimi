# Android 1.10.1

`versionCode 50`

Questa versione si installa sopra la 1.10.0 mantenendo preferiti, note,
promemoria e impostazioni.

## Aggiornamenti

- In **Preferenze → Aggiornamenti** una sezione espandibile mostra le principali
  novità della release, ricavate dalla descrizione pubblicata su GitHub. Il
  controllo usa la richiesta già necessaria per cercare aggiornamenti.
- Le note recuperate rimangono sul dispositivo e possono essere rilette anche
  dopo l'installazione, senza connessione. Se l'app è stata installata senza
  averle mai recuperate, compaiono al primo controllo riuscito.

## Appelli

- La lente cerca sia corsi di laurea sia insegnamenti con appelli disponibili
  nell'anno accademico selezionato. I risultati sono separati per categoria;
  aprire un insegnamento mostra gli appelli pertinenti anche se è presente in
  più corsi di laurea.
- Se più riferimenti dello stesso insegnamento appartengono a un unico corso,
  l'app ne recupera gli appelli una sola volta e li filtra localmente. Un
  catalogo parzialmente non disponibile lascia visibili i risultati ricevuti
  e offre un pulsante per riprovare.

## Correzione dei titoli

- Ripristinati i titoli e le etichette Indietro della 1.10.0 nei dettagli.
  Il nome del corso o dell'insegnamento è sempre visibile anche nell'elenco
  degli appelli aperto dai Preferiti o dal dettaglio di un corso.
- Questa revisione della 1.10.1 usa `versionCode 50`, così viene riconosciuta
  come aggiornamento anche da chi aveva già installato la prima 1.10.1.

Verifiche: 54 test unitari superati, lint Android e build release riusciti.
L'APK è firmato con lo stesso certificato della 1.10.0; versione, firma e
checksum SHA-256 sono stati verificati prima della pubblicazione.
