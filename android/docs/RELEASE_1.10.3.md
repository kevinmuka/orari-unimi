# Android 1.10.3

`versionCode 53`

Questa versione si installa sopra la 1.10.2 mantenendo preferiti, note,
promemoria e impostazioni.

## Notifiche

- Nelle impostazioni delle notifiche compare un breve suggerimento per
  scegliere **Batteria → Senza restrizioni** quando gli avvisi in background
  arrivano in ritardo. Un pulsante apre le impostazioni di questa app; la voce
  Batteria è evidenziata nel suggerimento e ricordata all'apertura della pagina.
- La scelta resta nelle mani dell'utente: l'app non chiede un'esenzione
  automatica. Android può comunque rinviare i controlli periodici.
- La pillola per Batteria è ora nelle Preferenze, subito sotto
  **Impostazioni notifiche**, con un colore distinto dagli interruttori.
- Le future release contrassegnate come **solo nell'app** generano un avviso
  nel centro notifiche interno e restano scaricabili dalle Preferenze, ma non
  inviano notifiche Android. Le release normali continuano a comparire in
  entrambi i luoghi. L'interruttore già presente **Aggiornamenti dell'app**
  governa entrambi i tipi di avviso.

Questa revisione della 1.10.3 usa `versionCode 53`, così viene riconosciuta
come aggiornamento anche da chi ha già installato la prima 1.10.3.

Verifiche automatiche: 70 test unitari per variante (debug e release), nessun
errore lint e build release riuscita. L'APK è firmato con lo stesso certificato
della prima 1.10.3. La nuova disposizione delle Preferenze resta da verificare
visivamente su un dispositivo.
