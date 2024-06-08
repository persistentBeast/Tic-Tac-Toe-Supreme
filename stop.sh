#!/bin/bash

#!/bin/bash

# Stop Java applications
echo "Stopping Java applications..."
pkill -f "java -jar"

# Stop Hazelcast server
echo "Stopping Hazelcast server..."
cd ~/hazelcast-3.12.12/bin
sh stop.sh

# Stop React application
echo "Stopping React application..."
cd ~/apps/tic-tac-toe
npm stop