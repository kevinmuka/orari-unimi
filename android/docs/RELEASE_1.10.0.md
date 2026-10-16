# Android 1.10.0

`versionCode 47`

Questa release riunisce tutte le modifiche delle anteprime successive alla
1.9.0, dalle build 1.9.1-preview fino a 1.9.1-preview.12. I dati salvati dalle versioni
precedenti restano nella stessa installazione: non occorre disinstallare l'app.

## Aule

- In **Esplora → Aule** compaiono tutti gli edifici, senza dover prima cercare.
  La ricerca trova edifici per nome, indirizzo e codice e può trovare una sede
  anche dal nome di un'aula. I numeri civici devono corrispondere per intero:
  cercare «Celoria, 20» non include «Celoria, 2».
- Ogni edificio mostra indirizzo e numero di aule. Aprendolo si vedono le aule
  e un punto verde, rosso o grigio per distinguere rispettivamente libera,
  occupata e stato non verificabile secondo il planning pubblicato. Il colore
  non certifica la presenza fisica o l'apertura dell'aula.
- Il dettaglio di un'aula mostra gli impegni del giorno, con navigazione fra le
  date e aggiornamento manuale. Il titolo identifica l'aula e la sede compare
  sotto; i dati non disponibili o offline sono indicati esplicitamente.
- Il catalogo degli edifici, il planning dell'edificio e quello di una singola
  aula vengono riusati con cache separate. Aprire un edificio richiede il suo
  planning in una sola chiamata, senza interrogare ogni aula. Frequenze, ripieghi
  offline e limiti sono descritti in
  [ROOMS.md](https://github.com/kevinmuka/orari-unimi/blob/android-v1.10.0/android/docs/ROOMS.md).

## Interfaccia e navigazione

- Esplora usa un unico selettore animato per **Corsi, Docenti, Insegnamenti e
  Aule**, con icone leggibili e spaziatura coerente. Il controllo dell'anno
  accademico si nasconde gradualmente quando si passa alle Aule. Tornando su
  Esplora non si ripete l'ultima animazione del selettore.
- La ricerca delle Aule viene indicizzata fuori dal thread dell'interfaccia;
  caricamento e aggiornamento usano una sola barra di progresso, evitando
  risultati vuoti provvisori e flash durante il passaggio tra schermate.
- Le pagine di dettaglio mostrano un titolo proprio e, accanto alla freccia,
  il nome della pagina a cui si torna. I titoli e le azioni di Lezioni e Appelli
  sono allineati. L'edificio mostra il numero di aule accanto al nome.
- Le schede degli insegnamenti condividono stile e azioni tra ricerca, dettaglio
  del corso e Preferiti. In Preferiti **Lauree** diventa **Corsi**, con le icone
  usate in Esplora; dopo il caricamento appaiono le azioni Lezioni e/o Appelli
  disponibili per ciascun insegnamento.
  I pulsanti `+` aprono sempre la categoria corretta di Esplora, anche dopo
  aver consultato Aule.

## Calendari e widget

- Nei calendari mensili di lezioni e appelli, uno, due o tre pallini indicano
  esattamente uno, due o tre elementi. Da quattro in su compare il numero
  preciso; quando il giorno è evidenziato, il numero si allontana leggermente
  dal cerchio senza spostare la data o i pallini.
- Il widget conserva la navigazione manuale fra i giorni per tutta la giornata.
  Al primo aggiornamento dopo il cambio di data torna a oggi, rispettando la
  preferenza sui weekend. Il tocco centrale per tornare a oggi usa un feedback
  arrotondato e discreto. Android può posticipare l'aggiornamento periodico:
  il cambio non è garantito esattamente a mezzanotte.

La release mantiene tutte le funzioni della 1.9.0: orario settimanale e agenda,
calendario mensile delle lezioni, appelli e promemoria, Preferiti, notifiche e
aggiornamento dall'app.

Verifiche: 52 test unitari superati, lint Android e build release riusciti.
L'APK è firmato con lo stesso certificato della 1.9.0; versione, firma e
checksum SHA-256 sono stati verificati prima della pubblicazione.
