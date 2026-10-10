IFT3913 – Tâche 2
Membres de l'équipe
- Zhexin Guo 20195027 – GitHub : FaustGzx

Liens
- Référentiel GitHub : https://github.com/FaustGzx/tika
- README de la tâche : https://github.com/FaustGzx/tika/blob/tache2/README.md

1. Classe sélectionnée
Nous avons choisi la classe :
org.apache.tika.digest.InputStreamDigester
dans le module :
tika-core
Cette classe possédait déjà un test original :
InputStreamDigesterTest
mais la couverture n'était pas complète.
Avec uniquement le test original, JaCoCo indiquait :
- couverture des instructions : 88 %
- couverture des branches : 66 %
- couverture des lignes utilisée par PITest : 25/28, soit 89 %
Plusieurs comportements n'étaient pas complètement testés, notamment :
- l'utilisation d'un Provider explicite ;
- le traitement d'un algorithme de digest invalide ;
- le cas où Metadata.CONTENT_LENGTH existe déjà ;
- le contenu exact du digest ;
- le comportement de rewind du TikaInputStream.
La classe possédait également plusieurs mutants vivants avec les tests originaux, ce qui en faisait une bonne candidate pour cette tâche.
2. Configuration de ChatUniTest
ChatUniTest a été ajouté au pipeline Maven avec le plugin :
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
Le modèle était exécuté localement avec Ollama.
Modèle utilisé :
codeqwen:v1.5-chat
La génération a été lancée sur la classe :
org.apache.tika.digest.InputStreamDigester
avec la commande :
mvn chatunitest:class -DselectClass=org.apache.tika.digest.InputStreamDigester
3. Résultat de la génération ChatUniTest
ChatUniTest a bien appelé le modèle local et a généré plusieurs propositions de tests.
Cependant, les tests générés ne compilaient pas directement.
Pour la méthode digest, ChatUniTest a effectué une génération initiale puis plusieurs tentatives automatiques de réparation. Les tentatives de compilation ont échoué malgré les réparations automatiques.
Les générations et les différentes tentatives ont été enregistrées dans :
C:\tmp\chatunitest-info\...
Comme aucun test final compilable n'a été produit automatiquement dans le répertoire du projet, nous avons récupéré le code généré dans les fichiers records.json.
Le code a ensuite été corrigé manuellement et placé dans :
src/test/java/org/apache/tika/digest/InputStreamDigesterChatUniTestTest.java
Les principales corrections nécessaires ont été :
1. adaptation de la création de TikaInputStream à l'API actuelle avec TikaInputStream.get(...) ;
2. suppression ou adaptation de mocks et appels incompatibles avec l'API actuelle ;
3. adaptation des clés de métadonnées utilisées par InputStreamDigester ;
4. correction de certains oracles et types d'exceptions attendus.
Après correction, quatre tests issus de la génération ChatUniTest s'exécutent correctement :
- testDigest
- testDigestWithOverriddenLength
- testDigestWithoutEncoder
- testDigestWithInvalidAlgorithm
Résultat :
Tests run: 4
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
4. Analyse qualitative des oracles générés par l'IA
Les oracles générés par ChatUniTest sont utiles, mais leur qualité est variable.
testDigest
Ce test vérifie :
- que la métadonnée contenant le digest est écrite ;
- que Metadata.CONTENT_LENGTH contient la valeur attendue.
L'oracle sur la longueur est précis :
assertEquals("4", metadata.get(Metadata.CONTENT_LENGTH));
Il permet notamment de détecter des mutations affectant le calcul de la longueur.
En revanche, l'encodeur utilisé retourne toujours la chaîne "encoded".
L'oracle :
assertEquals("encoded", metadata.get(key));
vérifie donc principalement que la valeur produite par l'encodeur est stockée dans les métadonnées. Il ne vérifie pas que les octets donnés à l'encodeur représentent réellement le bon SHA-256.
Cet oracle est donc utile pour certains comportements structurels, mais moins spécifique pour valider le digest lui-même.
testDigestWithOverriddenLength
Ce test est pertinent car il vérifie qu'une valeur de CONTENT_LENGTH déjà présente n'est pas remplacée.
Il couvre un chemin de branche qui n'était pas correctement testé auparavant.
testDigestWithoutEncoder
Ce test vérifie le comportement lorsqu'aucun encodeur n'est fourni.
Il permet de documenter le comportement actuel de l'implémentation, mais il est davantage lié à un détail d'implémentation qu'à un comportement métier important.
testDigestWithInvalidAlgorithm
Ce test vérifie qu'un algorithme inexistant provoque une IllegalArgumentException.
Cet oracle est pertinent puisqu'il vérifie explicitement le comportement d'erreur attendu.
Comparaison avec le test original
Le test original utilisait principalement :
assertNotNull(...)
Il vérifiait donc que certaines valeurs existaient, sans toujours vérifier leur contenu exact.
Les tests générés par l'IA introduisent plusieurs assertEquals et assertThrows, ce qui améliore la spécificité des oracles.
Cependant, certains oracles générés restent relativement faibles, en particulier lorsque l'encodeur retourne une valeur constante.
5. Analyse de mutation avec les tests originaux
PITest a été ajouté au projet pour analyser uniquement :
org.apache.tika.digest.InputStreamDigester
Avec uniquement le test original InputStreamDigesterTest, le résultat était :
Line Coverage: 25/28 = 89 %
Generated mutations: 12
Killed: 3
Mutation coverage: 25 %
Mutations with no coverage: 1
Test strength: 27 %
Le score de mutation initial est donc :
25 %
Ce faible score confirme que le test original ne détecte qu'une petite partie des modifications introduites artificiellement par PITest.
6. Analyse après ajout des tests ChatUniTest
Après ajout des quatre tests issus de ChatUniTest :
Line Coverage: 27/28 = 96 %
Generated mutations: 12
Killed: 8
Mutation coverage: 67 %
Mutations with no coverage: 1
Test strength: 73 %
Le score de mutation passe donc :
25 % → 67 %
Les tests issus de l'IA ont permis de tuer cinq mutants supplémentaires.
Mutants détectés grâce aux tests ChatUniTest
Ligne 112 – remplacement de l'addition par une soustraction
Mutation :
Replaced long addition with subtraction
Test détecteur :
InputStreamDigesterChatUniTestTest.testDigest
Le code normal ajoute le nombre d'octets lus à total.
Le mutant remplace cette addition par une soustraction. La valeur finale de CONTENT_LENGTH devient incorrecte.
L'oracle :
assertEquals("4", metadata.get(Metadata.CONTENT_LENGTH));
détecte donc la mutation.
Ligne 110 – condition de boucle inversée
Mutation :
negated conditional
Test détecteur :
InputStreamDigesterChatUniTestTest.testDigest
L'inversion de la condition modifie la lecture du flux. Le contenu et/ou la longueur finale ne correspondent plus aux valeurs attendues par le test.
Ligne 115 – suppression de setContentLength
Mutation :
removed call to InputStreamDigester::setContentLength
Test détecteur :
InputStreamDigesterChatUniTestTest.testDigest
Le test attend explicitement :
assertEquals("4", metadata.get(Metadata.CONTENT_LENGTH));
La suppression de l'appel empêche l'écriture de cette métadonnée et le test échoue.
Ligne 59 – condition sur CONTENT_LENGTH inversée
Mutation :
negated conditional
Test détecteur :
InputStreamDigesterChatUniTestTest.testDigest
Le Metadata utilisé par le test ne contient initialement aucune longueur.
La mutation empêche la branche correcte d'être exécutée et la longueur attendue n'est plus enregistrée.
Ligne 61 – suppression de l'écriture de CONTENT_LENGTH
Mutation :
removed call to Metadata::set
Test détecteur :
InputStreamDigesterChatUniTestTest.testDigest
Le test attend la valeur "4". Si l'appel à Metadata.set est supprimé, cette valeur est absente et l'oracle échoue.
7. Tests supplémentaires écrits manuellement
Après les tests ChatUniTest, plusieurs mutants étaient encore vivants ou non couverts.
Nous avons ajouté :
InputStreamDigesterManualTest
avec trois tests manuels.
testDigestUsesInputBytes
Intention
Vérifier que le digest enregistré correspond réellement aux octets du flux d'entrée.
Données choisies
La chaîne :
"test"
est utilisée car elle est courte, déterministe et permet de calculer facilement un SHA-256 de référence.
Oracle
Le résultat attendu est calculé indépendamment avec :
MessageDigest.getInstance("SHA-256").digest(input)
puis encodé en Base64.
Le résultat est ensuite comparé à la valeur enregistrée dans les métadonnées.
Cet oracle est plus spécifique que l'oracle généré par ChatUniTest utilisant un encodeur retournant toujours "encoded".
testDigestWithExplicitProvider
Intention
Forcer l'exécution de la branche :
MessageDigest.getInstance(algorithm, provider)
qui n'était pas couverte par les tests précédents.
Données choisies
Le Provider réel utilisé par Java pour SHA-256 est obtenu avec :
MessageDigest.getInstance("SHA-256").getProvider()
Cela évite d'utiliser un faux provider et teste un scénario réaliste.
Oracle
Le digest attendu est calculé explicitement avec le même algorithme et le même provider.
Ce test tue notamment :
- le mutant de la ligne 71 qui remplace la valeur de retour de newMessageDigest() par null ;
- le mutant de la ligne 111 qui supprime l'appel à MessageDigest.update(...).
testDigestRewindsInputStream
Intention
Vérifier qu'après l'appel à digest, le TikaInputStream peut être relu depuis son début.
Données choisies
La chaîne :
"rewind-test"
permet de comparer facilement les octets relus aux octets originaux.
Une variante utilisant un ByteArrayInputStream a également été testée afin de mieux vérifier le comportement de rewind.
Oracle
Après digest, tous les octets sont relus :
byte[] reread = tis.readAllBytes();
puis comparés aux données originales :
assertArrayEquals(input, reread);
Ce test tue le mutant de la ligne 118 qui supprime l'appel à :
tis.rewind();
8. Résultat final de l'analyse de mutation
Après ajout des tests manuels :
Line Coverage: 28/28 = 100 %
Generated mutations: 12
Killed: 11
Mutation coverage: 92 %
Mutations with no coverage: 0
Test strength: 92 %
Évolution :
Ensemble de tests	Line coverage	Mutants tués	Mutation score	Test strength
Tests originaux	89 %	3/12	25 %	27 %
Originaux + ChatUniTest	96 %	8/12	67 %	73 %
Originaux + ChatUniTest + manuels	100 %	11/12	92 %	92 %


