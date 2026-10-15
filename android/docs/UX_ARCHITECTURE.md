# Struttura dell'interfaccia Android

Questo documento descrive la struttura funzionale dell'app e gli invarianti da
preservare durante i refactoring dell'interfaccia. L'obiettivo è evitare che una
stessa informazione venga presentata in più elenchi indipendenti o che componenti
equivalenti assumano comportamenti diversi.

## Navigazione principale

La barra inferiore contiene quattro destinazioni stabili, nello stesso ordine in
cui compaiono nell'app:

1. **Lezioni** apre direttamente l'orario settimanale; da lì si passa alla vista
   giornaliera, al calendario mensile e agli insegnamenti preferiti;
2. **Appelli** apre subito il calendario degli appelli salvati e la loro lista
   cronologica; la ricerca è un'azione nell'intestazione;
3. **Esplora** cerca corsi di laurea, docenti e insegnamenti per anno accademico,
   e aule indipendentemente dall'anno;
4. **Preferiti** raccoglie gli insegnamenti e i corsi di laurea salvati.

L'avvio normale e l'apertura dal widget portano a Lezioni.

Gli insegnamenti si gestiscono da un'unica raccolta in Preferiti. Un elemento
può alimentare il calendario delle lezioni, essere seguito soltanto per gli
appelli o entrambe le cose. La scelta avviene nello stesso pannello aperto da
Esplora, dal dettaglio di una lezione o dalla scheda nei Preferiti. Per un nuovo
insegnamento con appelli disponibili, entrambe le destinazioni sono preselezionate.
La raccolta unisce per anno e codice gli
insegnamenti salvati per le lezioni e quelli nella chiave locale
`followed_exam_subjects`, senza duplicare le schede o modificare le chiavi già
pubblicate. La provenienza resta leggibile su ciascuna scheda. I corsi di laurea
preferiti sono una seconda categoria della stessa destinazione.

Lezioni usa gli insegnamenti scelti per comporre calendario, agenda, widget e
notifiche. La prima schermata è il calendario settimanale, con i giorni
selezionabili e le lezioni del giorno sotto. Dall'intestazione si apre l'agenda
giornaliera o settimanale; dal calendario si raggiunge il mese. La gestione
della raccolta rimanda a Preferiti sia da Lezioni sia da Appelli. Appelli
mostra gli appelli che lo studente ha salvato: calendario mensile e vista
cronologica sono due rappresentazioni dello stesso insieme. La ricerca per
corso di laurea si apre in un pannello sopra il calendario. Aprendo un dettaglio e tornando
indietro si deve rientrare nel contesto di provenienza.
Nel dettaglio di un corso di laurea Lezioni e Appelli sono le due azioni
affiancate. Il salvataggio del corso resta nella barra superiore; l'agenda si
apre dal calendario delle lezioni del corso, e Indietro torna a quel calendario.

## Inventario delle funzioni da preservare

| Area | Funzioni |
| --- | --- |
| Esplora | anno accademico, ricerca corsi/docenti/insegnamenti, ricerca aule per sede o nome, colori delle lauree, salvataggio corso |
| Corso | percorsi e insegnamenti, calendario, agenda, appelli, salvataggio corso e insegnamenti |
| Lezioni | orario settimanale iniziale, agenda giorno/settimana, calendario aggregato giorno/mese, accesso agli insegnamenti preferiti |
| Calendari | giorno corrente, navigazione giorno/settimana/mese, weekend facoltativo, cache e refresh manuale |
| Appelli | calendario degli appelli salvati, lista cronologica, ricerca per corso, oggi/30 giorni/tutti, dettagli, calendario Android, note personali, avvisi persistenti per appelli rimossi |
| Preferiti | insegnamenti usati nelle lezioni o seguiti solo per gli appelli, corsi di laurea salvati, aggiunta e rimozione |
| Notifiche | variazioni con Prima/Dopo, prossima lezione, iscrizioni agli appelli, aggiornamenti, inbox, refresh e cancellazione con annulla |
| Widget | agenda del giorno, navigazione tra date, ritorno a oggi, weekend coerente con le preferenze |
| Preferenze | weekend, notifiche, anticipi dei promemoria, diagnostica discreta, controllo e installazione aggiornamenti |

## Componenti condivisi

`UiComponents.kt` contiene gli elementi che devono rimanere coerenti tra le
schermate:

