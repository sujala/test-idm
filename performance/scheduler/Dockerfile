FROM python:3

COPY requirements.txt /opt/requirements.txt


RUN pip install -r /opt/requirements.txt

COPY . /opt
WORKDIR /opt

# this is required for RSI since it runs services under uid/gid 1001
RUN useradd -r -s /bin/false -u 1001 test || true
RUN chown -R 1001:1001 /opt

USER 1001

ENTRYPOINT /opt/bootstrap.sh
