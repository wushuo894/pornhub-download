FROM openjdk:11-jdk
COPY target/pornhub-download-jar-with-dependencies.jar /usr/app/pornhub-download-jar-with-dependencies.jar
WORKDIR /usr/app
VOLUME /video
ENV PORT="7093"
ENV FILE="/video"
ENV TZ="Asia/Shanghai"
EXPOSE 7093
CMD ["java", "-jar", "pornhub-download-jar-with-dependencies.jar"]
