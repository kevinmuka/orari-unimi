# Notifiche locali

Dalla versione 1.5.0 Orari UNIMI può controllare in background gli insegnamenti
salvati; il controllo è stato poi esteso agli appelli salvati. Gli avvisi sono
raccolti in un centro notifiche interno. Il sistema è
disattivato per impostazione predefinita. L'utente deve attivare l'interruttore
generale nelle Preferenze e scegliere singolarmente quali categorie usare.
Nella versione 1.5.1 le categorie sono raccolte nella schermata
**Impostazioni notifiche**, accessibile dalla classica riga con freccia che
compare sotto l'interruttore generale. La scelta dell'anticipo è incorporata
nel riquadro **Prossima lezione**.

## Categorie

- **Variazioni delle lezioni**: annullamenti, ripristini e cambi di data, ora,
  aula, docente o note negli insegnamenti salvati.
- **Variazioni degli appelli**: cambi di data, ora, luogo, docente, periodo di
  iscrizione, tipo di prova e degli altri dati pubblicati per gli appelli
  salvati nei promemoria.
- **Apertura iscrizioni**: avviso il giorno in cui diventano disponibili le
  iscrizioni a un appello salvato.
- **Scadenza iscrizioni**: avviso 1, 3 o 7 giorni prima della chiusura.
- **Prossima lezione**: promemoria silenzioso 15, 30 o 60 minuti prima
  dell'inizio.
- **Aggiornamenti dell'app**: avviso silenzioso quando GitHub pubblica una
  versione più recente.

La campanella accanto alle Preferenze mostra il numero di avvisi non letti.
Aprendola si trovano le notifiche raggruppate per data; uno swipe verso sinistra
elimina la singola voce. Trascinando l'elenco verso il basso si avvia subito un
nuovo controllo delle categorie abilitate. Lo storico contiene al massimo 100
elementi ed è salvato solo sul dispositivo. Toccando una variazione si apre il
dettaglio dell'elemento con i valori **Prima** e **Dopo**; da lì si può aprire
il giorno della lezione o l'appello. Anche il tap sulla notifica Android apre
direttamente questo dettaglio, mentre una notifica riepilogativa apre il centro.

## Frequenza e limiti Android

I controlli di lezioni, appelli e aggiornamenti vengono pianificati con
WorkManager ogni 15 minuti, il minimo consentito per il lavoro periodico. È la
frequenza richiesta dall'app, non una garanzia di esecuzione esatta: Android può
rinviare un controllo per batteria, modalità Doze, assenza di rete o limiti
imposti dal produttore. Quando si attiva una categoria viene richiesto anche un
primo controllo appena la rete è disponibile. Ogni apertura o ritorno in primo
piano dell'app richiede inoltre un controllo immediato di tutte le categorie
abilitate; WorkManager accorpa e sostituisce le richieste immediate omonime per
evitare controlli simultanei duplicati.

Dalla 1.10.3, nelle **Preferenze**, subito sotto **Impostazioni notifiche**,
una pillola distinta suggerisce di scegliere **Senza restrizioni** in
**Batteria** se gli avvisi arrivano in ritardo. Il pulsante apre la pagina
delle impostazioni di questa app; da lì si entra in Batteria.
La voce da cercare è evidenziata nel suggerimento e ricordata all'apertura
delle impostazioni.
Il percorso e il nome della voce possono variare secondo il produttore. Questa
scelta può ridurre i ritardi dovuti ai limiti per il lavoro in background, ma
non rende esatti i controlli ogni 15 minuti e può aumentare il consumo della
batteria. L'app non richiede un'esenzione automatica alle ottimizzazioni.

Dalla versione 1.5.4 i recuperi automatici degli orari condividono una finestra
di freschezza di due minuti: se l'interfaccia o un altro worker hanno appena
ottenuto gli stessi dati, il controllo riutilizza quella risposta. Gli
insegnamenti salvati dello stesso anno vengono inoltre richiesti insieme, in
gruppi da massimo 20. La stessa finestra viene applicata agli appelli; per
questi ultimi la cache e i lock sono distinti per corso di laurea e intervallo.
Il refresh manuale continua a forzare la rete. Dettagli e vincoli sono raccolti
in [Sincronizzazione locale degli orari](SCHEDULE_SYNC.md).

