#!/bin/bash

NAME="cali-config"

kubectl delete configmap $NAME
kubectl create configmap $NAME --from-file=../conf