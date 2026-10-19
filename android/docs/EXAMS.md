# Appelli degli esami

La sezione Appelli usa il servizio JSON richiamato dalla pagina ufficiale
"Calendario degli appelli" dell'Università degli Studi di Milano:

- `https://work.unimi.it/foProssimiEsami/json/{codice-corso}/1` per oggi;
- `https://work.unimi.it/foProssimiEsami/json/{codice-corso}/30` per i prossimi 30 giorni;
- `https://work.unimi.it/foProssimiEsami/json/{codice-corso}` per tutti gli appelli programmati.

Il servizio è pubblico e non richiede autenticazione, ma non è documentato come
API stabile. L'integrazione è quindi isolata in `ExamApi`: modifiche future al
servizio non devono propagarsi alla UI o al resto del client AgendaWeb.

## Dati utilizzati

Ogni risposta raggruppa gli appelli per insegnamento. L'app conserva il codice e
il nome dell'insegnamento e, per ogni appello, data, ora, aula, docente, fascia
alfabetica, tipo di prova, tipo di appello e apertura e chiusura delle iscrizioni.
`idAppello` è usato come identità primaria; se manca, viene costruita una chiave
locale dai campi disponibili.

Ora, aula e alcune descrizioni possono non essere ancora pubblicate. La UI le
tratta come opzionali e non inventa valori mancanti. L'endpoint non fornisce la
durata dell'esame: l'inserimento nel calendario propone l'ora iniziale e lascia
all'app calendario la gestione della durata. Se manca anche l'ora, l'evento è
proposto come evento giornaliero.

## I miei appelli

Il segnalibro presente su ogni appello lo salva esclusivamente sul dispositivo.
“I miei appelli” offre una vista mensile e una lista cronologica di tutti gli
appelli salvati. Il calendario evidenzia con uno o più punti i giorni che
contengono appelli; selezionando una data mostra gli esami del giorno. Lo stesso
pannello di dettaglio si apre dal calendario, dalla lista dei salvati e dai
risultati di un corso, tornando poi alla schermata di provenienza. Dal dettaglio
si può salvare o rimuovere l'appello, aggiungerlo al calendario Android e creare,
modificare o eliminare una nota personale. Le schede degli appelli salvati
indicano quando è presente una nota. Il calendario completa le settimane
iniziali e finali con i giorni dei mesi adiacenti e può essere sfogliato con le
frecce o con uno scorrimento laterale.
Questa funzione serve come promemoria visivo e non effettua automaticamente
l'iscrizione all'esame; l'iscrizione ufficiale resta su Unimia.
Quando il corso viene aggiornato dalla rete, i dati degli appelli già salvati
vengono riallineati usando `idAppello`, senza cancellare automaticamente un
promemoria se l'appello non compare nella finestra richiesta.

Appelli e note sono salvati soltanto nelle preferenze locali. All'avvio, quando
si apre “I miei appelli” e allo scoccare della mezzanotte mentre l'app è attiva,
gli appelli con una data precedente a quella corrente vengono eliminati insieme
alla rispettiva nota.

## Notifiche delle variazioni

Nelle Preferenze l'utente può attivare **Variazioni degli appelli** in modo
indipendente dalle variazioni delle lezioni. Il controllo riguarda soltanto gli
appelli salvati nei promemoria: aggiungere o rimuovere corsi preferiti e
insegnamenti seguiti non crea notifiche. Anche il salvataggio o la rimozione di
un appello è una scelta locale e serve soltanto a inizializzare una nuova
fotografia di riferimento.

Per gli appelli già presenti nella fotografia vengono rilevati cambi di data,
ora, luogo, docente, fascia alfabetica, tipo di prova, tipo di appello, apertura
o chiusura delle iscrizioni, descrizioni e codici dell'insegnamento. Un appello
scomparso deve mancare in due fotografie valide consecutive ottenute da
recuperi distinti, non da due letture della stessa cache, prima di essere
segnalato. Errori o risultati parziali non sostituiscono la fotografia
precedente. L'implementazione completa, compresi frequenza, deduplicazione e
futura migrazione a un backend, è descritta in
[NOTIFICATIONS.md](NOTIFICATIONS.md) e [SCHEDULE_SYNC.md](SCHEDULE_SYNC.md).

Toccando una notifica si apre il dettaglio della variazione con i valori prima
e dopo. Se un appello manca in due fotografie valide consecutive, viene rimosso
dai promemoria e la nota associata viene cancellata. La sezione **I miei
appelli** mantiene comunque un riquadro **Da verificare** con i dati dell'appello
finché l'utente non conferma di averlo visto; cancellare la notifica non elimina
questo riquadro.

## Promemoria delle iscrizioni

Le Preferenze offrono due opzioni indipendenti per gli appelli salvati:

- avviso alle 09:00 del giorno di apertura delle iscrizioni;
- avviso alle 09:00 di 1, 3 o 7 giorni prima della chiusura.

I lavori sono locali e non effettuano richieste aggiuntive. Ogni aggiornamento
valido degli appelli confronta nuovamente le date e lascia invariato, sostituisce
o cancella il lavoro corrispondente. Prima di mostrare il promemoria, il worker
verifica che l'appello sia ancora salvato e che la data pubblicata sia ancora
quella prevista. Come per gli altri lavori WorkManager, Android può ritardare
l'esecuzione per batteria, Doze o politiche del produttore.

## Cache e aggiornamento

Aprendo un corso viene mostrata subito una risposta recente presente nella cache
locale e viene sempre avviato un aggiornamento di rete. Una risposta può essere
usata offline per non più di 24 ore; la schermata indica chiaramente quando sta
mostrando dati offline e riporta l'ora dell'ultimo aggiornamento. La cache usa lo
stesso limite complessivo di 8 MB degli orari.

Il servizio a monte dichiara una cache HTTP fino a otto ore. L'app richiede una
risposta aggiornata, ma non può garantire che una modifica sia visibile prima
che UNIMI la renda disponibile nel servizio.

## Limiti

- Le richieste sono per corso di laurea, non per singolo insegnamento. Il filtro
  per insegnamento viene quindi applicato ai risultati del corso.
- Non è disponibile un parametro per l'anno accademico o per gli appelli storici.
- Una risposta `404` senza cache valida viene mostrata come elenco vuoto nella
  ricerca, come fa la pagina ufficiale. Se c'è una copia precedente, questa
  rimane visibile con l'avviso di aggiornamento non riuscito. Il `404` non viene
  memorizzato né usato per confermare la scomparsa di un appello salvato.
- Il servizio non espone un collegamento autenticato per iscriversi all'appello:
  l'iscrizione continua a essere effettuata tramite Unimia.
- Non esiste un campo esplicito per un appello annullato. Il sistema di
  notifiche tratta quindi la scomparsa come un segnale incerto e la conferma in
  due controlli validi consecutivi prima di segnalarla.
