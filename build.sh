#!/bin/bash

# Copy .m2 directory from investTrack root to bezkoder-app
if [ -d ".m2" ]; then
    echo "Copying .m2 directory to bezkoder-app..."
    cp -r .m2 bezkoder-app/.m2
    echo ".m2 directory copied successfully."
else
    echo "Warning: .m2 directory not found in investTrack root. Skipping copy."
fi

# Execute docker compose build
echo "Building Docker images..."
docker compose build





