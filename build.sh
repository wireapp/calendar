#!/usr/bin/env bash
mvn package -DskipTests=true -Dmaven.javadoc.skip=true
docker build -t dejankovacevic/cali-bot:latest .
docker push dejankovacevic/cali-bot
kubectl delete pod -l name=cali -n staging
kubectl get pods -l name=cali -n staging
