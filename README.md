# Google Calendar Bot for Wire

## Build
```
docker build -t dejankovacevic/cali-bot:1.0.0 .
```

## Run
```
docker run \
-e SERVICE_TOKEN="secret" \
-e DB_URL='jdbc:postgresql://localhost/cali' \
--name cali --rm dejankovacevic/cali-bot:1.0.0
```

## Compose
```
docker-compose -f docker-compose.yaml up --build -d --remove-orphans
```