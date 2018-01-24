FROM wire/bots.runtime:latest

COPY target/cali.jar      /opt/cali/cali.jar

WORKDIR /opt/cali

