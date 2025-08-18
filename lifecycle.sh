#!/bin/bash

DB_PASSWORD=password123
DB_USER=neo4j

function purge() {
  purge_db
  purge_docker
}

function purge_db() {
  docker exec -it neo4j cypher-shell -u $DB_USER -p $DB_PASSWORD "MATCH (n) DETACH DELETE n"
}

function purge_docker() {
  docker system prune -a --volumes
}

if [ "$1" = "purge" ]; then
  purge
elif [ "$1" = "purge_db" ]; then
  purge_db
elif [ "$1" = "purge_docker" ]; then
  purge_docker
fi