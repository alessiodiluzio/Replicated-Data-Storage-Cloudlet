# Replicated Data Storage

Lo scopo del progetto contenuto in questa repository è lo sviluppo di un sistema
di Data Storage replicato, il cui deployment è stato eseguito con gli strumenti offerti da Amazon Web Services, in particolar modo EC2.
Il linguaggio scelto per lo sviluppo è Java.

## Architettura del sistema

Il sistema di Data Storage Replicato consta di tre tipologie di nodi:

* Master: gestisce il mapping tra nome di un file e posizione delle repliche. E’ responsabile del coordinamento degli altri nodi del sistema occupandosi di allocare e de-allocare istanze in modo elastico per rispondere alle variazioni del carico sul sistema.
* DataNode: memorizza file e loro contenuto.
* Cloudlet: nodo periferico del sistema, pensato per posizionarsi ai bordi della rete in vicinanza delle sorgenti di dati (Device Client, Sensori...). E’ il front- end del sistema essendo il nodo di interfaccia con il sistema per le operazioni supportate.

Il sistema ha un’architettura multi-Master in cui ogni singolo Master conosce e può contattare tutti gli altri e gestisce un sottoinsieme di DataNode e Cloudlet.
Il numero di Master, DataNode e Cloudlet del sistema all’avvio, è pienamente configurabile.

In questa repository è contenuto il codice del nodo Cloudlet


### Cloudlet
La Cloudlet è pensata per ottimizzare i tempi di risposta delle operazioni di scrittura e lettura e garantire un’alta disponibilità; i dati sottomessi al sistema sono memorizzati in una cache di scrittura e poi inviati al nucleo del sistema in modo asincrono cosicché il dispositivo sorgente non debba soffrire ritardi di risposta a causa dell’overhead delle operazioni di instradamento del file e di replicazione.
La prima lettura di un file avviene sempre richiedendo i dati direttamente al nucleo del sistema, la Cloudlet mantiene i dati dei file richiesti in una cache di lettura e ne richiede periodicamente gli aggiornamenti, tutte le successive operazioni di lettura sono velocizzate dall’utilizzo della cache.

## Dettagli implementativi 


Il Sistema è distribuito su istanze di AWS EC2.
Il Sistema è implementato in linguaggio Java, la comunicazione tra i nodi del sistema avviene sfruttando la tecnologia Java RMI, la gestione delle dipendenze è affidata al tool Maven.
La comunicazione RMI avviene usando gli indirizzi IP pubblici delle istanze EC2.
Le tabelle locali presenti in ogni nodo sono salvate in database relazionali Apache Derby embedded in memory.

La Cloudlet espone i servizi REST tramite i quali è possibile interfacciarsi col sistema, è implementata usando il framework Spring MVC.
Ogni Cloudlet è gestita da un Master di cui conosce l’indirizzo IP pubblico.
I servizi offerti da una Cloudlet sono:
* PUT di un file (nuovo o aggiornamento).
* GET di un file.
* DELETE di un file.

In caso di PUT il contenuto del file è salvato nella cache di scrittura locale alla Cloudlet.
In caso di GET viene prima interrogata la cache di lettura, solo in caso di cache miss il file viene richiesto al Master di riferimento e poi inserito in cache.
L’operazione dei DELETE è invece sempre propagata direttamente al sistema.


