# fsp 
Connecteur permettant de réaliser le suivi périodique des projets.

## Fonctionnement 
### Fichier de paramètres
Le fichier de configuration est décrit par le schéma XSD schemas/parametres.xsd
### Source
Le connecteur récupère la liste des projets actifs sur la page désignée par le paramètre *projectListPath*. Chaque page est ensuite traitée afin de récupérer l'identifiant de projet Chronos [^1].

Chronos est interrogé pour récupérer :

* la date de la FSP [^2]
* la météo [^3]
* les annonces et alertes [^4]
* le temps consommé sur les projets

Si la date de la FSP est plus récente que celle de l'historique, ce dernier est mis à jour. La météo, les annonces et les alertes aussi. Il faut donc saisir les alertes/annonces avant de mettre à jour la date de la FSP pour que tout soit pris en compte lors du rafraichissement des données.

### Historique
Le connecteur dispose de l'historique des projets au format XML :

* un fichier par projet
* un élément par entrée d'historique
* un élément par annonce/alerte au sein de chaque entrée d'historique

L'historique a été créé à partir du contenu des FSPv1 (si existant) et des informations Chronos.

Le nom du fichier est l'identifiant wiki du projet (nom du namespace).

### Calcul du temps consommé
Pour chaque projet, l'ensemble des tâches est récupéré (ouvertes et fermées). Pour chaque tâche, les entrées de temps sont comptabilisées.

Les doublons (sous tâches, sous projets, ...) ne sont pas pris en compte. Le résultat est cohérent avec celui affiché dans l'interface web.

Seules les entrées de temps plus anciennes que la FSP sont prises en compte.
### Modifications dans la liste des projets
Les modifications dans la liste des projets sont identifiées grâce à un fichier d'historique XML contenant la liste des projets ainsi que la phase. Les projets ont pour identifiant la page wiki qui leur est dédiée. Ainsi, les renommages de projet sont aussi repérés.

Si une modification est détectée, un mail est envoyé aux gestionnaires du portefeuilles des projets pour leur permettre de suivre le cycle de vie des projets.

## Compilation
### Dépendances

Le fichier repose sur un [fork de redmine-java-api](https://github.com/nilseckert/redmine-java-api.git) gérant les *custom fields*.

    git clone https://github.com/nilseckert/redmine-java-api.git 
    mvn install

### Création de l'archive
    mvn package -Dconfig="path/to/config/file"

Tout est pris en compte dans la configuration Maven

- le schéma du fichier de configuration est utilisé pour générer les classes xmlbeans
- le plugin maven-jar-plugin est utilisé pour repackager les dépendances
- une archive autonome *fsp-X.X.jar* est créée.

A noter que le fichier de configuration est nécessaire pour tester les possibilités d'envoi de mail

## Usage
    java -Xmx1024m -jar target/fsp-X.X.jar /chemin/fichier/configuration.xml

## Documentations

* [API REST Redmine](http://www.redmine.org/projects/redmine/wiki/Rest_api)
* [API Java Redmine](https://github.com/taskadapter/redmine-java-api/)
* [Dokuwiki XML/RPC API](https://www.dokuwiki.org/devel:xmlrpc)

***
[^1]: Projet → Configuration → Informations → Identifiant
[^2]: Projet → Configuration → Informations → Date mise à jour FSP
[^3]: Projet → Configuration → Informations → Météo
[^4]: Projet → Configuration → Modules → Publication d'annonces
