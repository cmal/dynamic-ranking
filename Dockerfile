FROM java:8-alpine
MAINTAINER Your Name <you@example.com>

ADD target/uberjar/dynamic-ranking.jar /dynamic-ranking/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/dynamic-ranking/app.jar"]