I controlli avviati mentre si usa l'app, inclusi quelli richiesti trascinando il
centro notifiche verso il basso, aggiungono gli avvisi soltanto al centro interno.
Le notifiche di sistema vengono pubblicate esclusivamente dai controlli periodici
eseguiti mentre l'app è chiusa o in background. Un avviso già registrato dentro
l'app non viene ripubblicato nel sistema per la stessa variazione o versione.

Il recupero periodico programma un lavoro singolo per l'anticipo della prossima
lezione. Una fotografia successiva lo lascia invariato, lo sostituisce o lo
cancella in base al nuovo orario. Il promemoria non effettua richieste di rete,
ma dipende comunque dall'esecuzione concessa dal sistema e non deve essere
considerato una sveglia esatta. Su Android 13 o successivo serve anche il
permesso di sistema per mostrare gli avvisi. Se il permesso viene revocato, gli
eventi possono comunque essere conservati nel centro notifiche locale.

I promemoria delle iscrizioni sono lavori singoli calcolati dai dati già
scaricati e non effettuano richieste proprie. Quello di apertura è programmato
alle 09:00 del giorno indicato da UNIMI; quello di scadenza alle 09:00 del giorno
ottenuto sottraendo l'anticipo scelto. WorkManager può ritardarli e quindi non
sono sveglie esatte. Ogni fotografia valida riconcilia tutti i lavori: una data
modificata sostituisce il vecchio promemoria, una data rimossa lo cancella e un
appello rimosso cancella entrambi.

## Controllo delle variazioni delle lezioni

Il primo recupero valido crea una fotografia di riferimento senza inviare
avvisi. I controlli successivi confrontano solo le lezioni odierne o future,
identificate prima di tutto dall'identificativo fornito dal portale UNIMI.

Per ridurre i falsi positivi vengono applicate queste protezioni:

1. un errore di rete o del portale non modifica la fotografia precedente;
2. una risposta completamente vuota, quando erano presenti lezioni future,
   deve ripetersi prima di essere accettata;
3. una singola lezione scomparsa deve mancare in due controlli consecutivi;
4. lo stesso cambiamento ha un identificativo stabile e non viene archiviato o
   notificato due volte;
5. la fotografia contiene una firma della selezione degli insegnamenti salvati;
   quando la firma cambia viene creata una nuova baseline senza avvisi;
6. la firma viene verificata di nuovo dopo la risposta di rete, così un worker
   già in corso non può interpretare un salvataggio o una rimozione dell'utente
   come una variazione dell'orario.

Annullamenti espliciti e cambi di campi in una lezione già identificata vengono
segnalati al primo controllo valido. Tutti i confronti e i dati di riferimento
restano nei file privati dell'app; non esiste un server dedicato e non vengono
caricati dati personali.

I corsi di laurea preferiti non fanno parte di questa baseline e aggiungerli o
rimuoverli non genera notifiche. Per le lezioni conta esclusivamente la
selezione degli insegnamenti salvati in **Lezioni**.

## Controllo delle variazioni degli appelli

Il controllo degli appelli considera esclusivamente quelli salvati nei
promemoria. I corsi di laurea preferiti e gli insegnamenti soltanto seguiti
servono alla navigazione e alla ricerca, ma non generano notifiche. Anche qui il
primo recupero valido, o il primo successivo a una modifica della selezione,
crea una baseline senza avvisi.

Un appello è identificato dalla coppia composta dal codice del corso di laurea
e dall'identificativo pubblicato dal portale. Per un appello già noto vengono
confrontati tutti i dati restituiti dall'API: data, ora, luogo, docente,
intervallo alfabetico, tipo di prova, tipo di appello, apertura e chiusura delle
iscrizioni, nome dell'insegnamento, nome del corso e codici dell'insegnamento.

Le protezioni applicate sono le seguenti:

1. se anche una sola richiesta fallisce, la baseline intera rimane invariata;
2. una risposta complessivamente vuota deve ripetersi prima di essere accettata;
3. un singolo appello non più presente deve mancare in due controlli validi
   consecutivi prima di produrre l'avviso **Appello non più disponibile**;
4. le notifiche hanno un identificativo stabile e non vengono duplicate;
5. la firma degli appelli salvati viene controllata prima e dopo la rete, per
   neutralizzare le modifiche fatte dall'utente mentre il worker è attivo;
6. un appello appena salvato aggiorna la baseline e non è trattato come una
   novità pubblicata dall'università.

