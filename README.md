# IFT3913 – Tâche 2

## Membre

- **Zhexin Guo (20195027)** — GitHub : [`FaustGzx`](https://github.com/FaustGzx)

## Liens

- **Référentiel GitHub :** https://github.com/FaustGzx/tika
- **README de la tâche :** https://github.com/FaustGzx/tika/blob/tache2/README.md

---

## Résumé des résultats

| Étape | Couverture des lignes | Mutants tués | Score de mutation | Test strength |
|---|---:|---:|---:|---:|
| Tests originaux | 89 % | 3 / 12 | 25 % | 27 % |
| + tests ChatUniTest | 96 % | 8 / 12 | 67 % | 73 % |
| + tests manuels | **100 %** | **11 / 12** | **92 %** | **92 %** |

---

## 1. Classe sélectionnée

Nous avons choisi la classe :

```text
org.apache.tika.digest.InputStreamDigester
```

dans le module :

```text
tika-core
```

Cette classe possédait déjà un test original :

```text
InputStreamDigesterTest
```

mais la couverture n'était pas complète.

Avec uniquement le test original, JaCoCo indiquait :

- couverture des instructions : **88 %** ;
- couverture des branches : **66 %** ;
- couverture des lignes utilisée par PITest : **25/28, soit 89 %**.

Plusieurs comportements n'étaient pas complètement testés, notamment :

- l'utilisation d'un `Provider` explicite ;
- le traitement d'un algorithme de digest invalide ;
- le cas où `Metadata.CONTENT_LENGTH` existe déjà ;
- le contenu exact du digest ;
- le comportement de rewind du `TikaInputStream`.

La classe possédait également plusieurs mutants vivants avec les tests originaux, ce qui en faisait une bonne candidate pour cette tâche.

---

## 2. Configuration de ChatUniTest

ChatUniTest a été ajouté au pipeline Maven avec le plugin suivant :

```xml
<plugin>
  <groupId>io.github.zju-aces-ise</groupId>
  <artifactId>chatunitest-maven-plugin</artifactId>
  <version>2.1.1</version>
  <configuration>
    <apiKeys>xxx</apiKeys>
    <model>codeqwen:v1.5-chat</model>
    <url>http://localhost:11434/v1/chat/completions</url>
    <testNumber>3</testNumber>
    <maxRounds>3</maxRounds>
    <thread>false</thread>
    <stopWhenSuccess>true</stopWhenSuccess>
  </configuration>
</plugin>
```

Le modèle était exécuté localement avec **Ollama**.

**Modèle utilisé :**

```text
codeqwen:v1.5-chat
```

La génération a été lancée sur la classe `InputStreamDigester` avec :

```bash
mvn chatunitest:class -DselectClass=org.apache.tika.digest.InputStreamDigester
```

---

## 3. Résultat de la génération ChatUniTest

ChatUniTest a bien appelé le modèle local et a généré plusieurs propositions de tests.

Cependant, les tests générés ne compilaient pas directement. Pour la méthode `digest`, ChatUniTest a effectué une génération initiale puis plusieurs tentatives automatiques de réparation, mais les compilations ont continué à échouer.

Les générations et différentes tentatives ont été enregistrées dans :

```text
C:\tmp\chatunitest-info\...
```

Comme aucun test final compilable n'a été produit automatiquement dans le répertoire du projet, nous avons récupéré le code généré dans les fichiers `records.json`.

Le code a ensuite été corrigé manuellement et placé dans :

```text
src/test/java/org/apache/tika/digest/InputStreamDigesterChatUniTestTest.java
```

### Corrections principales

1. Adaptation de la création de `TikaInputStream` à l'API actuelle avec `TikaInputStream.get(...)`.
2. Suppression ou adaptation de mocks et appels incompatibles avec l'API actuelle.
3. Adaptation des clés de métadonnées utilisées par `InputStreamDigester`.
4. Correction de certains oracles et types d'exceptions attendus.

Après correction, quatre tests issus de la génération ChatUniTest s'exécutent correctement :

- `testDigest`
- `testDigestWithOverriddenLength`
- `testDigestWithoutEncoder`
- `testDigestWithInvalidAlgorithm`

Résultat :

```text
Tests run: 4
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

---

## 4. Analyse qualitative des oracles générés par l'IA

### `testDigest`

Ce test vérifie :

- que la métadonnée contenant le digest est écrite ;
- que `Metadata.CONTENT_LENGTH` contient la valeur attendue.

L'oracle sur la longueur est précis :

```java
assertEquals("4", metadata.get(Metadata.CONTENT_LENGTH));
```

Il permet notamment de détecter des mutations affectant le calcul de la longueur.

En revanche, l'encodeur utilisé retourne toujours la chaîne `"encoded"` :

```java
assertEquals("encoded", metadata.get(key));
```

Cet oracle vérifie donc principalement que la valeur produite par l'encodeur est stockée dans les métadonnées. Il ne vérifie pas que les octets donnés à l'encodeur représentent réellement le bon SHA-256.

### `testDigestWithOverriddenLength`

Ce test vérifie qu'une valeur de `CONTENT_LENGTH` déjà présente n'est pas remplacée. Il couvre un chemin de branche qui n'était pas correctement testé auparavant.

### `testDigestWithoutEncoder`

Ce test vérifie le comportement lorsqu'aucun encodeur n'est fourni. Il documente le comportement actuel de l'implémentation, mais reste davantage lié à un détail d'implémentation qu'à un comportement métier important.

### `testDigestWithInvalidAlgorithm`

Ce test vérifie qu'un algorithme inexistant provoque une `IllegalArgumentException`. Cet oracle est pertinent puisqu'il vérifie explicitement le comportement d'erreur attendu.

### Comparaison avec le test original

Le test original utilisait principalement :

```java
assertNotNull(...)
```

Il vérifiait donc que certaines valeurs existaient, sans toujours vérifier leur contenu exact.

Les tests générés par l'IA introduisent plusieurs `assertEquals` et `assertThrows`, ce qui améliore la spécificité des oracles.

Cependant, certains oracles restent relativement faibles, notamment lorsque l'encodeur retourne une valeur constante.

---

## 5. Analyse de mutation avec les tests originaux

PITest a été ajouté au projet pour analyser uniquement :

```text
org.apache.tika.digest.InputStreamDigester
```

Avec uniquement le test original `InputStreamDigesterTest`, le résultat était :

```text
Line Coverage: 25/28 = 89 %
Generated mutations: 12
Killed: 3
Mutation coverage: 25 %
Mutations with no coverage: 1
Test strength: 27 %
```

Le score de mutation initial est donc de **25 %**.

Ce faible score confirme que le test original ne détecte qu'une petite partie des modifications introduites artificiellement par PITest.

---

## 6. Analyse après ajout des tests ChatUniTest

Après ajout des quatre tests issus de ChatUniTest :

```text
Line Coverage: 27/28 = 96 %
Generated mutations: 12
Killed: 8
Mutation coverage: 67 %
Mutations with no coverage: 1
Test strength: 73 %
```

Le score de mutation passe donc de **25 % à 67 %**.

Les tests issus de l'IA ont permis de tuer **cinq mutants supplémentaires**.

### Mutants détectés grâce aux tests ChatUniTest

| Ligne | Mutation | Test détecteur | Pourquoi le test échoue |
|---:|---|---|---|
| 112 | `Replaced long addition with subtraction` | `testDigest` | La longueur finale devient incorrecte. |
| 110 | `negated conditional` | `testDigest` | La lecture du flux est modifiée. |
| 115 | suppression de `setContentLength` | `testDigest` | `CONTENT_LENGTH` n'est plus écrit. |
| 59 | `negated conditional` | `testDigest` | La branche d'écriture de la longueur n'est plus correcte. |
| 61 | suppression de `Metadata.set` | `testDigest` | La longueur attendue n'est plus présente. |

Exemple d'oracle utilisé :

```java
assertEquals("4", metadata.get(Metadata.CONTENT_LENGTH));
```

---

## 7. Tests supplémentaires écrits manuellement

Après les tests ChatUniTest, plusieurs mutants étaient encore vivants ou non couverts.

Nous avons ajouté :

```text
InputStreamDigesterManualTest
```

avec trois tests manuels.

### `testDigestUsesInputBytes`

**Intention :** vérifier que le digest enregistré correspond réellement aux octets du flux d'entrée.

**Données choisies :** la chaîne `"test"`, courte et déterministe.

**Oracle :**

```java
MessageDigest.getInstance("SHA-256").digest(input)
```

Le résultat est ensuite encodé en Base64 et comparé à la valeur enregistrée dans les métadonnées.

Cet oracle est plus spécifique que l'oracle généré par ChatUniTest utilisant un encodeur retournant toujours `"encoded"`.

---

### `testDigestWithExplicitProvider`

**Intention :** forcer l'exécution de la branche :

```java
MessageDigest.getInstance(algorithm, provider)
```

**Données choisies :**

```java
MessageDigest.getInstance("SHA-256").getProvider()
```

Cela permet d'utiliser un `Provider` réel et de tester un scénario réaliste.

**Oracle :** le digest attendu est calculé explicitement avec le même algorithme et le même provider.

Ce test tue notamment :

- le mutant de la ligne 71 qui remplace la valeur de retour de `newMessageDigest()` par `null` ;
- le mutant de la ligne 111 qui supprime l'appel à `MessageDigest.update(...)`.

---

### `testDigestRewindsInputStream`

**Intention :** vérifier qu'après l'appel à `digest`, le `TikaInputStream` peut être relu depuis son début.

**Données choisies :** la chaîne `"rewind-test"`.

Une variante utilisant un `ByteArrayInputStream` a également été testée afin de mieux vérifier le comportement de rewind.

**Oracle :**

```java
byte[] reread = tis.readAllBytes();
assertArrayEquals(input, reread);
```

Ce test tue le mutant de la ligne 118 qui supprime l'appel à :

```java
tis.rewind();
```

---

## 8. Résultat final de l'analyse de mutation

Après ajout des tests manuels :

```text
Line Coverage: 28/28 = 100 %
Generated mutations: 12
Killed: 11
Mutation coverage: 92 %
Mutations with no coverage: 0
Test strength: 92 %
```

### Évolution

| Ensemble de tests | Line coverage | Mutants tués | Mutation score | Test strength |
|---|---:|---:|---:|---:|
| Tests originaux | 89 % | 3 / 12 | 25 % | 27 % |
| Originaux + ChatUniTest | 96 % | 8 / 12 | 67 % | 73 % |
| Originaux + ChatUniTest + manuels | **100 %** | **11 / 12** | **92 %** | **92 %** |

---

## 9. Mutant restant

Un seul mutant reste vivant.

**Ligne 104 :**

```java
tis.enableRewind();
```

**Mutation :**

```text
removed call to TikaInputStream::enableRewind
```

Nous avons ajouté et renforcé le test `testDigestRewindsInputStream`, notamment avec un `ByteArrayInputStream`.

Cependant, même sans l'appel explicite à `enableRewind()`, le comportement observable reste identique dans nos scénarios : le flux peut toujours être rembobiné avec succès.

Le mutant reste donc vivant. Il peut être considéré comme potentiellement équivalent dans le contexte des entrées testées, car sa suppression ne produit pas de différence observable avec les scénarios construits.

---

## 10. Résultat d'exécution

Les tests finaux ont été exécutés ensemble avec Maven :

```text
Tests run: 8
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

La vérification Checkstyle s'exécute également correctement.

### Fichiers ajoutés

```text
src/test/java/org/apache/tika/digest/InputStreamDigesterChatUniTestTest.java
src/test/java/org/apache/tika/digest/InputStreamDigesterManualTest.java
```

Le fichier `pom.xml` a également été modifié pour intégrer ChatUniTest et PITest.

---

## Conclusion

Les tests originaux obtenaient un score de mutation de seulement **25 %**.

Les tests générés avec ChatUniTest ont amélioré ce score à **67 %**, montrant que l'IA a permis d'identifier plusieurs comportements insuffisamment testés.

Les tests manuels ajoutés après analyse des mutants survivants ont ensuite augmenté le score à **92 %** et la couverture des lignes à **100 %**.

L'expérience montre que ChatUniTest peut améliorer significativement une suite de tests existante, mais que les tests générés nécessitent encore une validation humaine :

- certains tests ne compilaient pas automatiquement ;
- plusieurs corrections liées à l'API du projet ont été nécessaires ;
- certains oracles produits par l'IA étaient moins spécifiques que des oracles écrits manuellement ;
- l'analyse de mutation a permis d'identifier précisément les faiblesses restantes et de guider l'écriture de tests supplémentaires.
