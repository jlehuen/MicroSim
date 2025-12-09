#!/bin/bash

DIST_DIR="distrib"
ARCH_NAME="microsim-linux.tar.gz"

mkdir -p $DIST_DIR/microsim/

# Chemin vers le JRE Linux à intégrer
JRE_LINUX=/Users/lehuen/bin/JRE-25.0.1+8-Linux/

# -----------------------------------------------------------------------
echo "Nettoyage de l'ancienne distribution..."
# -----------------------------------------------------------------------

rm $DIST_DIR/$ARCH_NAME

# -----------------------------------------------------------------------
echo "Copie des fichiers de l'application..."
# -----------------------------------------------------------------------

cp -r build/* $DIST_DIR/microsim/

mkdir $DIST_DIR/microsim/man/
cp -r man/* $DIST_DIR/microsim/man/

mkdir $DIST_DIR/microsim/data/
cp data/icons/icon_512.png $DIST_DIR/microsim/data/

# -----------------------------------------------------------------------
echo "Copie du JRE Linux..."
# -----------------------------------------------------------------------

cp -R $JRE_LINUX $DIST_DIR/microsim/jre/

# -----------------------------------------------------------------------
echo "Création du script de lancement..."
# -----------------------------------------------------------------------

LAUNCH_SCRIPT=$DIST_DIR/microsim/microsim.sh

cat << 'EOF' > "$LAUNCH_SCRIPT"
#!/bin/bash
cd $(dirname $0)
./jre/bin/java \
	--enable-native-access=ALL-UNNAMED \
	-jar microsim.jar
EOF

chmod +x $LAUNCH_SCRIPT

# -----------------------------------------------------------------------
echo "Création du script d'installation..."
# -----------------------------------------------------------------------

SETUP_SCRIPT=$DIST_DIR/microsim/setup.sh

cat << 'EOF' > "$SETUP_SCRIPT"
#!/bin/bash

# MicroSim Setup Script
# Run this script to install MicroSim for the current user.

echo "Installing MicroSim..."

# Define installation paths
APP_DIR="$HOME/.local/share/microsim"
DESKTOP_DIR="$HOME/.local/share/applications"

# The script is expected to be in the root of the extracted files
SOURCE_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Create target directories
mkdir -p "$APP_DIR"
mkdir -p "$DESKTOP_DIR"

# Copy all application files
echo "Copying files to $APP_DIR..."
rsync -a --exclude='setup.sh' "$SOURCE_DIR/" "$APP_DIR/"
chmod +x "$APP_DIR/microsim.sh"

# Create the .desktop file
echo "Creating application shortcut..."
cat > "$DESKTOP_DIR/microsim.desktop" <<EOF_DESKTOP
[Desktop Entry]
Version=1.0
Name=MicroSim
Comment=8-bit Microprocessor Simulator
Exec=$APP_DIR/microsim.sh
Icon=$APP_DIR/data/icon_512.png
Terminal=false
Type=Application
Categories=Education;Development;Electronics;
EOF_DESKTOP

# Update the desktop database
if command -v update-desktop-database &> /dev/null; then
    echo "Updating application database..."
    update-desktop-database "$DESKTOP_DIR"
fi

echo ""
echo "Installation complete!"
echo "You can find MicroSim in your application menu."
echo "To uninstall, simply remove the following directory and file:"
echo " - Directory: $APP_DIR"
echo " - File: $DESKTOP_DIR/microsim.desktop"
EOF

chmod +x $SETUP_SCRIPT

# -----------------------------------------------------------------------
echo "Création de l'archive de distribution Linux..."
# -----------------------------------------------------------------------

cd $DIST_DIR

export COPYFILE_DISABLE=1
chmod -R u+w microsim
tar -czhf $ARCH_NAME microsim
rm -r microsim
