# Edifici, aule e occupazione

In **Esplora → Aule** compaiono subito tutti gli edifici. La ricerca locale
filtra gli edifici per nome, indirizzo, codice o nome di una loro aula. Ogni
edificio mostra indirizzo e numero di aule; aprirlo mostra tutte le aule e la
loro occupazione corrente. Aprire un'aula mostra gli impegni del giorno, con
navigazione ai giorni precedenti e successivi. Se l'indirizzo non è pubblicato,
l'app lo indica senza inventarne uno.

La ricerca dà precedenza a nome e indirizzo dell'edificio; solo se non trova
una sede cerca tra le aule. Un numero civico deve corrispondere per intero:
"Celoria, 20" non include via Celoria 2 solo perché lì c'è un'aula 20.

## Sorgenti pubbliche di AgendaWeb

Il catalogo arriva da `POST rooms_call.php` con `form-type`, `view` e `include`
impostati a `rooms`, data `dd-MM-yyyy`, `_lang=it` e nessun `sede[]`. In questo
caso `area_rooms` include i metadati di tutte le sedi e aule (nome, indirizzo e
capienza) ma non gli eventi. Il 15 ottobre 2026 erano presenti 76 edifici e
535 aule; la risposta era circa 0,5 MB. Il catalogo precedente
`combo.php?sw=rooms_` non forniva gli indirizzi.

Aprendo un edificio, una sola richiesta a `rooms_call.php` con il suo `sede[]`
recupera il planning delle aule di quell'edificio per oggi. Per Celoria la
risposta era circa 1,5 MB: interrogare ogni aula separatamente avrebbe
moltiplicato inutilmente le richieste. `all_rooms` identifica le aule coperte;
`events` contiene gli impegni, che il client associa tramite `CodiceAula`.
Un'aula assente da `rooms` può comunque essere presente in `all_rooms` e avere
zero impegni: per questo non usiamo `rooms` come elenco completo. Un evento
con `Annullato=1` rimane visibile nel dettaglio ma non rende l'aula occupata.

Quando si apre un'aula, il client riusa il planning dell'edificio se è ancora
fresco. Per un giorno diverso, oppure con il refresh manuale, chiede soltanto
il planning della singola aula (`sede[]` più `aula[]`). Non partono richieste
per ogni risultato della ricerca né controlli periodici in background.

## Stato e cache

Il punto verde indica che nessun evento pubblicato copre l'ora corrente; il
punto rosso indica un evento in corso. Entrambi descrivono **il planning
pubblicato**, non la presenza fisica o l'apertura dell'aula. Se il planning
non è disponibile, o si sta usando un ripiego offline, il punto è grigio.
L'ora di pubblicazione di UNIMI resta visibile e il planning si può aggiornare
manualmente. Il dettaglio dell'aula mostra gli impegni senza un riquadro
separato di disponibilità.

Il catalogo viene riusato per 24 ore e può essere usato fino a 14 giorni se
il portale non risponde. Il planning di oggi, sia per un edificio sia per
un'aula, viene riusato per 5 minuti; quello di altri giorni per un'ora. Una
risposta già scaricata può servire da ripiego offline per 24 ore, segnalata
come tale. La cache delle aule è separata da quella delle lezioni e degli
appelli e limitata a 16 MB; i file meno recenti vengono rimossi.
