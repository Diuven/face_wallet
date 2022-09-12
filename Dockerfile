FROM ubuntu:20.04
RUN apt-get update \
    && apt-get install -y curl bash openjdk-11-jre \
	&& rm -rf /var/cache/apk/*

EXPOSE 8080

CMD ./gradlew bootRun -Dspring.devtools.livereload.enabled=true