# Struttura dell'interfaccia Android

Questo documento descrive la struttura funzionale dell'app e gli invarianti da
preservare durante i refactoring dell'interfaccia. L'obiettivo è evitare che una
stessa informazione venga presentata in più elenchi indipendenti o che componenti
equivalenti assumano comportamenti diversi.

## Navigazione principale

La barra inferiore contiene quattro destinazioni stabili, nello stesso ordine in
cui compaiono nell'app:

1. **Lezioni** gestisce gli insegnamenti scelti, il calendario personale e
   l'agenda;
2. **Appelli** contiene la ricerca degli esami e gli insegnamenti seguiti;
3. **Esplora** cerca corsi di laurea, docenti e insegnamenti per anno accademico;
4. **Preferiti** contiene esclusivamente i corsi di laurea salvati.

L'avvio normale e l'apertura dal widget portano a Lezioni.

Gli insegnamenti appartengono a Lezioni perché alimentano calendario personale,
agenda, widget e notifiche. I corsi di laurea preferiti servono invece a
ritrovare il corso con tutti i suoi insegnamenti. In Appelli vengono seguiti
gli insegnamenti: quelli già salvati in Lezioni compaiono automaticamente e
quelli scelti soltanto per gli esami usano la chiave locale
`followed_exam_subjects`. Le due origini restano distinte nell'interfaccia.

Appelli possiede due viste: **Cerca** apre tutti gli appelli di un corso di
laurea, mentre **Seguiti** raccoglie gli insegnamenti provenienti da Lezioni e
quelli aggiunti appositamente. Il calendario mensile e la lista cronologica
degli appelli salvati si aprono dall'azione calendario sempre visibile
nell'intestazione. Sono due viste dello stesso insieme di dati, non due
raccolte. Aprendo un dettaglio e tornando indietro si deve rientrare nel
contesto di provenienza.

## Inventario delle funzioni da preservare

| Area | Funzioni |
| --- | --- |
| Esplora | anno accademico, ricerca corsi/docenti/insegnamenti, colori delle lauree, salvataggio corso |
| Corso | percorsi e insegnamenti, calendario, agenda, appelli, salvataggio corso e insegnamenti |
| Lezioni | prossima lezione, calendario aggregato, agenda giorno/settimana, aggiunta e rimozione insegnamenti |
| Calendari | giorno corrente, navigazione giorno/settimana/mese, weekend facoltativo, cache e refresh manuale |
| Appelli | ricerca per corso, insegnamenti seguiti da Lezioni o aggiunti manualmente, oggi/30 giorni/tutti, dettagli, calendario Android, calendario mensile, lista cronologica, note personali |
| Preferiti | corsi di laurea salvati e accesso ai relativi insegnamenti |
| Notifiche | variazioni, prossima lezione, aggiornamenti, inbox, refresh e cancellazione con annulla |
| Widget | agenda del giorno, navigazione tra date, ritorno a oggi, weekend coerente con le preferenze |
| Preferenze | weekend, notifiche, anticipo promemoria, controllo e installazione aggiornamenti |

## Componenti condivisi

`UiComponents.kt` contiene gli elementi che devono rimanere coerenti tra le
schermate:

- `AppScreenHeader` per titolo, descrizione e azione contestuale;
- `AppSurfaceIconButton` per i pulsanti icona delle intestazioni;
- `AppSegmentedControl` per selettori animati come giorno/settimana,
  finestra degli appelli e sezioni di Appelli;
- `AppEmptyState` per stati vuoti e relativi inviti all'azione.

`DegreeCourseCard` è l'unica scheda dei corsi di laurea usata da ricerca,
Appelli e Preferiti. Colore, icona e gerarchia del testo devono quindi evolvere
insieme. Gli insegnamenti seguiti negli appelli usano una scheda distinta perché
mostrano anche la loro origine.

## Dati e compatibilità

Il riordino conserva le chiavi pubblicate di `LocalStore` e aggiunge
`followed_exam_subjects` per la selezione dedicata agli esami. Ogni elemento
memorizza anche i riferimenti corso/insegnamento necessari a interrogare
l'endpoint degli appelli senza scandire l'intero catalogo. Dopo un aggiornamento
devono restare disponibili:

- insegnamenti e corsi salvati;
- appelli e note personali;
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
