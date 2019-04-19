FROM centos

ENV KAFKA_PATH /usr/local/kafka

WORKDIR /root

RUN yum install -y wget java-1.8.0-openjdk

RUN wget -nv http://apache.uvigo.es/kafka/2.2.0/kafka_2.12-2.2.0.tgz

RUN mkdir -p ${KAFKA_PATH}

RUN tar zxvf kafka_2.12-2.2.0.tgz -C ${KAFKA_PATH} --strip 1

RUN echo "export PATH=\$PATH:${KAFKA_PATH}/bin" >> ~/.bashrc

RUN bash -c 'mkdir -p /kafka_data/{kafka,zookeeper}'

COPY docker_files/kafka_startup.sh .

RUN chmod +x kafka_startup.sh


EXPOSE 2181 9092

CMD ./kafka_startup.sh