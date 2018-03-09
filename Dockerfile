FROM dejankovacevic/bots.runtime:latest

COPY target/cali.jar  /opt/cali/cali.jar
COPY cali.yaml        /etc/cali/cali.yaml
COPY libblender.so    /opt/cali/libblender.so

WORKDIR /opt/cali

