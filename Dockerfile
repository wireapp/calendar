FROM wire/bots.runtime:latest

COPY target/cali.jar      /opt/cali/cali.jar
COPY certs/keystore.jks   /opt/cali/keystore.jks

WORKDIR /opt/cali
EXPOSE  443

