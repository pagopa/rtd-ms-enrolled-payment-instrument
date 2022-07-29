# rtd-ms-enrolled-payment-instrument

Micro-service designed to manage (read only) the payment instruments for both BPD and FA system


## Local deploy
```
docker-compose up -d --build
```

## Environment Variables
| Variable name                               | Default              | Accepted        | Description                                       |
|---------------------------------------------|----------------------|-----------------|---------------------------------------------------|
| MONGODB_CONNECTION_URI                      | null                 | `mongodb://...` | Connection string to mongodb                      |
| MONGODB_NAME                                | `rtd`                | string          | The db name                                       |
| KAFKA_BROKER                                | localhost:29095      | `hostname:port` | The kafka broker host + port                      |
| KAFKA_SASL_JAAS_CONFIG_CONSUMER_ENROLLED_PI | null                 |                 | Configuration for JAAS authentication             |
| KAFKA_PARTITION_KEY_EXPRESSION              | headers.partitionKey |                 | A spring SpEL expression to extract partition key |
| KAFKA_PARTITION_COUNT                       | 1                    |                 | Number of kafka partitions                        |