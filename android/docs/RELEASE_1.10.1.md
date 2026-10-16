# Android 1.10.1

`versionCode 49`

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

## Interfaccia

- I dettagli di corsi, edifici, aule e insegnamenti evitano di ripetere il
  nome nella barra superiore e nel contenuto. La barra indica la sezione e la
  destinazione del ritorno; il nome completo resta grande nella pagina.
- Nella lista degli appelli aperta dal dettaglio di un corso, il nome già
  presente accanto a Indietro non viene ripetuto una seconda volta.

Verifiche: 54 test unitari superati, lint Android e build release riusciti.
L'APK è firmato con lo stesso certificato della 1.10.0; versione, firma e
checksum SHA-256 sono stati verificati prima della pubblicazione.
