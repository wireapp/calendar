FROM dejankovacevic/bots.runtime:latest

COPY target/cali.jar       /opt/cali/cali.jar
COPY libs/libblender.so    /opt/cali/libblender.so
COPY cali.yaml             /etc/cali/cali.yaml

WORKDIR /opt/cali



