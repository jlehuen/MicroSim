#!/bin/bash

DIST_DIR="build"
JAR_NAME="microsim.jar"
MANIFEST="MANIFEST.MF"
MAIN_CLASS="microsim.MicroSim"

# Adaptez ici le chemin vers votre JDK
JAR=/Users/lehuen/bin/JDK-21.0.5+11/Contents/Home/bin/jar

# -----------------------------------------------------------------------
# Nettoyage du répertoire de distribution précédent
# -----------------------------------------------------------------------

rm -rf $DIST_DIR
mkdir -p $DIST_DIR

# -----------------------------------------------------------------------
# Création du fichier manifeste
# -----------------------------------------------------------------------

echo "Manifest-Version: 1.0" > $MANIFEST
echo "Main-Class: $MAIN_CLASS" >> $MANIFEST

# Lister les JARs dans le répertoire lib pour le Class-Path
# Le Class-Path dans le manifeste est relatif au JAR principal
CLASS_PATH=$(ls lib/*.jar | xargs -n 1 basename | paste -sd ' ' -)
echo "Class-Path: $CLASS_PATH" >> $MANIFEST

# Afficher le contenu du manifeste pour vérification
echo "Contenu du MANIFEST.MF:"
cat $MANIFEST
echo ""

# -----------------------------------------------------------------------
# Compiler le projet si les classes ne sont pas à jour
# -----------------------------------------------------------------------

if [ ! -d "class" ] || [ -z "$(ls -A class)" ]; then
    echo "Le répertoire 'class' est vide ou n'existe pas. Lancement de la compilation..."
    ./compile.sh
fi

# -----------------------------------------------------------------------
# Supprimer les fichiers .DS_Store
# -----------------------------------------------------------------------

echo "Suppression des fichiers .DS_Store..."
find . -name ".DS_Store" -delete

# -----------------------------------------------------------------------
# Créer le fichier JAR en incluant les classes et les données
# -----------------------------------------------------------------------

echo "Création du fichier JAR..."
$JAR cfm "$DIST_DIR/$JAR_NAME" $MANIFEST -C class . -C data .

# -----------------------------------------------------------------------
# Copier les bibliothèques dans le répertoire de distribution
# -----------------------------------------------------------------------

echo "Copie des bibliothèques..."
cp -r lib "$DIST_DIR/"

# -----------------------------------------------------------------------
# Nettoyer le fichier manifeste temporaire
# -----------------------------------------------------------------------

rm $MANIFEST

echo ""
echo "Le packaging est terminé"
echo "Le JAR exécutable et ses dépendances se trouvent dans le répertoire '$DIST_DIR'"
