# Ricerca e occupazione delle aule

La categoria **Aule** di Esplora è indipendente dall'anno accademico. Cerca per
nome, sede o codice nell'elenco già scaricato sul dispositivo. Nessuna richiesta
parte a ogni carattere digitato: il planning viene richiesto solo aprendo
un'aula, cambiando data o toccando Aggiorna.

## Dati pubblici di AgendaWeb

Il catalogo arriva da `GET combo.php?sw=rooms_`. Le variabili JavaScript
`elenco_sedi` ed `elenco_aule` contengono codici, nomi, sedi e, quando
disponibile, capienza. Il dettaglio usa `POST rooms_call.php` con `form-type`,
`view` e `include` impostati a `rooms`, un solo `sede[]`, un solo `aula[]`,
`date` in formato `dd-MM-yyyy` e `_lang=it`.

La risposta contiene `events` con `from`, `to`, `name`, `type`, `CodiceAula` e
`Annullato`, oltre a `file_timestamp` per l'ultimo aggiornamento pubblicato.
Un evento annullato resta leggibile nell'elenco ma non rende l'aula occupata.
Il client verifica il codice aula restituito e filtra gli eventi su quel codice.

Il 15 ottobre 2026 il catalogo pubblico comprendeva 76 sedi e 535 aule. Una
risposta per una sola aula pesava circa 0,5 MB; una per una sede di 32 aule
circa 1,5 MB. Per questo non si interroga ogni aula nei risultati della ricerca
e non si esegue alcun controllo periodico in background.

## Stato e cache

La dicitura **Libera secondo il planning** significa soltanto che nessun evento
non annullato copre l'ora corrente. Non garantisce che l'aula sia aperta o
fisicamente vuota. Dopo 30 minuti dall'aggiornamento indicato da UNIMI, oppure
quando si mostra una risposta conservata dopo un errore di rete, lo stato
diventa **Disponibilità da verificare**. La schermata mostra sempre l'ora
dell'ultimo aggiornamento del planning e offre il refresh manuale.

Il catalogo viene riusato per 24 ore e può essere usato fino a 14 giorni se
il portale non risponde. Il planning del giorno corrente viene riusato per
5 minuti; quello di altre date per un'ora. Una risposta già scaricata può
servire da ripiego offline per 24 ore, segnalata come tale. La cache delle aule
è separata da quella delle lezioni e degli appelli e limitata a 16 MB; i file
meno recenti vengono rimossi automaticamente.

Il planning è una fonte pubblicata da AgendaWeb, non un sensore di presenza.
Orari, cancellazioni e disponibilità possono cambiare; quando serve una
certezza operativa, va consultato il portale UNIMI.
