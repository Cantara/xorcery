ARG JDK_IMAGE=eclipse-temurin:21-ubi9-minimal
FROM $JDK_IMAGE
ENV JAVA_OPTS="-Xmx1G -Xms1G"
ARG MAIN_JAR
ENV ENV_MAIN_JAR=$MAIN_JAR
ARG MAIN_CLASS
ENV ENV_MAIN_CLASS=$MAIN_CLASS

COPY ./target/app/. /home/app

COPY --chmod=755 <<-EOT /home/app/entrypoint.sh
#!/bin/sh
exec java $JAVA_OPTS -jar lib/${ENV_MAIN_JAR}
EOT

RUN echo "$(</home/app/entrypoint.sh)"
EXPOSE 8080
WORKDIR /home/app
ENTRYPOINT ["./entrypoint.sh"]
