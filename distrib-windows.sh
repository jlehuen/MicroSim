#!/bin/bash

DIST_DIR="distrib"
ARCH_NAME="microsim-windows.zip"

mkdir -p $DIST_DIR/microsim/

# Chemin vers le JRE Windows à intégrer
JRE_WINDOWS="/Users/lehuen/bin/JRE-25.0.4.1+1-Windows"

# -----------------------------------------------------------------------
echo "Vérification du répertoire du JRE..."
# -----------------------------------------------------------------------

if [ ! -d "$JRE_WINDOWS" ]; then
    echo "Erreur : Le répertoire de la JRE '$JRE_WINDOWS' n'a pas été trouvé."
    exit 1
fi

# -----------------------------------------------------------------------
echo "Nettoyage de l'ancienne distribution..."
# -----------------------------------------------------------------------

rm -f $DIST_DIR/$ARCH_NAME
rm -rf $DIST_DIR/microsim/*

# -----------------------------------------------------------------------
echo "Copie des fichiers de l'application..."
# -----------------------------------------------------------------------

cp build/microsim.jar $DIST_DIR/microsim/
cp build/lib/*.jar $DIST_DIR/microsim/

mkdir -p $DIST_DIR/microsim/man/
cp -r man/* $DIST_DIR/microsim/man/

mkdir -p $DIST_DIR/microsim/data/
cp data/icons/icon_512.png $DIST_DIR/microsim/data/

# -----------------------------------------------------------------------
echo "Copie du JRE Windows..."
# -----------------------------------------------------------------------

cp -R "$JRE_WINDOWS" $DIST_DIR/microsim/jre/

# -----------------------------------------------------------------------
echo "Création du script de lancement (microsim.bat)..."
# -----------------------------------------------------------------------

LAUNCH_SCRIPT=$DIST_DIR/microsim/microsim.bat

cat << 'EOF' > "$LAUNCH_SCRIPT"
@echo off
cd /d "%~dp0"
start "" "jre\bin\javaw.exe" ^
	--enable-native-access=ALL-UNNAMED ^
	-jar microsim.jar %*
EOF

# -----------------------------------------------------------------------
echo "Création du script d'installation du raccourci (setup.bat)..."
# -----------------------------------------------------------------------

SETUP_SCRIPT=$DIST_DIR/microsim/setup.bat

cat << 'EOF' > "$SETUP_SCRIPT"
@echo off
echo Creation du raccourci MicroSim sur le Bureau...
powershell -NoProfile -Command "$ws = New-Object -ComObject WScript.Shell; $s = $ws.CreateShortcut([System.IO.Path]::Combine([Environment]::GetFolderPath('Desktop'), 'MicroSim.lnk')); $s.TargetPath = '%~dp0microsim.bat'; $s.WorkingDirectory = '%~dp0'; $s.WindowStyle = 7; $s.Save()"
echo Installation terminee !
pause
EOF

# -----------------------------------------------------------------------
echo "Création de l'archive de distribution Windows..."
# -----------------------------------------------------------------------

cd $DIST_DIR

export COPYFILE_DISABLE=1
chmod -R u+w microsim
find microsim -name ".DS_Store" -delete
zip -r -q $ARCH_NAME microsim -x "*.DS_Store"
rm -rf microsim

echo "Distribution Windows générée dans $DIST_DIR/$ARCH_NAME"