Dopo la seconda assenza valida, l'appello viene rimosso dai promemoria attivi e
la sua nota personale viene eliminata. L'app conserva però una segnalazione
locale nella sezione **I miei appelli**, sotto **Da verificare**, finché l'utente
non preme **Ho capito**. Questa segnalazione è indipendente dal centro
notifiche: eliminare la notifica non fa perdere l'avviso della scomparsa.

Per `A` appelli salvati appartenenti a `C` corsi di laurea distinti, un ciclo
automatico esegue al massimo `C` richieste, in sequenza. Non esegue richieste se
non esistono appelli salvati. Richieste simultanee per lo stesso corso vengono
accorpate e una risposta ottenuta da meno di due minuti viene condivisa con
l'interfaccia. Il portale espone gli appelli per corso di laurea, quindi sul
solo client non è possibile ridurre in modo corretto il limite sotto `C` senza
un servizio centrale che condivida la stessa risposta tra dispositivi diversi.

## Diagnostica locale

In fondo a **Impostazioni notifiche** compare una riga discreta chiamata
**Stato sincronizzazione**. È chiusa inizialmente e non occupa spazio nelle
schermate principali. Una volta aperta mostra separatamente per lezioni e
appelli l'ultimo controllo riuscito, l'errore più recente, il numero di sorgenti,
le richieste di rete, i riusi della cache e le variazioni rilevate.

Le due icone di aggiornamento eseguono un controllo manuale indipendente per
lezioni o appelli. Il risultato distingue nessuna variazione, variazioni
rilevate ed esecuzione proseguita in background. I dati diagnostici rimangono
nelle preferenze locali e non vengono inviati a un server.

## Canali di sistema

Android espone cinque canali separati: variazioni delle lezioni, variazioni
degli appelli, iscrizioni agli appelli, promemoria delle lezioni e aggiornamenti.
I primi tre usano l'importanza normale; promemoria delle lezioni e aggiornamenti
sono creati silenziosi. Le impostazioni di sistema possono comunque cambiare
visibilità, suono e priorità di ciascun canale.

## Versione visibile e build Android

Il controllo aggiornamenti confronta prima il `versionCode` pubblicato nella
descrizione della release e usa il `versionName` soltanto come compatibilità con
le release più vecchie. In questo modo una nuova build può mantenere lo stesso
numero visibile ed essere comunque rilevata. La descrizione GitHub deve contenere
il marcatore seguente, aggiornato allo stesso valore di `app/build.gradle.kts`:

```html
<!-- orari-unimi-version-code: 24 -->
```

La deduplicazione delle notifiche usa la coppia `versionName:versionCode`, quindi
due build diverse della stessa versione possono generare due avvisi distinti.

## Release solo nell'app

L'interruttore esistente **Aggiornamenti dell'app** governa gli avvisi
automatici. Quando è attivo, una release normale viene aggiunta al centro
notifiche dell'app e, se il controllo avviene in background, anche al canale
Android degli aggiornamenti. Una release contrassegnata come **solo nell'app**
entra nel centro notifiche, ma non viene mai pubblicata nel sistema Android.
Se l'interruttore o quello generale delle notifiche è spento, non viene creato
nessun avviso automatico. Il controllo manuale nelle Preferenze continua a
mostrare le release normali; include quelle solo nell'app soltanto quando gli
avvisi degli aggiornamenti sono abilitati. Toccando l'avviso interno si aprono
le Preferenze, dove si può scaricare e installare l'APK come per ogni altra
versione.

Per pubblicare una release solo nell'app, aggiungere alle note GitHub il
marcatore esatto:

```html
<!-- orari-unimi-update: in-app-only -->
```

Pubblicarla inoltre con `gh release create ... --latest=false`. Le vecchie
versioni leggono soltanto l'endpoint GitHub `/releases/latest`: se la release
diventasse **Latest**, potrebbero inviare comunque una notifica Android perché
non conoscono il marcatore. Dalla build 53 il client consulta invece l'elenco
delle release Android pubblicate, seleziona la build più recente e usa il
marcatore per decidere il canale. Le release normali non devono contenere il
marcatore e vanno pubblicate come **Latest**. Verificare entrambe le proprietà
dopo la pubblicazione. Il `versionCode` deve sempre crescere; la release deve
contenere l'APK firmato con il certificato abituale e il suo checksum.
