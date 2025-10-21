---
title: Batch zuweisen
identifier: intranda_step_batch_assignment
description: Step Plugin für die Zuweisung des Vorgangs zu einem bestehenden oder neuen Batch
published: true
keywords:
    - Goobi workflow
    - Plugin
    - Step Plugin
---

## Einführung
Diese Dokumentation erläutert das Plugin für Zuweisung eines einzelnen Vorgangs zu einem Batch. Diese Zuweisung erfolgt hierbei direkt aus einer angenommenen Aufgabe heraus. Dort kann entweder ein neuer Batch erzeugt oder aus einer Liste von bereits vorhandenen wartenden Batches ausgewählt werden. 

## Installation
Um das Plugin nutzen zu können, müssen folgende Dateien installiert werden:

```bash
/opt/digiverso/goobi/plugins/step/plugin-step-ZZZ-base.jar
/opt/digiverso/goobi/plugins/GUI/plugin-step-ZZZ-gui.jar
/opt/digiverso/goobi/config/plugin_intranda_step_ZZZ.xml
```

Nach der Installation des Plugins kann dieses innerhalb des Workflows für die jeweiligen Arbeitsschritte ausgewählt werden. Zu beachten ist hierbei, dass zwei Arbeitsschritte im Workflow eingeplant werden müssen:

- Ein Arbeitsschritt dient für den Nutzer als derjenige, in dem die Batch-Zuweisung erfolgt. 
- Ein weiterer Arbeitsschritt dient als eine Art `Wartezone`, in dem alle bereits zu einem Batch zugewiesenen Vorgänge stehenbleiben und erst bei Vollständigkeit des Batches gemeinsam zum nachfolgenden Schritt wechseln werden. 
 
Ein Workflow könnte somit beispielhaft wie folgt aussehen:

![Beispielhafter Aufbau eines Workflows](screen1_de.png)

Für die Verwendung des Plugins muss dieses in dem ersten der beiden Arbeitsschritte ausgewählt sein:

![Konfiguration des Arbeitsschritts für die Nutzung des Plugins](screen2_de.png)

## Überblick und Funktionsweise
Nachdem der Nutzer die Aufgabe angenommen hat, kann er in dem Plugin zunächst entscheiden, ob ein neuer Batch angelegt oder eine Auswahl aus den bereits vorhandenen noch wartenden Batches erfolgen soll. Möchte der Nutzer einen neuen Batch definieren, kann er hier den Titel für den Batch definieren sowie bei Bedarf auch Eigenschaften mit erfassen, die über die Konfiguration festgelegt wurden:

![Erzeugen eines neuen Batches mit festgelegten Eigenschaften für den Vorgang](screen3_de.png)

Alternativ kann der Nutzer aus der Liste der aktuell wartenden Batches einen Batch auswählen. Nach der Auswahl des gewünschten Batches kann die Aufgabe regulär abgeschlossen werden.

![Auswahl aus der Liste der wartenden Batches](screen4_de.png)

Nach der Zuweisung zu einem bestehenden oder neu erzeugten Batches, geht der Workflow für den Vorgang in den nachfolgenden Arbeitsschritt über, der als eine Art `Wartezone` betrachtet werden kann. Dort verbleiben alle Vorgänge eines Batches zunächst und durchlaufen noch nicht die weiteren nachfolgenden Schritte. 

Wenn der Nutzer in dem Arbeitsschritt eines Vorgangs entscheidet, dass der Batch mit diesem Vorgang nun vollständig ist, kann er auf den Button für das `Schließen des Batches` klicken. Damit öffnet sich ein Dialogfenster, in dem ein Batch-Laufzettel heruntergeladen sowie der Batch geschlossen werden kann:

![Schließen eines Batches](screen5_de.png)

Durch das Schließen des Batches, wird der Arbeitsschritt für das Warten auf Vollständigkeit aller Vorgänge des Batches abgeschlossen und auch der der Arbeitsschritt des gerade geöffneten Arbeitsschritt wird abgeschlossen. Somit wechseln alle zu einem Batch zugewiesenen Vorgänge gleichzeitig in den nächsten nachfolgenden Arbeitsschritt, um dann gemeinsam weiter verarbeitet werden zu können.

![Weiterer Verlauf des Workflows](screen6_de.png)

## Konfiguration
Die Konfiguration des Plugins erfolgt in der Datei `plugin_intranda_step_batch_assignment.xml` wie hier aufgezeigt:

{{CONFIG_CONTENT}}

{{CONFIG_DESCRIPTION_PROJECT_STEP}}

Parameter               | Erläuterung
------------------------|------------------------------------
`batchWaitStep`         | Name desjenigen Arbeitsschrittes, in dem die Vorgänge verbleiben sollen, bis er letzte Vorgang zu dem Batch hinzugefügt wird
`property`              | Namen derjenigen Eigenschaften des Vorgangs, die beim Erzeugen des Batches bearbeitbar sein sollen und die für alle zugehörigen Vorgänge übernommen werden sollen
