FROM openjdk:8

RUN apt-get update && \
  apt-get install -y --no-install-recommends unzip sqlite3 inotify-tools && \
  rm -rf /var/lib/apt/lists/* && \
  apt-get clean

ENV GATLING_VERSION 2.2.4

COPY download-gatling.sh /opt/
COPY watch-report.sh /opt/

RUN /opt/download-gatling.sh 2.1.7 2.1
RUN /opt/download-gatling.sh 2.2.5 2.2
RUN /opt/download-gatling.sh 2.3.0 2.3

RUN chgrp 0 /opt; chmod -R g=u /opt

USER 1001

CMD ["./opt/watch-report.sh"]

