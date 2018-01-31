FROM dejankovacevic/bots.runtime:latest

COPY target/cali.jar      /opt/cali/cali.jar
COPY conf/cali.yaml       /etc/cali/cali.yaml

WORKDIR /opt/cali

