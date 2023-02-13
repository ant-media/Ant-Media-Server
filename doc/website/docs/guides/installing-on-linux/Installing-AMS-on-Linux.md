---
title: Installing AMS on Linux
---

Ant Media can be installed on Linux, particularly Ubuntu and CentOS distributions. In order to run AMS on a single instance, you need at least 4GB of RAM. And SSD disks are highly recommended for write/read performance.

This document explains the installation of both Community Edition and Enterprise Edition. There are several methods of installing, including deployment to a full VM, Docker or Kubernetes.

## Download Ant Media Server (AMS)

Download and save the latest AMS Community Edition or Enterprise Edition package.

*   Community Edition can be downloaded from Github [Releases](https://github.com/ant-media/Ant-Media-Server/releases) page.
*   Enterprise Edition can be downloaded from your account after you get a license on [antmedia.io](https://antmedia.io/)

Now open a terminal and go to the directory where you have downloaded AMS zip file.

```shell
cd path/to/where/ant-media-server....zip
```

## Download the installation script

Download the ```install_ant-media-server.sh``` shell script.

    wget https://raw.githubusercontent.com/ant-media/Scripts/master/install_ant-media-server.sh && chmod 755 install_ant-media-server.sh

## Run the installation script

    sudo ./install_ant-media-server.sh -i `<ANT_MEDIA_SERVER_ZIP_FILE>`

For more command line options, type ```sudo ./install_ant-media-server.sh -h```

## Check if the service is running

You can check the service by using service command.

```shell
sudo service antmedia status
```

You can also start/stop the AMS service.

```shell
sudo service antmedia stop
sudo service antmedia start
```

## Install SSL on AMS

Before this step, make sure that your server instance has a public IP address and a domain is assigned to its public IP address. Then, go to the folder where AMS is installed. Default directory is ```/usr/local/antmedia```

```shell
cd /usr/local/antmedia
```

Run ```./enable_ssl.sh``` script in the AMS installation directory. Please don't forget to replace ```{DOMAIN_NAME}``` with your domain name.

```shell
sudo ./enable_ssl.sh -d {DOMAIN_NAME}
```

For detailed information about SSL, follow [SSL Setup](/v1/docs/setting-up-ssl).

## Accessing the web panel

Open your browser and type ```http://SERVER_IP_ADDRESS:5080``` to go to the web panel. If you're having difficulty accessing the web panel, there may be a firewall that blocks accessing the 5080 port.

## Docker installation

Please visit for more information [Docker and Docker Compose](https://resources.antmedia.io/docs/docker-and-docker-compose-installation).

## Cluster installation

Cluster installation is an advanced topic and it has its own page. Please visit [Clustering & Scaling](https://resources.antmedia.io/docs/scaling-ant-media-server).

## Server ports

In order to server run properly you need to open some network ports, defined below:

*   TCP: 1935 (RTMP)
*   TCP: 5080 (HTTP)
*   TCP: 5443 (HTTPS)
*   UDP: 4200 (SRT)
*   UDP: 50000-60000 (WebRTC. This default range is 50000-60000 in v2.4.3 & above. Before 2.4.3, the default value was 5000-65000. Note that you can [change port range](https://stackoverflow.com/questions/62127593/how-to-limit-the-webrtc-udp-port-range-in-ant-media-server) in all releases.
*   TCP: 5000 (You need to open this port in only cluster mode for the internal network communication. It should not be open to the public)

## Forward default http (80), https (443) ports to 5080 and 5443

Generally, port forwarding is used to forward default ports to the server's ports for convenience. For example, let's forward all incoming data from 80 to 5080.

```shell
sudo iptables -t nat -A PREROUTING -p tcp --dport 80 -j REDIRECT --to-port 5080
sudo iptables -t nat -A PREROUTING -p tcp --dport 443 -j REDIRECT --to-port 5443
```

After running the command above, HTTP requests going to 80 will be forwarded to 5080. The HTTP requests going to 443 will be forwarded to 5443. 

Please pay attention that once you enable SSL, port 80 should not be used by any processes, or should not be forwarded to any other port.

## Listing and deleting current port forwardings

To list port forwarding run the command below.

```shell
sudo iptables -t nat --line-numbers -L
```

To delete a port forwarding run the command below.

```shell
iptables -t nat -D PREROUTING [LINE_NUMBER_IN_PREVIOUS_COMMAND]
```

## Make port forwarding persistent

If you want the server to reload port forwarding after reboot, we need to install iptables-persistent package and save rules like below.

    sudo apt-get install iptables-persistent

The command above will install iptables-persistent package. After installation, run the command below every time you make a change and want it to be persistent.

ActionScript

    sudo sh -c "iptables-save >` /etc/iptables/rules.v4"