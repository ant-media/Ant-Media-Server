# Deploying on AWS Wavelength

AWS Wavelength enables developers to build applications that deliver ultra-low latencies to mobile devices and end users. Wavelength deploys standard AWS compute and storage services to the edge of telecommunication carriers' 5G networks. 

You can extend an Amazon Virtual Private Cloud (VPC) to one or more Wavelength zones. You can then use AWS resources like Amazon Elastic Compute Cloud (Amazon EC2) instances to run the applications that require ultra-low latency and a connection to AWS services in the Region.

Deploying AMS on Wavelength helps you decrease the latency to under 150 milliseconds with the conditions provided within AWS Wavelength.

AWS Wavelength needs some special care to make Ant Media Server run successfully. The first point is the SSL and STUN Server configurations both in standalone and cluster deployments. You can find the solutions in the below links for SSL and STUN servers.

Second, there is no Elastic Load Balancer in Wavelength Zones for the Auto-scalable Cluster deployments. This problem is also addressed by providing a special NGINX load balancer that listens to the auto-scalable group and updates its configuration. It's installed automatically through the CloudFormation template below.

You can use Ant Media Server v2.4.1 and later for AWS Wavelength Deployments.

*   [Install SSL](/v1/docs/installing-ssl)
*   [Configure STUN Server](/v1/docs/configuring-stun-server)
*   [Standalone Server Deployment with Cloudformation](/v1/docs/aws-wavelength-standalone-deployment)
*   [Auto-Scalable Cluster Deployment with Cloudformation](/v1/docs/aws-wavelength-cluster-deployment)