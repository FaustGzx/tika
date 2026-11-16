# IFT3913 — Tâche 3 : automatisation des tests de sécurité

**Cours :** IFT3913 — Qualité du logiciel (automne 2026)  
**Équipe :** FaustGzx **  
**Étude de cas :** Apache Tika (fork du dépôt `umontreal-diro/tika`)  
**Dépôt de travail :** https://github.com/FaustGzx/tika  
**Branche de travail :** `tache3`

## 1. Objectif et périmètre

Notre objectif est d'intégrer une analyse statique de sécurité avec **Semgrep** dans GitHub Actions, pour que l'introduction d'une nouvelle vulnérabilité puisse faire échouer la vérification CI avant l'exécution des tests Maven. Nous avons également introduit sept exemples de vulnérabilités de types différents dans **`tika-core`**, comparé les rapports Semgrep et créé une action réutilisable qui réalise un *rickroll* après un échec de test.

Les exemples sont regroupés dans le fichier [`tika-core/src/main/java/org/apache/tika/Tache3SecurityExamples.java`](https://github.com/FaustGzx/tika/blob/tache3/tika-core/src/main/java/org/apache/tika/Tache3SecurityExamples.java). Nous avons choisi `tika-core` parce qu'il appartient au périmètre du cas d'étude et peut être compilé et testé par Maven. Nous avons choisi une classe dédiée pour isoler les exemples pédagogiques du traitement normal des documents Tika. **Ces méthodes sont volontairement vulnérables, ne sont pas utilisées dans le fonctionnement normal de Tika et ne doivent pas être réutilisées en production.**

## 2. Modifications de GitHub Actions

### 2.1 Fichiers et responsabilités

| Fichier | Rôle |
| --- | --- |
| [`.github/workflows/semgrep.yml`](https://github.com/FaustGzx/tika/blob/tache3/.github/workflows/semgrep.yml) | Analyse incrémentale de sécurité et échec en présence de nouveaux résultats Semgrep. |
| [`.github/workflows/semgrep-report.yml`](https://github.com/FaustGzx/tika/blob/tache3/.github/workflows/semgrep-report.yml) | Analyse complète du dépôt et publication du rapport JSON comme artefact. |
| [`.github/workflows/main-jdk17-build.yml`](https://github.com/FaustGzx/tika/blob/tache3/.github/workflows/main-jdk17-build.yml) | Exécution du contrôle Semgrep avant la compilation et les tests Maven sous Java 17. |
| [`.github/actions/rickroll/action.yml`](https://github.com/FaustGzx/tika/blob/tache3/.github/actions/rickroll/action.yml) | Action composite réutilisable appelée après un échec réel de test. |

### 2.2 Conception du contrôle de sécurité

Le workflow `semgrep.yml` peut être déclenché lors d'un `push`, d'une `pull_request`, manuellement (`workflow_dispatch`) ou comme workflow réutilisable (`workflow_call`). Il récupère l'historique Git avec `actions/checkout` et `fetch-depth: 0` pour permettre la comparaison avec une version de référence.

Le contrôle utilise les configurations `p/java` et `p/security-audit`, ainsi que la règle supplémentaire `r/java.lang.security.audit.command-injection-process-builder.command-injection-process-builder`. L'option `--baseline-commit` demande à Semgrep de ne retenir que les problèmes introduits par rapport au commit de référence. L'option `--error` retourne un code d'échec lorsqu'un résultat bloquant est trouvé.

Pour un `push`, le workflow compare normalement avec le commit précédent (`github.event.before`). Pour une `pull_request`, il prend le SHA de la branche de base. Ce choix réduit le bruit provoqué par les problèmes déjà présents. Il implique cependant qu'un résultat introduit lors d'un ancien commit de la branche ne réapparaît pas nécessairement à chaque `push` suivant. Pour l'ensemble des vulnérabilités, le workflow de rapport complet est donc utilisé en complément.

Le workflow Maven JDK 17 a été modifié pour utiliser trois jobs successifs :

1. **`security`** appelle le workflow Semgrep réutilisable.
2. **`build`** possède `needs: security` : la compilation et les tests Maven ne démarrent que si Semgrep réussit.
3. **`e2e-tests`** possède `needs: build` : les tests de bout en bout dépendent de la réussite du job de construction.

Ainsi, un échec de sécurité bloque la suite du pipeline. Dans notre environnement de validation, le job Maven a été limité à `tika-core` et ses dépendances avec `mvn -pl tika-core -am clean apache-rat:check verify -Pci -B`. Cela vérifie le module que nous avons modifié, mais **ne constitue pas une validation de tous les 124 modules du dépôt Tika**.

### 2.3 Validation du workflow

**Cas normal.** Une exécution du workflow `main jdk17 build` a réussi avec les trois jobs en vert : `Semgrep Security Gate`, `build (17)` et `e2e-tests (17)`. Cela confirme le fonctionnement de la chaîne de dépendances en l'absence de nouvelle alerte sur le commit concerné.

**Cas d'une nouvelle vulnérabilité.** Lors de l'introduction successive des exemples SHA-1, AES-ECB, socket non chiffrée, XXE, vérificateur de nom d'hôte non sécurisé et conversion hexadécimale incorrecte, le workflow Semgrep indépendant a affiché `1 Code Finding`, puis `Findings: 1 (1 blocking)` et s'est terminé avec le code de sortie 1. Le septième type, l'injection de commandes, a été confirmé dans le rapport complet après l'ajout d'une règle dédiée.

**Limite de validation à préciser.** Nous avons vérifié les échecs Semgrep lors des ajouts de vulnérabilités et le bon enchaînement `security → build → e2e-tests`. Une exécution d'essai montrant **le job `security` en échec et le job `build` non exécuté dans ce même workflow intégré** doit encore être ajoutée aux preuves, si elle est réalisée avant le rendu.

## 3. Sept vulnérabilités introduites

Les sept résultats suivants ont été vérifiés dans le rapport JSON complet après modification. Les numéros de ligne correspondent à **l'instantané du rapport** et peuvent changer après des modifications de formatage.

| # | Type / CWE | Méthode dans `Tache3SecurityExamples.java` | Règle Semgrep (suffixe) | Ligne du rapport | Sévérité |
| --- | --- | --- | --- | ---: | --- |
| 1 | SHA-1 faible — CWE-328 | `insecureSha1` | `use-of-sha1` | 43 | WARNING |
| 2 | Mode AES-ECB — CWE-327 | `insecureEcbCipher` | `use-of-aes-ecb` | 68 | WARNING |
| 3 | Socket non chiffrée — CWE-319 | `insecureSocket` | `unencrypted-socket` | 77 | WARNING |
| 4 | XML External Entity (XXE) — CWE-611 | `insecureXmlParse` | `documentbuilderfactory-disallow-doctype-decl-missing` | 91 | ERROR |
| 5 | Validation TLS du nom d'hôte désactivée — CWE-295 | `insecureHostnameVerifier` | `insecure-hostname-verifier` | 98 | WARNING |
| 6 | Conversion hexadécimale incorrecte — CWE-704 | `insecureHexConversion` | `bad-hexa-conversion` | 107 | WARNING |
| 7 | Injection de commandes — CWE-78 | `insecureCommandExecution` | `command-injection-process-builder` | 128 | ERROR |

### 3.1 Utilisation de SHA-1 (CWE-328)

Le code utilise `MessageDigest.getInstance("SHA-1")`. SHA-1 n'offre plus une résistance suffisante aux collisions pour un usage cryptographique sensible, en particulier les signatures ou les vérifications d'intégrité de sécurité. Semgrep signale `use-of-sha1`. Une alternative adaptée à un hachage général est SHA-256 ; pour des mots de passe, il faut utiliser un algorithme spécialisé comme PBKDF2, bcrypt ou Argon2.

### 3.2 Chiffrement AES en mode ECB (CWE-327)

La méthode utilise `Cipher.getInstance("AES/ECB/PKCS5Padding")`. Le mode ECB chiffre indépendamment les blocs identiques et peut révéler des motifs dans les données. Semgrep signale `use-of-aes-ecb`. Un mode de chiffrement authentifié tel que AES-GCM, avec nonce correctement généré, est préférable.

### 3.3 Communication par socket sans chiffrement (CWE-319)

La méthode crée une connexion avec `new Socket(host, port)` puis envoie une chaîne considérée sensible. Les octets peuvent circuler en clair. Semgrep signale `unencrypted-socket`. Pour protéger la communication, il faudrait employer TLS, par exemple avec `SSLSocketFactory`, et vérifier correctement l'identité du serveur.

### 3.4 Analyse XML vulnérable aux entités externes (CWE-611)

La méthode utilise `DocumentBuilderFactory.newInstance()` puis `newDocumentBuilder().parse(input)` sans désactiver les déclarations DOCTYPE et les entités externes. Avec un XML non fiable, cela peut permettre l'accès à des ressources locales ou réseau. Semgrep signale `documentbuilderfactory-disallow-doctype-decl-missing`. La prévention consiste à désactiver les fonctionnalités XML dangereuses avant le parsing.

### 3.5 Vérificateur TLS de nom d'hôte non sécurisé (CWE-295)

L'implémentation de `HostnameVerifier.verify(...)` retourne toujours `true`. Elle accepte donc des noms d'hôte qui ne correspondent pas à l'identité attendue du serveur et facilite une attaque de l'homme du milieu. Semgrep signale `insecure-hostname-verifier`. La solution est de conserver la vérification standard du certificat et du nom d'hôte.

### 3.6 Conversion incorrecte d'un condensat en hexadécimal (CWE-704)

Le code calcule un condensat SHA-256 mais transforme chaque octet avec `Integer.toHexString(b & 0xFF)` sans compléter les valeurs à deux caractères. La représentation perd des zéros initiaux et peut devenir ambiguë. Semgrep signale `bad-hexa-conversion`. La solution est de produire exactement deux chiffres hexadécimaux par octet, par exemple avec un formateur approprié.

### 3.7 Injection de commandes système (CWE-78)

La méthode utilise `ProcessBuilder` avec `bash -c` et une commande fournie par un paramètre non fiable. Si elle était appelée avec une valeur contrôlée par un attaquant, cela permettrait l'exécution de commandes arbitraires. Semgrep signale `command-injection-process-builder`. Il faut éviter l'exécution par un interpréteur de commandes et n'accepter que des opérations explicitement autorisées, avec des arguments contrôlés.

**Essai non retenu :** un exemple d'injection SQL a été testé, mais n'a pas été identifié par les règles utilisées dans notre configuration ; il a été retiré et n'est **pas** compté parmi les sept vulnérabilités validées.

## 4. Comparaison des rapports Semgrep avant / après

Deux scans complets ont été conservés sous forme de rapports JSON produits par `semgrep-report.yml` :

- **Avant les modifications :** artefact lié au commit `011deb5d3a417ec581eab63254059588ce75d858`.
- **Après l'introduction des exemples et de la règle supplémentaire :** artefact lié au commit `64c8d191741c4f68f10c07e305e599316978d108`.

| Indicateur | Avant | Après |
| --- | ---: | ---: |
| Fichiers analysés | 2 159 | 2 160 |
| Résultats Semgrep | 19 | 42 |
| Règles distinctes ayant produit un résultat | 13 | 14 |
| Résultats de sévérité `ERROR` | 6 | 24 |
| Résultats de sévérité `WARNING` | 13 | 18 |

**Interprétation :** la différence brute est de 23 résultats, mais elle **ne représente pas 23 vulnérabilités introduites**. Le scan final utilise une règle supplémentaire de détection d'injection de commandes. Cette règle repère 17 emplacements, dont **un dans notre nouvelle classe** et **16 dans des fichiers déjà présents**. Les six autres exemples ajoutent six résultats. Le rapport final identifie donc **exactement sept résultats correspondant à nos sept exemples**, dans `Tache3SecurityExamples.java`.

La comparaison des totaux n'est pas strictement à règles constantes puisque la configuration de Semgrep a changé. Une comparaison strictement homogène nécessiterait de relancer le scan du commit initial avec exactement les mêmes règles que le scan final. Un avertissement de *parsing partiel* existe également dans `PipesParsingHelper.java` lors des deux scans ; cette limite est indépendante de la classe ajoutée.

## 5. Rickroll déclenché par un échec de test

Nous avons créé une action composite réutilisable dans `.github/actions/rickroll/action.yml`. Elle affiche le message « You've been Rickrolled! » et un lien vers la vidéo dans les journaux GitHub Actions et dans le résumé de l'exécution.

Dans le workflow JDK 17, le traitement est le suivant :

1. Le job Maven exécute les tests.
2. Si Maven échoue, une étape lit les rapports XML de Surefire/Failsafe et vérifie qu'un test possède une valeur `failures` ou `errors` supérieure à zéro.
3. Si un véritable échec de test est trouvé, le workflow invoque `./.github/actions/rickroll`.
4. Le statut d'échec initial du job Maven reste inchangé.

**Validation observée :** nous avons temporairement ajouté `Tache3RickrollTest` dans `tika-core/src/test/java/org/apache/tika/` avec un test JUnit qui échoue volontairement. Maven a alors signalé `Tests run: 750, Failures: 1, Errors: 0, Skipped: 2`. La section `Rickroll on test failure` a réellement exécuté l'action et affiché le message ainsi que l'URL. Ensuite, **le test temporaire a été supprimé** pour ne pas laisser un échec intentionnel dans la version finale.

Cette validation montre que le rickroll est bien conditionné à un échec de **test**, et pas seulement à une erreur de compilation ou à un résultat Semgrep.

## 6. Difficultés rencontrées et choix techniques

- **Formatage Java :** les vérifications Checkstyle puis Spotless ont initialement échoué sur le nouvel exemple Java. Nous avons corrigé l'en-tête de licence, les espaces et l'ordre des imports, sans modifier les vulnérabilités pédagogiques.
- **Intégration S3 hors périmètre :** le build Maven complet a échoué dans `tika-pipes-s3-integration-tests` au moment du téléchargement du conteneur `quay.io/minio/minio:latest`, avec une erreur HTTP `401 UNAUTHORIZED`. Nous avons limité la validation principale à `tika-core` et ses dépendances pour ne pas confondre cet incident externe avec les changements de sécurité. Il reste nécessaire de distinguer ce contrôle ciblé d'une validation complète du projet Tika.
- **Analyse incrémentale :** la commande `--baseline-commit` compare uniquement les problèmes apparus depuis le commit de référence. Une nouvelle règle ajoutée après les exemples n'entraîne donc pas forcément un échec lors du commit qui ne modifie que le workflow ; un scan complet est nécessaire pour inventorier tous les résultats.

## 7. Reproduction et preuves

Les fichiers de configuration peuvent être consultés dans le [dépôt Tika, branche `tache3`](https://github.com/FaustGzx/tika/tree/tache3).

Pour reconstruire le module modifié :

```bash
mvn -pl tika-core -am clean apache-rat:check verify -Pci -B
```

Pour exécuter une analyse complète avec les mêmes familles de règles que le rapport final :

```bash
semgrep scan \
  --config p/java \
  --config p/security-audit \
  --config r/java.lang.security.audit.command-injection-process-builder.command-injection-process-builder \
  --json --output semgrep-report.json .
```

- [Historique des analyses Semgrep](https://github.com/FaustGzx/tika/actions/workflows/semgrep.yml)
- [Historique des rapports complets Semgrep](https://github.com/FaustGzx/tika/actions/workflows/semgrep-report.yml)
- [Historique du build Java 17](https://github.com/FaustGzx/tika/actions/workflows/main-jdk17-build.yml)
- **Exécution de l'échec JUnit + rickroll :** [ajouter ici le lien direct vers le run GitHub Actions].
- **Exécution finale après suppression du test temporaire :** [ajouter ici le lien direct et confirmer le statut vert].
- **Validation supplémentaire du blocage `security → build` sur un nouveau commit vulnérable :** [ajouter ici le lien vers le run si ce scénario a été réalisé].

## 8. Utilisation de l'intelligence artificielle

Nous avons utilisé **ChatGPT (OpenAI)** comme assistance pour comprendre les consignes, proposer des exemples de code et des configurations GitHub Actions, analyser les journaux d'erreurs et préparer une première version de cette documentation. Les exemples de vulnérabilités ont été vérifiés avec les résultats réels de Semgrep, et les étapes CI ont été testées sur notre fork. Les décisions, vérifications et soumissions finales restent sous la responsabilité sur moi.

## 9. Conclusion

Le travail montre comment une analyse Semgrep peut être intégrée à GitHub Actions avant la construction Maven, comment identifier sept catégories de vulnérabilités dans le code Java de Tika, et comment déclencher une action humoristique réutilisable lors d'un échec réel de test. Les résultats et leurs limites sont documentés afin de distinguer les vulnérabilités volontairement introduites, les problèmes préexistants du dépôt et les problèmes d'environnement des tests.
