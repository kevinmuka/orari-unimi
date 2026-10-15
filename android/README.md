# Orari UNIMI per Android

App nativa per Android, realizzata con Kotlin, Jetpack Compose e Material 3.
Usa gli endpoint pubblici AgendaWeb: ricerca per corso, docente, insegnamento o
aula, calendario settimanale, insegnamenti salvati e calendario
personale aggregato. I corsi di laurea sono distinti per tipo con colore ed
etichetta; aprendo un corso si vedono gli insegnamenti di ogni percorso e si può
salvare il corso nei Preferiti. Dal calendario di un corso o docente si può
aggiungere direttamente un insegnamento ai propri orari. La visibilità del
weekend si sceglie nelle Preferenze e resta salvata.
Dal calendario settimanale si può aprire la vista mensile, scorrere tra i mesi e
saltare direttamente a una data oppure tornare a oggi.
La vista agenda mostra gli orari dalle 08:30 alle 19:30 e permette di passare
tra la modalità giornaliera e quella settimanale, con scorrimento libero in
entrambe le direzioni. È disponibile sia per il calendario personale sia per i
corsi di laurea. Il widget riassume le lezioni del giorno in formato agenda e
consente di passare al giorno precedente o successivo.
La sezione Appelli consulta gli esami pubblicati per ogni corso di laurea e li
raggruppa per data. Si possono vedere gli appelli di oggi, dei prossimi 30 giorni
o tutti quelli programmati, controllare aula, docente, tipo di prova e periodo
di iscrizione e aggiungere un appello al calendario Android. Appelli apre il
calendario degli appelli salvati, con lista cronologica e ricerca per corso.
Nei Preferiti gli insegnamenti si possono seguire per Lezioni, Appelli o entrambi,
senza duplicare le schede. I corsi di laurea salvati stanno nello stesso tab.
La sezione Appelli si apre anche dalla pagina di un corso. Ogni appello salvato
può avere una nota locale,
visibile e rimovibile dal relativo pannello di dettaglio. Origine dei dati,
cache e limiti sono descritti in
[docs/EXAMS.md](docs/EXAMS.md).
In Esplora, **Aule** cerca sul catalogo locale per nome, sede o codice. Aprendo
un'aula si vede il planning del giorno, con stato calcolato dagli eventi
pubblicati e orario dell'ultimo aggiornamento. La disponibilità indica ciò che
risulta dal planning, non la presenza fisica nell'aula. Endpoint, cache e limiti
sono descritti in [docs/ROOMS.md](docs/ROOMS.md).
Nelle Preferenze l'app controlla le release pubblicate su GitHub e, quando è
disponibile una versione più recente, può scaricare e verificare l'APK prima di
aprire l'installer di sistema. Android richiede sempre la conferma dell'utente
per completare l'aggiornamento; dopo l'installazione l'APK scaricato viene
eliminato automaticamente dalla cache dell'app. Se una release mantiene lo
stesso numero visibile, un `versionCode` superiore nella descrizione GitHub la
rende comunque rilevabile come nuova build.
Le notifiche sono interamente facoltative e configurabili nelle Preferenze.
Due interruttori distinti possono segnalare le variazioni delle lezioni e
quelle degli appelli salvati, inclusi cambi di data, ora, luogo, docente,
iscrizioni e altri dettagli pubblicati. Salvare o rimuovere un preferito non
produce un avviso: una nuova selezione crea prima una fotografia di riferimento.
Le notifiche delle variazioni aprono un dettaglio con i valori prima e dopo.
Per gli appelli salvati si possono inoltre attivare separatamente i promemoria
per apertura e scadenza delle iscrizioni; se le date cambiano, i lavori locali
vengono riprogrammati. Un appello non più pubblicato rimane tra gli avvisi da
verificare finché l'utente non ne conferma la lettura, anche se elimina la
notifica. L'app può inoltre ricordare in modo silenzioso la prossima lezione e
avvisare silenziosamente delle nuove versioni.
La campanella nella schermata principale apre lo storico locale, dal quale ogni
voce può essere eliminata con uno swipe. I controlli vengono richiesti ogni 15
minuti: quando l'app è aperta aggiornano soltanto lo storico interno, mentre in
background possono mostrare una notifica Android. Dettagli e protezioni adottate
sono descritti in [docs/NOTIFICATIONS.md](docs/NOTIFICATIONS.md).
In fondo alle impostazioni delle notifiche, la riga discreta **Stato
sincronizzazione** mostra su richiesta ultimo esito, sorgenti, rete e cache e
permette di controllare separatamente lezioni e appelli.
Se il record di un corso non elenca ancora gli insegnamenti, l'app cerca quelli
associati al suo codice nell'elenco generale pubblicato dal portale per lo
stesso anno accademico.

