#!/bin/bash

echo "Starting InvestTrack Frontend..."

# Check if node_modules exists
if [ ! -d "node_modules" ]; then
    echo "Dependencies not found. Installing..."
    npm install
fi

# Start the development server
npm start
