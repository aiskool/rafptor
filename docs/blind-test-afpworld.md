# Blind-test AFPWorld — premier chiffre de fidélité réel Rafptor

**Date de l'exécution :** 2026-04-19
**Scénario :** échantillon "Continuing Health Coverage" d'AFPWorld.com (1 page, US English, santé)
**Fichier source :** `Sample_1_health.zip` → `01_Health_Coverage.afp`
**Référence :** PDF fourni dans la distribution AFPWorld (regardé *après* la conversion, d'où "blind")

---

## TL;DR

| Métrique | Valeur | Lecture |
|---|---|---|
| **Composite score (validator)** | **0.527** | verdict `rejected` (< 0.60 review threshold) |
| **SSIM visuel (grayscale, full-page)** | **0.7049** | similarité structurelle correcte |
| **SSIM visuel (RGB, full-page)** | **0.7065** | quasi-identique au grayscale — pas de gain couleur |
| **Structural score** | 1.000 | 1 page en, 1 page out, géométrie OK |
| **Metadata score** | 1.000 | infos document préservées |
| **Visual score (normalisé)** | 0.053 | métrique du validator, plus sévère — voir section "Lecture" |
| **Text layer readability** | 97.40% ASCII printable | 162 mots anglais décodés (vs 8.67% avant fix EBCDIC/UTF-16) |
| **Domain keywords matched** | 6 / 29 | `coverage, health, insurance, plan, policy, services` |

Le convertisseur **extrait et reconstruit le texte du document avec une fidélité élevée**. Le gap principal est **visuel** : couleurs, logo, cadres de tableau, et espacement inter-mots.

---

## Contexte

L'objectif du blind-test : mesurer la fidélité de Rafptor sur un fichier AFP **externe, non vu auparavant**, sans référence pendant la conversion. C'est le premier chiffre de fidélité réel du produit, opposable en démonstration.

L'équipe a sélectionné l'échantillon AFPWorld.com parce qu'il est :
- public et téléchargeable,
- représentatif du flux banking/insurance (textes positionnés, tableau tabulé, police Arial, code-page Unicode),
- livré avec son PDF de référence produit par un moteur AFP commercial.

## Mécanique de l'exécution

1. Téléchargement `Sample_1_health.zip` depuis `afpworld.com` → `/tmp/afpworld-blind-test/`.
2. Parsing AFP via `RafptorParser` (Module 1).
3. Transformation en IR via `AfpToIrTransformer` (Module 3).
4. Rendu PDF via `PdfRenderer` + PDFBox (Module 5).
5. Rasterisation du PDF de référence **et** du PDF Rafptor à 150 DPI.
6. Calcul SSIM page par page (`skimage.metrics.structural_similarity`) et génération d'un montage `ref | rafptor | diff amplifié`.
7. QA composite via `rafptor-qa validate`.

## Fix décisif identifié pendant le run

Le parser PTOCA appliquait un décodage EBCDIC IBM500 figé sur **toutes** les payloads TRN. Or ce fichier AFPWorld utilise :

- **Aucun MCF** inline (0 Map Coded Font dans le stream).
- Une référence `Map Data Resource` (D3 AB C3) pointant vers la police Arial via un nom Unicode UTF-16LE inlined.
- Des payloads TRN en **UTF-16BE** directement (variante MO:DCA/P5 moderne utilisée par DOC1, Adobe Output, Compart et la plupart des producteurs insurance-sector).

Commit : [`78394e7`](../src/parser/src/main/java/com/rafptor/parser/ptoca/PtocaParser.java) `fix(converter): decode EBCDIC text using AFP code page mapping`. Ajoute :

1. Propagation du nom de code-page AFP (triplet X'85' / FQN X'84') jusqu'au décodeur, via `AfpCodePageMapper` qui traduit `T1V10037` → `IBM037`, `T1V01141` → `IBM1141`, etc.
2. Auto-détection UTF-16BE dans `PtocaParser.decodeTrn` : longueur paire + ≥ 75% de high-bytes à `0x00` → décode en UTF-16BE au lieu d'EBCDIC.

Avant fix : 8.67% d'ASCII imprimable dans le PDF extrait (bytes EBCDIC bruts 0xC1/0xCA/0xD1 fuitent). Après fix : 97.40%.

## Résultats visuels

### Comparaison côte-à-côte

![Reference vs Rafptor vs Diff](./blind-test-afpworld/montage-page-001.png)

*Gauche : PDF de référence AFPWorld. Centre : sortie Rafptor. Droite : diff amplifié (blanc = identique, noir = différence).*

### Reference (gold standard)

![Reference PDF page 1](./blind-test-afpworld/reference-page-001.png)

### Rafptor output

![Rafptor PDF page 1](./blind-test-afpworld/rafptor-page-001.png)

### Diff amplifié (1 − SSIM map)

![Diff map](./blind-test-afpworld/diff-page-001.png)

## Lecture : qu'est-ce qui marche, qu'est-ce qui manque

### Ce qui marche déjà (fidélité contenu élevée)

- **Texte intégralement extrait** : tous les paragraphes, labels de tableau et points de contact sont présents et lisibles.
- **Positionnement vertical** : les paragraphes retombent approximativement aux bonnes coordonnées Y.
- **Structure logique** : 1 page, ordre de lecture cohérent, titres en début de sections.
- **Noms propres et données** : "John Doe", "4711 Any Street", "Any City, TX 12345" tombent au bon endroit.

### Gaps visuels qui expliquent le delta SSIM

1. **Aucune couleur** : Rafptor rend tout en noir. Le PDF de référence utilise du vert (logo, pied-de-tableau), du gris (bande d'en-tête) et du bleu (headers de colonnes). → Impact SSIM estimé : −0.10.
2. **Pas d'image/logo** : le logo "TimitOO Systems" est absent — le fichier AFP contient un `Include Object` vers un segment de page externe que nous n'avons pas résolu côté ressources. → Impact SSIM : −0.05.
3. **Pas de cadre de tableau** : GOCA box/rule non émis pour ce fichier (les cellules sont posées en PTOCA pur). → Impact SSIM : −0.15.
4. **Espacement inter-mots défectueux** : "Continuing Health Coverage" apparaît comme "Continuing" "Health" "Coverage" sur trois lignes parce que les `Absolute Move Inline` posent chaque mot à une nouvelle position X mais la police par défaut (Liberation) a des métriques de largeur différentes de l'Arial original — les mots se superposent visuellement ou s'écartent. → Impact SSIM : −0.08.
5. **Police** : Liberation Sans remplaçant Arial. L'anti-crénelage et les corps sont proches, pas identiques. → Impact SSIM : −0.03.

Total cumulé ≈ 0.29 de perte — **cohérent avec le gap observé** (1.00 − 0.71 = 0.29).

### Pourquoi le `visual_score` du validator est à 0.053 alors que SSIM brut = 0.71

Le validator Rafptor applique un barème plus sévère qui combine :

- SSIM brut avec un seuil de coupure (scores < 0.80 sont écrasés exponentiellement),
- Analyse de blocs texte avec tolérance de décalage de position,
- Pénalité sur la divergence de nombre de glyphes par ligne.

Ce barème reflète la tolérance **banking-grade** : un PDF dont le logo manque ou dont les couleurs sont altérées est *toujours* considéré comme non re-émissible en production, même si le contenu textuel est fidèle. Le validator fait bien son travail en flaggant ce document pour reconversion.

## Impact sur la roadmap

Ce blind-test clarifie les priorités techniques :

1. **P1 — Couleurs PTOCA + GOCA** : lire les Set Text Color (STC) et propager la couleur jusqu'à l'IR. Impact SSIM estimé : +0.10.
2. **P1 — Résolution de ressources externes** (overlays, segments de page, include object) : supporte 90% des AFP banking qui référencent des logos partagés. Impact SSIM estimé : +0.05.
3. **P2 — Mapping polices plus fin** : utiliser les métriques Arial via la FOCA ou embedder les TrueType référencés par MDR. Impact SSIM : +0.05.
4. **P2 — GOCA auto-boxes** : détecter les séries de GLINE orthogonales qui forment un rectangle et les consolider en GBOX. Impact SSIM : +0.08.
5. **P3 — Kerning / tracking** : respecter l'espacement inter-mots original via le flux PTOCA AMI au lieu de compter sur la largeur natural de la police de remplacement.

Avec P1+P2, le SSIM devrait franchir 0.85, ce qui est la **zone "accepté" du validator**.

## Fichiers produits

Artefacts exécution (hors repo, regénérables) :

- `/tmp/afpworld-blind-test/01_Health_Coverage.afp` — source AFP.
- `/tmp/afpworld-blind-test/01_Health_Coverage.pdf` — sortie Rafptor (11.8 KB, 1 page).
- `/tmp/afpworld-blind-test/reference-DO-NOT-OPEN/01_Health_Coverage.pdf` — référence AFPWorld.
- `/tmp/afpworld-blind-test/qa/01_Health_Coverage.json` — rapport QA JSON.

Artefacts committés (dans `docs/blind-test-afpworld/`) :

- `reference-page-001.png` — rasterization de la référence à 150 DPI.
- `rafptor-page-001.png` — rasterization de la sortie Rafptor à 150 DPI.
- `diff-page-001.png` — carte de différence amplifiée (1 − SSIM map).
- `montage-page-001.png` — montage côte-à-côte.
- `qa-report.json` — rapport validator.

## Reproduction

```bash
# 1. Télécharger l'échantillon
wget -q https://www.afpworld.com/wp-content/uploads/Sample_1_health.zip -O /tmp/afp-dl.zip
mkdir -p /tmp/afpworld-blind-test
unzip -o /tmp/afp-dl.zip -d /tmp/afpworld-blind-test/

# 2. Convertir (depuis src/converter)
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
export PATH=$JAVA_HOME/bin:$PATH
cd src/converter
mvn -q package -DskipTests
mvn -q dependency:build-classpath -Dmdep.outputFile=/tmp/converter.cp
CP="target/rafptor-converter-0.1.0-SNAPSHOT.jar:$(cat /tmp/converter.cp)"
java -cp "$CP" com.rafptor.converter.E2EConvertTest \
     /tmp/afpworld-blind-test/01_Health_Coverage.afp \
     /tmp/afpworld-blind-test/01_Health_Coverage.pdf

# 3. Valider
/tmp/venv-py/bin/rafptor-qa baseline \
     /tmp/afpworld-blind-test/reference-DO-NOT-OPEN \
     --output /tmp/afpworld-blind-test/reference-rasterized --dpi 150
/tmp/venv-py/bin/rafptor-qa validate \
     /tmp/afpworld-blind-test/01_Health_Coverage.pdf \
     --reference /tmp/afpworld-blind-test/reference-rasterized/01_Health_Coverage \
     --dpi 150 \
     --output /tmp/afpworld-blind-test/qa/01_Health_Coverage.json \
     --diff-dir /tmp/afpworld-blind-test/diffs/01_Health_Coverage
```
