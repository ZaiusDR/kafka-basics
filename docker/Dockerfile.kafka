FROM centos

ENV KAFKA_PATH /usr/local/kafka

WORKDIR /root

# Install dependencies and utilities
RUN yum install -y wget java-1.8.0-openjdk net-tools

# Install kafka
RUN wget -nv http://apache.uvigo.es/kafka/2.2.0/kafka_2.12-2.2.0.tgz

RUN mkdir -p ${KAFKA_PATH}

RUN tar zxvf kafka_2.12-2.2.0.tgz -C ${KAFKA_PATH} --strip 1

# Set PATH for kafka CLI commands
RUN echo "export PATH=\$PATH:${KAFKA_PATH}/bin" >> ~/.bashrc

# TODO: Set data folders. Still not used
RUN bash -c 'mkdir -p /kafka_data/{kafka,zookeeper}'

# Copy entrypoint script
COPY docker_files/kafka_startup.sh .

RUN chmod +x kafka_startup.sh


EXPOSE 2181 9092

CMD ./kafka_startup.sh