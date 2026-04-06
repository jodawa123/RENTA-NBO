#!/bin/bash

echo "=== Building Rentanbo Docker Image ==="

# Check if local.properties exists
if [ ! -f "local.properties" ]; then
    echo "❌ Error: local.properties not found!"
    echo "Make sure you're in the project root directory"
    exit 1
fi

# Extract API key from local.properties
MAPS_API_KEY=$(grep MAPS_API_KEY local.properties | cut -d '=' -f2)

if [ -z "$MAPS_API_KEY" ]; then
    echo "❌ Error: MAPS_API_KEY not found in local.properties"
    exit 1
fi

echo "✅ Found API key in local.properties"

# Build the Docker image
echo "🏗️ Building Docker image (this may take several minutes)..."
docker build --build-arg MAPS_API_KEY="$MAPS_API_KEY" -t rentanbo-app .

if [ $? -eq 0 ]; then
    echo "✅ Build complete!"
    echo ""
    echo "To run the container:"
    echo "  docker run -d -p 8080:80 --name rentanbo-container rentanbo-app"
    echo ""
    echo "To stop the container later:"
    echo "  docker stop rentanbo-container"
    echo ""
    echo "To view the app:"
    echo "  open http://localhost:8080"
else
    echo "❌ Build failed!"
    exit 1
fi