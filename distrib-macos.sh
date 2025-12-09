#!/bin/bash

DIST_DIR="distrib"
APP_NAME="microsim"
APP_DIR="$APP_NAME.app"

mkdir -p $DIST_DIR

# Chemin vers le JRE Macos à intégrer
JRE_PATH="/Users/lehuen/bin/JRE-25.0.1+8"

# Classe principale de votre application
MAIN_CLASS="microsim.MicroSim"

# Icône source (PNG)
ICON_SOURCE="data/icons/icon_512.png"

# -----------------------------------------------------------------------
echo "Nettoyage du répertoire de distribution..."
# -----------------------------------------------------------------------

rm -rf "$DIST_DIR/$APP_DIR"

# -----------------------------------------------------------------------
echo "Vérification du répertoire du JRE..."
# -----------------------------------------------------------------------

if [ ! -d "$JRE_PATH" ]; then
    echo "Erreur : Le répertoire de la JRE '$JRE_PATH' n'a pas été trouvé."
    exit 1
fi

# -----------------------------------------------------------------------
echo "Création de l'arborescence de l'application..."
# -----------------------------------------------------------------------

mkdir -p "$APP_DIR/Contents/MacOS"
mkdir -p "$APP_DIR/Contents/Resources/Java"
mkdir -p "$APP_DIR/Contents/Java"

# -----------------------------------------------------------------------
echo "Copie du JRE dans l'application..."
# -----------------------------------------------------------------------

cp -r "$JRE_PATH" "$APP_DIR/Contents/Java/jre"

# -----------------------------------------------------------------------
echo "Création du fichier Info.plist..."
# -----------------------------------------------------------------------

cat > "$APP_DIR/Contents/Info.plist" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleExecutable</key>
    <string>microsim</string>
    <key>CFBundleIconFile</key>
    <string>icon.icns</string>
    <key>CFBundleIdentifier</key>
    <string>com.github.lehuen.microsim8bits</string>
    <key>CFBundleName</key>
    <string>$APP_NAME</string>
    <key>CFBundlePackageType</key>
    <string>APPL</string>
    <key>CFBundleShortVersionString</key>
    <string>1.0</string>
    <key>CFBundleVersion</key>
    <string>1</string>
    <key>LSMinimumSystemVersion</key>
    <string>10.13.0</string>
    <key>NSHumanReadableCopyright</key>
    <string>© 2025 Jérôme Lehuen</string>
    <key>JVMMainClassName</key>
    <string>$MAIN_CLASS</string>
</dict>
</plist>
EOF

# -----------------------------------------------------------------------
echo "Création du script de lancement..."
# -----------------------------------------------------------------------

cat > "$APP_DIR/Contents/MacOS/microsim" <<EOF
#!/bin/sh
cd \$(dirname \$0)
BASE=\$(pwd)
LOG_FILE="\$(dirname \$BASE)/microsim.log"
JAVA_HOME=\$(dirname \$BASE)/Java/jre/Contents/Home
JAVA=\$JAVA_HOME/bin/java
\$JAVA -Xdock:name="$APP_NAME" -Xdock:icon="\$BASE/../Resources/icon.icns" -jar \$BASE/../Resources/Java/microsim.jar >> "\$LOG_FILE" 2>&1
EOF

# Rendre le script de lancement exécutable
chmod +x "$APP_DIR/Contents/MacOS/microsim"

# -----------------------------------------------------------------------
echo "Conversion et copie de l'icône..."
# -----------------------------------------------------------------------

if [ -f "$ICON_SOURCE" ]; then
    sips -s format icns "$ICON_SOURCE" --out "$APP_DIR/Contents/Resources/icon.icns" > /dev/null 2>&1
else
    echo "Avertissement : Fichier icône '$ICON_SOURCE' non trouvé. L'application n'aura pas d'icône."
fi

# -----------------------------------------------------------------------
echo "Copie des fichiers JAR (application et librairies)..."
# -----------------------------------------------------------------------

cp build/$APP_NAME.jar build/lib/*.jar "$APP_DIR/Contents/Resources/Java/"

# -----------------------------------------------------------------------
echo "Copie des ressources (images, manuel, etc.)..."
# -----------------------------------------------------------------------

cp -r data/ "$APP_DIR/Contents/Resources/data"
cp -r man/ "$APP_DIR/Contents/MacOS/man"

mv $APP_DIR $DIST_DIR
