#!/bin/bash

KAFKA_PATH=/usr/local/kafka

${KAFKA_PATH}/bin/zookeeper-server-start.sh ${KAFKA_PATH}/config/zookeeper.properties &
${KAFKA_PATH}/bin/kafka-server-start.sh ${KAFKA_PATH}/config/server.properties