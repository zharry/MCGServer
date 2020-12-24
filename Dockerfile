FROM ubuntu

RUN apt update && apt upgrade -y && \
    apt install -y tmux wget gnupg
RUN wget -q -O - https://download.bell-sw.com/pki/GPG-KEY-bellsoft | apt-key add - && \
    echo "deb [arch=amd64] https://apt.bell-sw.com/ stable main" | tee /etc/apt/sources.list.d/bellsoft.list
RUN apt update && apt upgrade -y && \
    apt install -y bellsoft-java11-full

WORKDIR /srv
VOLUME /home/harry/MCG

COPY startup.sh /srv/startup.sh
ENTRYPOINT ["bash", "-c", "/srv/startup.sh"]