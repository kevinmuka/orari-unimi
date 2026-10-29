# Android 1.10.3

`versionCode 52`

Questa versione si installa sopra la 1.10.2 mantenendo preferiti, note,
promemoria e impostazioni.

## Notifiche

- Nelle impostazioni delle notifiche compare un breve suggerimento per
  scegliere **Batteria → Senza restrizioni** quando gli avvisi in background
  arrivano in ritardo. Un pulsante apre le impostazioni di questa app; la voce
  Batteria è evidenziata nel suggerimento e ricordata all'apertura della pagina.
- La scelta resta nelle mani dell'utente: l'app non chiede un'esenzione
  automatica. Android può comunque rinviare i controlli periodici.

Verifiche automatiche: 68 test unitari per variante (debug e release), nessun
errore lint e build release riuscita. L'APK è firmato e la versione è stata
verificata. L'apertura delle impostazioni non è stata provata su un dispositivo
collegato.