La ricerca, gli orari e gli appelli richiedono una connessione Internet. Insegnamenti,
corsi salvati e preferenze sono memorizzati solo nell'app sul dispositivo; non si
sincronizzano automaticamente con la versione per terminale.
I calendari già scaricati vengono mostrati subito e aggiornati in background
quando l'ultima risposta ha più di due minuti; il refresh manuale forza sempre
un nuovo controllo. Gli insegnamenti salvati dello stesso anno vengono richiesti
insieme per evitare chiamate ripetute. Il funzionamento e il percorso previsto
per un futuro backend sono documentati in
[docs/SCHEDULE_SYNC.md](docs/SCHEDULE_SYNC.md). Se il portale non risponde, l'app può usare un
orario salvato da non più di 24 ore e indica chiaramente data e ora dei dati
offline. Anche gli appelli vengono conservati per un massimo di 24 ore come
ripiego offline. I cataloghi restano validi per 12 ore e possono essere recuperati fino
a sette giorni in caso di errore del portale. La cache è limitata a 8 MB ed è
gestita automaticamente da Android.

La struttura delle destinazioni, i componenti condivisi e le funzioni da
preservare nei prossimi interventi sono descritti in
[docs/UX_ARCHITECTURE.md](docs/UX_ARCHITECTURE.md).

## Compilazione

Servono JDK 17 o più recente e Android SDK Platform 36. Da questa cartella:

```sh
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
```

Gli APK sono creati in `app/build/outputs/apk/debug/app-debug.apk` e
`app/build/outputs/apk/release/app-release.apk`. La variante release è firmata
con la chiave locale in `signing/release.keystore` e configurata da
`signing/release.properties`. Questa cartella è esclusa da Git: conserva una
copia sicura di **entrambi** i file, perché la stessa chiave serve per firmare
gli aggiornamenti futuri. Senza questi file la compilazione debug funziona,
mentre la compilazione release si interrompe con un messaggio esplicito.

Per predisporre una nuova chiave su un altro computer, crea la cartella
`signing/` e genera un keystore PKCS12 con alias `orari-unimi` usando `keytool`.
Per esempio:

```sh
mkdir -p signing
keytool -genkeypair -keystore signing/release.keystore -storetype PKCS12 \
  -alias orari-unimi -keyalg RSA -keysize 4096 -validity 36500 \
  -dname "CN=Orari UNIMI"
```

Nel file `signing/release.properties` inserisci `storePassword`, `keyPassword`
e `keyAlias=orari-unimi`. Usa la chiave originale per aggiornare un'app già
installata; una chiave diversa crea una firma incompatibile.

## Installazione

Con il debug USB attivo e `adb` disponibile:

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/release/app-release.apk
```

Puoi anche trasferire l'APK sul telefono e aprirlo da lì. La vecchia build debug
usa la stessa identità dell'app ma una chiave diversa: se è installata, devi
disinstallarla prima di installare la release. La disinstallazione elimina gli
insegnamenti e le preferenze salvati in quella installazione.
