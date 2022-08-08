FROM openjdk:8-jdk-alpine

MAINTAINER sean<donghyun.kim@amona.io>

COPY *.jar /var/local
WORKDIR /var/local
RUN ulimit -c unlimited

EXPOSE 8080

#ENV TZ="Asia/Seoul"
ENV JAVA_OPTS="-server -Xms2048m -Xmx4096m -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap"

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "twinkorea-api-0.0.1-SNAPSHOT.jar"]