FROM ubuntu:18.04

MAINTAINER "Identity Team <identity@rackspace.com>"

RUN apt-get update
ENV echo 'America/Chicago' > /etc/timezone
RUN DEBIAN_FRONTEND=noninteractive apt-get install -y make build-essential zlib1g-dev libbz2-dev \
libreadline-dev libsqlite3-dev wget curl llvm libncurses5-dev libncursesw5-dev \
xz-utils tk-dev python-dev libxml2-dev libxslt1-dev libssl1.0-dev

RUN DEBIAN_FRONTEND=noninteractive apt-get install -y unzip zip git-core vim curl wget telnet
RUN DEBIAN_FRONTEND=noninteractive apt-get install -y openjdk-8-jdk-headless

RUN useradd -ms /bin/bash test
RUN echo 'test:password' | chpasswd

USER test
ENV HOME /home/test
WORKDIR $HOME

RUN git clone https://github.com/pyenv/pyenv.git $HOME/.pyenv
RUN git clone https://github.com/pyenv/pyenv-virtualenv.git $HOME/.pyenv/plugins/pyenv-virtualenv

RUN echo 'export PYENV_ROOT="$HOME/.pyenv"' >> $HOME/.bash_aliases
RUN echo 'export PATH="$PYENV_ROOT/bin:$PATH"' >> $HOME/.bash_aliases
RUN printf 'if command -v pyenv 1>/dev/null 2>&1; then\n  eval "$(pyenv init -)"\nfi\n' >> $HOME/.bash_aliases
RUN echo 'eval "$(pyenv virtualenv-init -)"' >> $HOME/.bash_aliases

ENV PYENV_ROOT $HOME/.pyenv
ENV PATH $PYENV_ROOT/shims:$PYENV_ROOT/bin:$PATH

RUN pyenv install 2.7.9
RUN pyenv virtualenv 2.7.9 cloud-identity
RUN pyenv global cloud-identity
RUN pip install virtualenv

WORKDIR $HOME/source/tests

CMD exec /bin/bash -c "trap : TERM INT; sleep infinity & wait"
