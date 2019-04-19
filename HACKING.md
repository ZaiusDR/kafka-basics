# Docker Kafka Container

## Building the image

```
docker build . -t kafka
```


## Running the container

```
docker build -t kafka . && docker run -p 2181:2181 -p 9092:9092 --name kafka -d --rm kafka
```

This will map both Zookeeper and Kafka ports and start their services.

_Note `--rm`: The container will be deleted after kafka service is stopped._


## Running commands inside the container

```
docker exec -it kafka bash
```