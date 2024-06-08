#!/bin/bash

# Function to check if a command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}


# Load environment variables from .env file
if [ -f .env ]; then
    export $(grep -v '^#' .env | xargs)
else
    echo ".env file not found!"
    exit 1
fi

# Check if Redis is installed
if command_exists redis-server; then
    echo "Redis is already installed."
    # Enable and start Redis service
    sudo systemctl start redis-server
else
    # Install Redis
    echo "Installing Redis..."
    sudo apt-get update
    sudo apt-get install -y redis-server

    # Enable and start Redis service
    echo "Enabling and starting Redis service..."
    sudo systemctl enable redis-server
    sudo systemctl start redis-server
fi

# Check if Hazelcast is installed
cd ~
cd hazelcast-3.12.12/bin

sh start.sh

# Check if Maven is installed
if command_exists mvn; then
    echo "Maven is already installed."
else
    # Install Maven
    echo "Installing Maven..."
    sudo apt-get install -y maven
fi


cd ~/apps

# Run the Java applications with environment variables
echo "Running Java Application 1..."
java -jar user-connector-0.0.1-jar-with-dependencies.jar &

echo "Running Java Application 2..."
java -jar game-service-0.0.1-jar-with-dependencies.jar &

# Clone and start the React application
echo "Starting React application..."

# Assuming the React app is in app1 repository
cd ~/apps/tic-tac-toe
npm start