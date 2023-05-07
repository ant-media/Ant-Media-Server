<div align='center'>
   <img src="https://user-images.githubusercontent.com/54481799/95862105-16cb0e00-0d6b-11eb-9087-88888889825d.png" height="60">
</div>

<div align='center'>
   <a href="https://maven-badges.herokuapp.com/maven-central/io.antmedia/ant-media-server">
    <img src="https://maven-badges.herokuapp.com/maven-central/io.antmedia/ant-media-server/badge.svg"/>
   </a>
   <a href="https://sonarcloud.io/dashboard?id=io.antmedia%3Aant-media-server">
    <img src="https://sonarcloud.io/api/project_badges/measure?project=io.antmedia%3Aant-media-server&metric=alert_status"/>
   </a>
   <a href="https://in.linkedin.com/company/antmedia">
    <img src="https://img.shields.io/badge/LinkedIn-0077B5?style=social"/>
   </a>
   <a href="https://twitter.com/antmedia_io">
    <img src="https://img.shields.io/twitter/follow/antmedia_io?style=social"/>
   </a>
   <a href="https://travis-ci.org/ant-media/Ant-Media-Server">
    <img src="https://travis-ci.org/ant-media/Ant-Media-Server.svg?branch=master"/>
   </a>
   <a href="https://github.com/ant-media/Ant-Media-Server/blob/master/COMMUNITY_EDITION_LICENSE">
    <img src="https://img.shields.io/badge/license-Apache-blue"/>
   </a>
</div>

<div align="center">
<br />
<a href="https://www.producthunt.com/posts/ant-media-streaming-server?utm_source=badge-featured&utm_medium=badge&utm_souce=badge-ant&#0045;media&#0045;streaming&#0045;server" target="_blank"><img src="https://api.producthunt.com/widgets/embed-image/v1/top-post-badge.svg?post_id=334960&theme=light&period=daily" alt="Ant&#0032;Media&#0032;Streaming&#0032;Server - Stream&#0032;your&#0032;live&#0032;videos&#0032;the&#0032;ultra&#0045;low&#0032;latency&#0032;way | Product Hunt" style="width: 250px; height: 54px;" width="160" height="26" /></a> <a href="https://www.producthunt.com/posts/ant-media-streaming-server?utm_source=badge-featured&utm_medium=badge&utm_souce=badge-ant&#0045;media&#0045;streaming&#0045;server" target="_blank"><img src="https://api.producthunt.com/widgets/embed-image/v1/top-post-topic-badge.svg?post_id=365190&theme=light&period=monthly&topic_id=267" alt="Ant&#0032;Media&#0032;Streaming&#0032;Server - Stream&#0032;your&#0032;live&#0032;videos&#0032;the&#0032;ultra&#0045;low&#0032;latency&#0032;way | Product Hunt" style="width: 250px; height: 54px;" width="160" height="26" /></a> <br />

<strong>Play with: <a href="https://antmedia.io/webrtc-samples?utm_source=github&utm_medium=readme&utm_campaign=ams">WebRTC Samples developed with Ant Media Server</a></strong>
</div>


## Introduction

Ant Media Server is a streaming engine software that provides adaptive, ultra-low latency streaming using WebRTC technology with ~0.5 seconds latency or low latency using HLS or CMAF. Ant Media Server is robust, battle-tested, and highly scalable horizontally and vertically, running on-premises or on any cloud provider you choose.

It enables building high-performing infrastructure with Ultra-Low Latency Video Streaming. This way, we're solving the video streaming and delivery headache, a massive problem for companies seeking low latency, adaptive video streaming, by making it simple and on-demand.

## ⚡ Ant Media Server Features

Ant Media Server is packed with features to help you create the best-quality live and on-demand video streaming. It offers two editions, the Community Edition and the Enterprise Edition, each with its own unique features.

#### ⭐️ Community Edition

The Community Edition is a free, open-source version of Ant Media Server. It provides basic live streaming functionality with adaptive bitrate, transcoding, and playback support for WebRTC, RTMP, RTSP, MP4, HLS, and MPEG-DASH support. Additionally, it offers recording and playback of live streams, simulcasting, and RESTful APIs for controlling and monitoring the server.

This also supports various SDKs, including iOS, Android, React Native, Flutter, and Javascript.

#### ✨ Enterprise Edition

The Enterprise Edition of Ant Media Server offers all the features of the Community Edition and more. It is designed for better scalability, where your use case requires advanced streaming capabilities and enhanced security features. Here are some of the key features of the Enterprise Edition:

- Sub-second Latency with WebRTC: Ultra-low latency streaming using WebRTC technology with ~0.5 seconds latency.

- Cluster Mode: Deploy multiple instances of Ant Media Server in a cluster to handle large-scale streaming traffic and provide high availability and failover support.

- WebRTC Data Channel: Enable data transfer between clients in real-time using the WebRTC Data Channel.

- WebRTC Server-Side Recording: Record WebRTC streams on the server side without the need for browser-based recording.

- Advanced Security: Secure your streams with SSL/TLS encryption, token authentication, IP filtering, and watermarking.

- Professional Support: Get access to dedicated technical support, training, and consulting services from the Ant Media team.

The Enterprise Edition requires a licence, and it offers a range of additional benefits to businesses and organizations. With its advanced streaming capabilities, enhanced security features, and dedicated technical support, it is the ideal choice for those who require a reliable and scalable media server for their video streaming needs.