- `AppScreenHeader` per titolo, descrizione e azione contestuale;
- `AppSurfaceIconButton` per i pulsanti icona delle intestazioni;
- `AppSearchField` per le ricerche in Esplora e Appelli;
- `AppCountBadge` per i conteggi delle raccolte;
- `AppSectionHeader` per il titolo di una raccolta, il conteggio e l'azione
  di aggiunta;
- `AppSegmentedControl` per selettori animati come giorno/settimana,
  finestra degli appelli, viste di Appelli e categorie di Preferiti;
- `AppEmptyState` per stati vuoti e relativi inviti all'azione.
- `AppLoadingState` per il progresso circolare nel contenuto; la barra sottile
  superiore segnala un recupero attivo senza coprire i dati già disponibili;
- `AppBottomNavigation` per le quattro destinazioni, con indicatore sottile e
  area di tocco coerente.

`SubjectSelectionSheet` è l'unico pannello per scegliere Lezioni, Appelli o
entrambi. I pulsanti `+` di Preferiti aprono Esplora nella categoria corretta:
Corsi per le lauree e Insegnamenti per gli insegnamenti. La ricerca degli
appelli usa un pannello sopra la schermata principale, mantenendo visibile il
contesto del calendario.

`DegreeCourseCard` è l'unica scheda dei corsi di laurea usata da ricerca,
Appelli e Preferiti. Colore, icona e gerarchia del testo devono quindi evolvere
insieme. Gli insegnamenti preferiti usano una scheda distinta perché mostrano
anche se alimentano le lezioni o soltanto gli appelli.

La versione 1.9.0 compatta Esplora: l'anno accademico si sceglie
accanto alla sua etichetta, seguito da categoria e ricerca. Il selettore apre
un pannello con margini coerenti invece di un menu ancorato al bordo. Lezioni
mostra subito il calendario settimanale senza ripetere la raccolta di Preferiti.
Appelli parte dal calendario degli appelli salvati, con il selettore delle viste
sopra i controlli del mese.
Preferiti usa le stesse intestazioni e azioni di Lezioni: aggiunta con `+` e
gestione dell'elenco dal menu a tre punti. La barra superiore mostra il nome
dell'app senza ripetere l'anno selezionabile in Esplora.
Le righe delle raccolte animate con chiavi stabili accompagnano aggiunte e
rimozioni senza cambiare la struttura dei dati.
La barra inferiore conserva la selezione a pillola; il tocco non mostra un
rettangolo di pressione sopra il pulsante.

Un indice non ancora ricevuto, una ricerca in corso e un calendario personale
ancora da recuperare non equivalgono a zero elementi: finché non è terminato un
tentativo valido si mostra il caricamento. Solo dopo si mostra il risultato
vuoto o un errore con azione di riprova. Durante un aggiornamento di dati già
visibili basta la barra sottile superiore.

## Dati e compatibilità

Il riordino conserva le chiavi pubblicate di `LocalStore`. La chiave
`followed_exam_subjects` memorizza la scelta esplicita degli appelli, mentre
`excluded_exam_subjects` memorizza chi ha scelto solo Lezioni. Un insegnamento
già salvato per le lezioni resta disponibile per gli appelli, quando la sorgente
è conosciuta, salvo esclusione esplicita. La scelta è quindi compatibile con i
dati precedenti e si può modificare in seguito. Ogni elemento seguito per gli
appelli memorizza anche i riferimenti corso/insegnamento necessari a interrogare
l'endpoint degli appelli senza scandire l'intero catalogo. Dopo un aggiornamento
devono restare disponibili:

- insegnamenti e corsi salvati;
- appelli e note personali;
- avvisi non ancora riconosciuti per appelli rimossi;
- preferenza weekend e configurazione notifiche;
- baseline delle variazioni e inbox locale.

Ogni modifica futura alla persistenza richiede una migrazione esplicita e un
test con dati prodotti dalla versione precedente.

## Regole per i prossimi interventi

- aggiungere una funzione alla destinazione che possiede quel dato, evitando un
  secondo elenco locale;
- preferire una scorciatoia verso la destinazione originale a una copia della
  stessa lista;
- riutilizzare i componenti condivisi prima di introdurre una variante;
- mantenere visibili cache, stato offline e aggiornamento manuale nelle schermate
  che dipendono dai servizi UNIMI;
- verificare sempre navigazione indietro, apertura da widget e ritorno da una
  notifica.
