FROM python:3.6-alpine

RUN apk update \
 && apk add jq \
 && rm -rf /var/cache/apk/*

COPY . /opt

WORKDIR /opt

RUN pip install -r /opt/requirements.txt
