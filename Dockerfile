FROM maven:3-openjdk-11 AS build
LABEL description="Google Calendar Bot - powered by Wire"
LABEL project="wire-bot:cali"

WORKDIR /app

# download dependencies
COPY pom.xml ./
RUN mvn verify --fail-never -U

# build
COPY src ./src
RUN mvn package -DskipTests=true

# runtime stage
FROM wirebot/runtime:1.2.2

WORKDIR /opt/cali

# Copy configuration
COPY cali.yaml /etc/cali/

# Copy built target
COPY --from=build /app/target/cali.jar /opt/cali/

# create version file
ARG release_version=development
ENV RELEASE_FILE_PATH=/etc/cali/release.txt
RUN echo $release_version > $RELEASE_FILE_PATH

EXPOSE  8080 8081 8082
ENTRYPOINT ["java", "-jar", "cali.jar", "server", "/etc/cali/cali.yaml"]