9. Mutant restant
Un seul mutant reste vivant.
Ligne 104 :
tis.enableRewind();
Mutation :
removed call to TikaInputStream::enableRewind
Nous avons ajouté et renforcé le test testDigestRewindsInputStream, notamment avec un ByteArrayInputStream.
Cependant, même sans l'appel explicite à enableRewind(), le comportement observable reste identique dans nos scénarios : le flux peut toujours être rembobiné avec succès.
Le mutant reste donc vivant.
Il peut être considéré comme potentiellement équivalent dans le contexte des entrées testées, car la suppression de cet appel ne produit pas de différence observable avec les scénarios que nous avons pu construire.
Nous ne considérons donc pas qu'ajouter un oracle artificiel dépendant uniquement d'un détail interne de l'implémentation améliorerait la qualité fonctionnelle de la suite de tests.
10. Résultat d'exécution
Les tests finaux ont été exécutés ensemble avec Maven :
Tests run: 8
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
La vérification Checkstyle s'exécute également correctement.
Les fichiers ajoutés sont :
src/test/java/org/apache/tika/digest/InputStreamDigesterChatUniTestTest.java
src/test/java/org/apache/tika/digest/InputStreamDigesterManualTest.java
Le fichier pom.xml a également été modifié pour intégrer ChatUniTest et PITest.
Conclusion
Les tests originaux obtenaient un score de mutation de seulement 25 %.
Les tests générés avec ChatUniTest ont amélioré ce score à 67 %, montrant que l'IA a permis d'identifier plusieurs comportements non suffisamment testés.
Les tests manuels ajoutés après analyse des mutants survivants ont ensuite augmenté le score à 92 % et la couverture des lignes à 100 %.
L'expérience montre que ChatUniTest peut améliorer significativement une suite de tests existante, mais que les tests générés nécessitent encore une validation humaine :
- certains tests ne compilaient pas automatiquement ;
- plusieurs corrections liées à l'API du projet ont été nécessaires ;
- certains oracles produits par l'IA étaient moins spécifiques que des oracles écrits manuellement ;
- l'analyse de mutation a permis d'identifier précisément les faiblesses restantes et de guider l'écriture des tests supplémentaires.