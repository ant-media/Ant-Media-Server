---
sidebar_position: 8
title: Frequently Asked Questions
---
# Frequently Asked Questions

## How to install SSL on the AWS EC2 server instance?

1.  Please use an Elastic IP address.
2.  Add A record in your Elastic IP address.
3.  After that please check DNS records in here ->` [https://dnschecker.org/](https://dnschecker.org/)
4.  If everything is fine, follow the [SSL Setup Tutorial](/v1/docs/setting-up-ssl)

## Where can I download JavaScript SDK?

JavaScript SDK is available in the Ant Media Server. It can be accessed via ```http://SERVER_ADDR:5080/LiveApp/js/webrtc_adaptor.js```. Its file location is ```/usr/local/antmedia/webapps/LiveApp/js/webrtc_adaptor.js```. Its source code is also available [here](https://github.com/ant-media/StreamApp/blob/master/src/main/webapp/js/webrtc_adaptor.js)

## Can I use Docker to deploy Ant Media Server?

Yes. Utilizing Docker images is a very common way of deploying Ant Media Server. Check the [Installation](https://github.com/ant-media/Ant-Media-Server/wiki/Installation#docker-installation)

## I cannot login to AMS dashboard after upgrading

Starting from version 2.2, Ant Media stores passwords with MD5 encryption. For this reason, you need to change your passwords to MD5 encryption. You can encrypt your password basically any MD5 encrypter tool like: [https://www.md5online.org/md5-encrypt.html](https://www.md5online.org/md5-encrypt.html)

Here are the steps:

1.  Login to MongoDB
2.  Run commands below:

*   ```use serverdb```
*   ```db.getCollection('User').find() //Get User details```
*   ```db.User.updateOne({"_id": "5e978ef3c9e77c0001228040"}, {$set:{password: "md5Password"}}) //Use User ID in updateOne section and use password with MD5 protection```

## How can I reset the admin password?

*   Stop the server using ```service antmedia stop```.
*   Delete the ```server.db``` file under ```/usr/local/antmedia/```
*   Start the server using ```service antmedia start```

If you're using ```mongodb``` as database, your password will be in ```serverdb``` database and in ```User``` collection.

*   Connect to your ```mongodb``` server with ```mongo``` client.
*   Type ```use serverdb;```
*   Type ```db.User.find()``` and it shows you the output like below. ```{ "_id" : ObjectId("5ea486690f09e71c2462385a"), "className" : "io.antmedia.rest.model.User", "email" : "test@antmedia.io", "password" : "1234567", "userType" : "ADMIN" }```
*   You can update the password with a command something like below. Change the parameters below according to the your case. ```db.User.updateOne( { email:"test@antmedia.io" }, { $set: { "password" : "test123" }})```
*   Alternatively, you can delete the user with a command something like below. Change the parameters below according to the your case. ```db.User.deleteOne( { "email": "test@antmedia.io" } )```
*   As of version 2.3.2, passwords should be hashed with MD5.

## What is HLS?

The HLS (HTTP Live Streaming) protocol was developed by Apple. The HLS streaming protocol works by chopping MPEG-TS video content into short chunks. On slow network speeds, HLS allows the player to use a lower quality video, thus reducing bandwidth usage. HLS videos can be made highly available by providing multiple servers for the same video, allowing the player to swap seamlessly if one of the servers fails.

## How can I reduce latency for RTMP to HLS streaming

To reduce the HLS latency, you need to modify a few parameters. This way, the latency can be reduced to 8-10 secs.

*   Make HLS segment time 2 seconds. You can decrease this value to have lower latency but then players start to poll servers more frequently, and it is a waste of resources.
*   Make key frame interval to 2 seconds (this value should be consistent with the HLS segment time). Open Broadcaster Software (OBS) sends key frames every 10 seconds by default.

![](@site/static/img/image-1645447048346.png)

After doing these adjustments, your latency will significantly be reduced.

## How can I enable SSL for Ant Media Server?

Follow the [SSL Setup Tutorial](/v1/docs/setting-up-ssl)

## How can I remove port forwarding?

Check that which port forwardings exist in your system with the command below.

    sudo iptables -t nat --line-numbers -L

The command above should give an output live as follows:

    Chain PREROUTING (policy ACCEPT)
    num  target     prot opt source               destination         
    1    REDIRECT   tcp  --  anywhere             anywhere             tcp dpt:https redir ports 5443
    2    REDIRECT   tcp  --  anywhere             anywhere             tcp dpt:http redir ports 5080
    
    ...

Delete a rule by line number. For instance, in order to delete the http ->` 5080 forwarding, run the command below

    iptables -t nat -D PREROUTING 2

The second parameter is the line number.

## How can I fix "Make sure that your domain name was entered correctly and the DNS A/AAAA record(s)" error?

*   First, make sure that A record is entered in your DNS settings and pointed to your server.
*   Check the ports (443 and 80) are not blocked or are not forwarded to any other ports.
*   If you forward 80 or 443 ports to 5080 and 5443, then remove these port forwarding settings as described above.

## How can I fix "NotSupportedError" while publishing ?

To solve this problem you must enable SSL. Follow the [SSL Setup Tutorial](/v1/docs/setting-up-ssl)

## WebRTC stream stops after a few seconds.

This issue is generally caused by closed UDP ports. Please make sure that UDP ports 5000 to 65535 are open.

## How can I fix a 403 Forbidden error?

Please see [this](/v1/docs/rest-api-guide#security-%E2%80%93-ip-filtering) document.

## How does adaptive bitrate work?

Ant Media Server measures the client's bandwidth and chooses the best quality in the adaptive bitrates according to the bandwidth of the client. For instance, if there are three bitrates, 2000Kbps, 1500Kbps, 1000Kbps and client's bandwidth is 1700Kbps, then the video with 1500Kbps will be sent to the client automatically.

## How can I set up an auto-scaling cluster with Ant Media Server?

Please see [this document.](/v1/docs/clustering-and-scaling-ant-media-server)

## What is the difference between LiveApp and WebRTCAppEE?

There are no differences between them and they are just names of applications. Users can have different options and configurations in the same server by using different applications (such as enabling H.264 in one app and enabling VP8 in the other).

## How can I improve WebRTC bit rate?

You can set the ```bandwidth``` property to any value you want to use in ```WebRTCAdaptor``` in the Javascript SDK. This is the maximum bitrate value that WebRTC can use. Its default value is 900kbps.

## What latencies can I achieve with Ant Media Server Enterprise Edition?

*   ~0.15 seconds latency with WebRTC to WebRTC streaming path with AWS Wavelength.
*   ~0.5 seconds latency with WebRTC to WebRTC streaming path.
*   0.5-1 seconds latency with RTSP/RTMP to WebRTC streaming path.
*   8-12 seconds latency with RTMP/WebRTC to HLS streaming path.

## How many different bit rates are possible with Ant Media Server Enterprise Edition?

There is no soft limit. Generally, it's recommended to use 2 or 3 bitrates for most of the cases.

The recommended resolutions and corresponding bitrates are:

*   **240p:** 500 Kbps
*   **360p**: 800 Kbps
*   **480p:** 1000 Kbps
*   **720p:** 1500 Kbps
*   **1080p:** 2000 Kbps

## Does ultra-low latency streaming support adaptive bit rates?

Yes. Ant Media Server provides ultra-low latency and adaptive bitrate support at the same time.

## Does Ant Media Server have an Embedded SDK?

Yes. Ant Media Server Enterprise has a native Embedded SDK for ARM, x86 and x64 platforms.

## How can I configure the location for MP4 recordings?

MP4 files are recorded to the streams folder under the web apps. A soft link can be created for that path using this command: ```ln -s {target_folder} {link_name}```.

## How to use Self-Signed Certificate on Ant Media Server?

**1.** Install OpenSSL package. This example is for Ubuntu, but you can install self-signed certificate using yum under CentOS as well.

```apt-get update && apt-get install openssl -y```

**2.** Create a self-signed certificate as follows.

ams.crt = your certificate file

ams.key = your key file

```openssl req -newkey rsa:4096 -x509 -sha256 -days 3650 -nodes -out ams.crt -keyout ams.key```

**3.** Submit the requested information and press the Enter button.

    Country Name (2 letter code) [AU]:UK
    State or Province Name (full name) [Some-State]:London
    Locality Name (eg, city) []:London
    Organization Name (eg, company) [Internet Widgits Pty Ltd]:Ant Media
    Organizational Unit Name (eg, section) []:Support
    Common Name (e.g. server FQDN or YOUR name) []:domain.com
    Email Address []: contact@antmedia.io

**4.** The certificate and private key will be created at the specified location. Run the ```enable_ssl.sh``` script as below.

```/usr/local/antmedia/enable_ssl.sh -f ams.crt -p ams.key -c ams.crt -d ams_server_ip```

Note:

If you want to use a domain address with your local network, you need to add the parameter below in ```/etc/hosts``` file.

```ams_server_ip domain.com```

When you add domain address in your hosts file, you need run ```enable_ssl.sh``` script.

```/usr/local/antmedia/enable_ssl.sh -f ams.crt -p ams.key -c ams.crt -d domain.com```

## How can I install custom SSL by building a chain certificate?

The most important reason to upload your intermediate certificate with your SSL certificate is that the browser attempts to verify whether your SSL certificate is real or not.

You are able to provide intermediate certificates from the certificate provider web page.

The order of the certificate should be as follows.

    Root Certificate
    Intermediate Certificate

To give an example for Comodo;

```cat COMODORSAAddTrustCA.crt COMODORSADomainValidationSecureServerCA.crt AddTrustExternalCARoot.crt >` chain.crt```

Now, your full chain certificate is ```chain.crt```

## How can I change the default HTTP port (5080)?

You can use a specific HTTP port instead of 5080. For this, you need to change the parameters below:

Change ```http.port``` in ```/AMS-FOLDER/conf/red5.properties```

    http.port=5080 // You need to change `5080` port what you want

Change **RequestDispatherFilter** parameter in /AMS-FOLDER/webapps/root/WEB-INF/web.xml as shown below:

      	`<servlet>`
    	  `<servlet-name>`RequestDispatherFilter`</servlet-name>`
    	  `<servlet-class>`io.antmedia.console.servlet.ProxyServlet`</servlet-class>`
    	  `<init-param>`
    	    `<param-name>`targetUri`</param-name>`
    	    `<param-value>`http://localhost:5080/{_path}`</param-value>`  // You need to change `5080` port what you want
    	  `</init-param>`
    	  `<init-param>`
    	    `<param-name>`log`</param-name>`
    	    `<param-value>`false`</param-value>`
    	  `</init-param>`
    	  `<init-param>`
    	  	`<param-name>`forwardip`</param-name>`
    	  	`<param-value>`false`</param-value>`
    	  `</init-param>`
    	`</servlet>`

## Where can I get WebRTC viewers information?

Ant Media Server doesn't get any information for WebRTC viewers on the server side. However, you can set this data in plain text format for each client in your application. This information may be a unique id, IP address, location or similar.

This data is involved in the return of the [webrtc-client-stats](https://antmedia.io/rest/#/BroadcastRestService/getWebRTCClientStatsListV2) REST method and also in Grafana reports.

You should pass this information by setting the \`viewerInfo\` field of WebRTCAdaptor object in your WebRTC player.

For example, to assign IDs to viewers according to the time, you can add  \`viewerInfo : "test\_"+Date.now()\` in player.html. Then, you can check it by calling this REST method:

\`http://AMS\_URL/APP\_NAME/rest/v2/broadcasts/STREAM\_ID/webrtc-client-stats/0/5\`

## How to set Apache Reverse Proxy settings for Ant Media Server?

    `<VirtualHost *:80>`
        RewriteEngine On
        RewriteCond %{HTTP_HOST} ^(.*)$
        RewriteRule ^(.*)$ https://%1$1 [R=Permanent,L,QSA]
    `</VirtualHost>`
    
    `<VirtualHost *:443>`
        ServerName yourdomain.com
        SSLEngine On
        SSLCertificateFile /etc/apache2/ssl/yourdomain.crt
        SSLCertificateKeyFile /etc/apache2/ssl/server.key
        SSLCertificateChainFile /etc/apache2/ssl/yourchain.crt
        RewriteEngine on
        RewriteCond %{HTTP:Upgrade} =websocket [NC]
        RewriteRule /(.*)           ws://localhost:5080/$1 [P,L]
        RewriteCond %{HTTP:Upgrade} !=websocket [NC]
        RewriteRule /(.*)           http://localhost:5080/$1 [P,L]
        ProxyPass / http://localhost:5080/
        ProxyPassReverse / http://localhost:5080/
    `</VirtualHost>`

## How can I install the Ant Media Server on Ubuntu 18.04 with ARM64?

First, build glibc 2.29.

    sudo apt install build-essential bison
    wget http://ftp.gnu.org/gnu/glibc/glibc-2.29.tar.gz
    tar -xf glibc-2.29.tar.gz
    cd glibc-2.29/
    mkdir build
    cd build
    ../configure --prefix=/opt/glibc-2.29
    make
    sudo make install

Run the server as an init.d service.

    sudo systemctl stop antmedia
    sudo systemctl disable antmedia
    sudo rm /etc/systemd/system/antmedia.service
    sudo cp /usr/local/antmedia/antmedia /etc/init.d/
    sudo update-rc.d antmedia defaults
    sudo update-rc.d antmedia enable

Add LD\_PRELOAD to the init.d script.

    sudo nano /etc/init.d/antmedia 
    # ADD the following line before the line case "$1" in
    export LD_PRELOAD="/opt/glibc-2.29/lib/libm.so.6"

Save and exit the editor. Run the following commands.

    sudo systemctl daemon-reload
    sudo service antmedia stop
    sudo service antmedia start