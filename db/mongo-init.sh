#!/bin/bash
set -e

echo "Waiting for MongoDB to be ready..."
# Wait for MongoDB to be ready
until mongosh mongodb://mongodb:27017 --quiet --eval "db.version()" > /dev/null 2>&1; do
  echo "Still waiting..."
  sleep 2
done

# Initialize replica set with localhost so it's accessible from the host machine
echo "Initializing replica set..."
mongosh mongodb://mongodb:27017 --quiet --eval '
  rs.initiate({
    _id: "rs0",
    members: [{ _id: 0, host: "localhost:27017" }]
  });
'

echo "Waiting for replica set to be ready..."
sleep 5

echo "MongoDB initialization complete - replica set is ready!"