If you're interested in trying the Enterprise Edition of Ant Media Server, please visit our website or contact us for more information. We're always happy to help!


## 🚀 Deploy Ant Media Server

Ant Media server is available on your favorite cloud platform; with 1-Click apps, cloud marketplaces, or deployment through Docker/Kubernetes/Scripts allows you to deploy and automate common setup steps to get your application running seamlessly. Discover how easily you can try and deploy Ant Media Server by exploring the following quick launches:


| Cloud Provider  | Community Edition | Enterprise Edition  |
| -------------- | -------------- | ------------- |
| AWS  | [Community Edition](https://aws.amazon.com/marketplace/pp/prodview-okmynlgwgvq6w)  | [Enterprise Edition](https://aws.amazon.com/marketplace/pp/prodview-464ritgzkzod6)  |
| Microsoft Azure  | [Community Edition](https://azuremarketplace.microsoft.com/en-us/marketplace/apps/antmedia.ams_community_edition)  | [Enterprise Edition](https://azuremarketplace.microsoft.com/en-us/marketplace/apps/antmedia.ant_media_server_enterprise)  |
| Alibaba  | - | [Enterprise Edition](https://marketplace.alibabacloud.com/products/56712002/sgcmjj00031246.html)  |
| Digital Ocean  | [Community Edition](https://marketplace.digitalocean.com/apps/ant-media-server-community-edition)  | [Enterprise Edition](https://marketplace.digitalocean.com/apps/ant-media-server-enterprise-edition) or [Enterprise Edition Kubernetes](https://marketplace.digitalocean.com/apps/ant-media-server-enterprise)  |
| Linode  | [Community Edition](https://www.linode.com/marketplace/apps/ant-media/ant-media-community-edition/)  | [Enterprise Edition](https://www.linode.com/marketplace/apps/ant-media/ant-media-enterprise-edition/)  |
| Linux  | [Install via  Script](https://antmedia.io/docs/guides/installing-on-linux/installing-ams-on-linux/)  | [SSL Setup](https://antmedia.io/docs/guides/installing-on-linux/setting-up-ssl/)  |
| Docker  | [Docker Compose](hhttps://antmedia.io/docs/guides/clustering-and-scaling/docker/docker-and-docker-compose-installation/)  | [Docker Swarm](https://antmedia.io/docs/guides/clustering-and-scaling/docker/docker-swarm/)  |
| Cluster  | [Cluster installation](https://antmedia.io/docs/guides/clustering-and-scaling/cluster-installation/)  | [Multi Level Cluster](https://antmedia.io/docs/guides/clustering-and-scaling/multi-level-cluster/)  |
| Kubernetes  | [Deploy on Kubernetes](https://antmedia.io/docs/guides/clustering-and-scaling/kubernetes/deploy-ams-on-kubernetes/)  | [Kubernetes Autoscaling](https://antmedia.io/docs/guides/clustering-and-scaling/kubernetes/kubernetes-autoscaling/)  |
 
> 📌 We offer a [free trial](https://antmedia.io/free-trial/) so that you can explore the advantages of Ant Media Enterprise Edition. So why wait? [Request your free-trial today](https://antmedia.io/self-hosted-free-trial/) and take your streaming capabilities to the next level with ultra-low latency!


## 📄 Documentation

We've created extensive documentation to help you get the most out of Ant Media, whether you're a developer looking to integrate our platform into your own applications or a content creator looking to set up a live stream. Our documentation includes:

- Quick start guides to get you up and running quickly
- API documentation for developers
- Tutorials on advanced features, such as live transcoding and adaptive bitrate streaming
- And much more!
 
## 📫 Support

At Ant Media, providing timely and effective support is critical for the success of our customers. Therefore, we offer two levels of support to meet the diverse needs of our users.

- For our **Community Edition** users: We offer support via GitHub Discussions, where you can post your questions, issues, or feature requests. Our vibrant Ant Media Community and the team of experts will be there to assist you, providing solutions and advice to help you get the most out of your Ant Media Server experience.

- For our **Enterprise Edition** users: We offer priority support via email. Our dedicated support team is available to help you with any technical issues or challenges you may face. We understand that your business needs may require immediate attention, so we prioritize your requests to ensure you receive timely and effective support.

No matter which edition you use, we are committed to providing you with the best possible support experience. We encourage you to explore our support packages and take advantage of the resources available to you. Our goal is to help you achieve success with Ant Media Server and empower you to create exceptional streaming experiences for your users. And, for any general inquiries, kindly connect with us via [contact@antmedia.io](mailto:contact@antmedia.io)

## 🤝 Contributing

We welcome contributions from the community and are always looking for ways to improve our platform. If you're interested, please review our contributing guidelines to get started.

If you would like to say thank you or/and support the active development of Ant Media Server: 
- Add a [GitHub Star](https://github.com/ant-media/Ant-Media-Server/) to the project.
- Tweet about the project on your Twitter.
- Write interesting articles about the project on [Dev.to](https://dev.to/), [Medium](https://medium.com/) or personal blog.

Together, we can make this project **better** every day! 😘

## 💡 More Information

To learn more about Ant Media and our live video streaming platform, visit our official website at https://antmedia.io.
