# Docker image for central
FROM docker.io/openjdk:8-jdk AS builder

ADD . /root/
WORKDIR /root/
RUN ./gradlew :central:installDist

FROM docker.io/openjdk:8-jre-alpine AS runtime

WORKDIR /
COPY --from=0 /root/central/build/install/ ./usr/

ENTRYPOINT [ '/usr/bin/central' ]
