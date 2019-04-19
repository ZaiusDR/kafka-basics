#!/bin/bash

KAFKA_PATH=/usr/local/kafka
KAFKA_CONFIG=${KAFKA_PATH}/config/server.properties

# Just a new line at the end of the config
echo  "" >> ${KAFKA_CONFIG}

# Grrr!!!! Setting networking configuration
# This is not obvious, but the networking is quite tricky with containers (I guess also on distributed envs)
# It seems Kafka returns the broker to connect using these options values. If they are not set
# Kafka returns internal addresses resolved by java.net.InetAddress.getCanonicalHostName() which can't be
# reachable from the Docker Host or the server trying to Produce or Consume
echo "listeners=PLAINTEXT://:9092" >> ${KAFKA_CONFIG}
echo advertised.listeners=PLAINTEXT://localhost:9092 >> ${KAFKA_CONFIG}

# Start services
${KAFKA_PATH}/bin/zookeeper-server-start.sh ${KAFKA_PATH}/config/zookeeper.properties &
${KAFKA_PATH}/bin/kafka-server-start.sh ${KAFKA_PATH}/config/server.properties