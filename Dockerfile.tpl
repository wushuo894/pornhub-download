FROM openjdk:11-jdk
COPY target/pornhub-download-jar-with-dependencies.jar /usr/app/pornhub-download-jar-with-dependencies.jar
WORKDIR /usr/app
VOLUME /video
VOLUME /config
ENV PORT="7093"
ENV CONFIG="/config"
ENV TZ="Asia/Shanghai"
EXPOSE 7093
CMD ["java", "-jar", "pornhub-download-jar-with-dependencies.jar"]
