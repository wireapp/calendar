FROM dejankovacevic/bots.runtime:2.10.2

COPY target/cali.jar       /opt/cali/cali.jar
#COPY libs/libblender.so    /opt/wire/lib/libblender.so
COPY cali.yaml             /etc/cali/cali.yaml

WORKDIR /opt/cali

EXPOSE  8080 8081 8082



