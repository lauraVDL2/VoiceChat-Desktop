#!/bin/bash

DB_PASSWORD=password123
DB_USER=neo4j

function purge() {
  docker exec -it neo4j cypher-shell -u $DB_USER -p $DB_PASSWORD "MATCH (n) DETACH DELETE n"
  docker system prune -a --volumes
}

if [ "$1" = "purge" ]; then
  purge
fi