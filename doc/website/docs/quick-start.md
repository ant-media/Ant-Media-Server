---
title: Quick start with AMS
id: introduction
slug: /
sidebar_position: 1
---
# Installation

This quick start guide lets you set up Ant Media Server (AMS) Enterprise and try ultra-low latency streaming in a few minutes of time. The installation procedure for Ant Media Server Community and Enterprise Editions are same so you can install The Community Edition by following these instructions as well.


<VideoPlayer video="https://www.youtube.com/embed/EH6v-yUyzjU" youtube="true" />

There are 2 options to start Ant Media Server (AMS).

1.  Install to a remote server with a public IP address
2.  Launch in [AWS](https://aws.amazon.com/marketplace/search/results?x=0&y=0&searchTerms=Ant+Media+Server&page=1&ref_=nav_search_box), [Azure](https://azuremarketplace.microsoft.com/en-us/marketplace/apps/antmedia.ant_media_server_enterprise?tab=Overview), [DigitalOcean](https://marketplace.digitalocean.com/apps/ant-media-server-enterprise-edition) or another service provider.

In this document we'll show you the first option.

## 1. Download Ant Media Server

You can download Community Edition from [Releases](https://github.com/ant-media/Ant-Media-Server/releases) or download Enterprise Edition on your account on [antmedia.io](https://antmedia.io/) after you get a license on [antmedia.io.](https://antmedia.io/) After you download AMS, you will have a compressed file that contains the Ant Media Server.

## 2. Install Ant Media Server

Open a terminal and go to the directory where you download the Ant Media Server.


```shell
cd /path/to/where/ant-media-server/is/downloaded
```

Get the `install_ant-media-server.sh` script.



```shell
wget https://raw.githubusercontent.com/ant-media/Scripts/master/install_ant-media-server.sh 
chmod 755 install_ant-media-server.sh
```

Install Ant Media Server(AMS) as follows. Please don't forget to replace `[ANT_MEDIA_SERVER_INSTALLATION_FILE]` below with the downloaded .zip file name at step 1.


```shell
sudo ./install_ant-media-server.sh -i [ANT_MEDIA_SERVER_DOWNLOADED_FILE] 
```

## 3. Install SSL for Ant Media Server

Please make sure that your server instance has a public IP address and a domain is assigned to its public IP address. Then go to the folder where Ant Media Server is installed. Default directory is `/usr/local/antmedia`


```shell
cd /usr/local/antmedia
```

Run ```./enable_ssl.sh``` script in AMS installation directory. Please don't forget to replace {DOMAIN_NAME} with your domain name.



```shell
sudo ./enable_ssl.sh -d {DOMAIN_NAME}
```

For detailed information about SSL, follow [SSL Setup](https://portal.document360.io/v1/docs/setting-up-ssl).

## 4. Ultra-low latency publishing/playing

Ant Media Server Enterprise Edition provides 0.5 seconds end-to-end latency. So let's try ultra-low latency first.

**Publishing**

-   Visit the page `https://your-domain-address.com:5443/WebRTCAppEE/`
-   Let the browser access the camera and mic. Then click `Start Publishing`  
    ![](@site/static/img/1_Open_WebRTCAppEE_and_Let_Browser_Access_Cam_and_Mic.jpg)
-   After you press the button, "Publishing" blinking text should appear  
    ![](@site/static/img/2_Press_Publish_Button.jpg)

**Playing**

-   Visit the page `https://your-domain-address.com:5443/WebRTCAppEE/player.html`
-   Write the stream id in the text box( `stream1` by default)  
    ![](@site/static/img/3_Go_to_Play_Page.jpg)
-   Press `Start Play` button. After you press the button, the WebRTC stream starts playing.  
    ![](@site/static/img/4_Press_Start_Play_Button.jpg)

<InfoBox>
Don't forget that the <code>stream id</code> on the publish and play pages should be the same.
</InfoBox>

This quick start lets your try ultra-low latency streaming in Ant Media Server. If you have any problems, don't be hesitate to [contact us](https://antmedia.io/#contact).