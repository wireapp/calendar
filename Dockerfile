FROM dejankovacevic/bots.runtime:2.10.0

COPY target/cali.jar       /opt/cali/cali.jar
COPY libs/libblender.so    /opt/wire/lib/libblender.so
COPY cali.yaml             /etc/cali/cali.yaml

WORKDIR /opt/cali



