# Security and privacy

Having more than 1200 paying customers managing more than 7000 Ant Media instances worldwide, we take security seriously here in Ant Media. As such, there are several measures we have undertaken to make sure Ant Media can answer your hardest security questions.

  

In this document, you can read about the security and privacy features of Ant Media, and how it helps secure your sensitive client information on-premises.

  

Introduction
------------

Ant Media provides ready-to-use, highly scalable real-time video streaming solutions for live video streaming needs. Based on customer requirements and preferences, it enables a live video streaming solution to be deployed easily and quickly on-premise or on public cloud networks such as AWS, Azure, Digital Ocean, Linode and Alibaba Cloud.

  

Using Ant Media, you are able to:

  

*   Build your highly scalable, ultra low latency video streaming backend easily
*   Develop your own solutions on top of this reliable platform, and
*   Benefit from an adaptive bitrate streaming feature that allows any video to be played with any bandwidth on mobile devices.
*   Expand your reach to a broader audience using our open source SDKs for iOS, Android, and web.

Working principles of Ant Media Server
--------------------------------------

  

Ant Media offers two distinct ways to manage your streaming backend.

  

1.  You can directly send streams using the Ant Media Server, using our intuitive dashboard.
2.  You can use our [platform API](https://antmedia.io/rest/) to either send video streams, or get information about the streams you have, e.g return active live streams, or move an IP camera. 

There are two different deployment options for Ant Media Server:

*   Self-hosting on your own premises: Ant Media Server (AMS) can be installed on-premise (i.e. in your own data center), allowing a greater depth and breadth of security and control. Self-hosting means that no third party (not even us) ever has access to your data unless you permit it. Accessing data collected on your servers is defined and agreed mutually in the contract, and it is limited to bug fixing, problem solving and temporary support purposes. We do not by default collect data from instances you own.
*   Hosting on a public cloud marketplace: The Ant Media server can be deployed on a marketplace. This is for companies that do not want to self-host AMS, and rather work with a cloud platform of their choice. Ant Media collaborates with many cloud partners like AWS, Azure, Digital Ocean, Linode and Alibaba Cloud.

Note that we do not host our customers' servers, but rather work with trusted, preferred hosting providers for hosting, managing and monitoring your servers.

  

This way, each Ant Media Server (or cluster) is private to the client, hosted in their location of choice, protected with strict firewall rules in its own private network, monitored 7/24 and backed up around the clock. 

  

You can choose server locations to be anywhere: this way you can ensure that your streaming video is sent to the nearest location of the viewer. The Ant Media staff access to AMS Enterprise Edition is limited to support, maintenance and upgrade purposes and is subject to your approval.

  

Ant Media runs on Apache Tomcat as the application server. When you deploy Ant Media to be able to respond to thousands of viewers at the same time, you will also require MongoDB (Community Edition, Enterprise Edition or MongoDB Atlas), where data-at-rest encryption is optionally available at the database level. Whether data-at-rest encryption on database level is utilized or not, major hosting providers leverage technologies like full disk encryption and drive locking, to protect your data at rest.

  

Ant Media application layer, powered by Tomcat, has the business logic that helps create your applications. Each application is a video stream. 

Beyond these security and privacy measurements, Ant Media has other capabilities to better safeguard your privacy:

  

*   System audit logs: The system logs collected by Ant Media helps system administrators be on top of important events such as application creation or the initiation of a video stream. In case of an emergency or an audit, logs can be viewed, allowing organizational insight into what has happened and the cause of an issue. 
*   Access levels: AMS dashboard users can only view applications and sections of the dashboard they have been given access to. There are 3 types of user access in the AMS dashboard.
    *   Admin can do anything in its scope, e.g can CRUD anything and access all web panel services. 
    *   User can do anything in the dashboard for particular applications granted. He cannot see or modify other applications.
    *   Read-only user can read anything in the dashboard for applications granted. This user cannot access web panel services, create an application, or start a broadcast.

3\. GDPR
--------

  

We see GDPR as a great milestone in personal data privacy that will force businesses to unveil what’s going on behind the scenes in the most transparent way in history. Consumers will be made aware of what will happen to the personal information they share with a business. They’ll get to learn about different ways businesses take advantage of their data and how that data flows around among other third parties. 

  

Most importantly, this transparency about how personal data is being used will create more educated and privacy-conscious consumers.

Ant Media also has a Data Protection Officer (DPO), along with a team of privacy and security professionals dedicated to our compliance and to helping you maintain your compliance when using AMS.

As summary, Ant Media adheres to privacy commitments and data protection. Simply stating,

  

*   Customers will own their instances and all data, not Ant Media.
*   Ant Media does not use customer data for advertising or sell customer data to third parties.
*   When connecting to the dashboard, customer data is encrypted in transit using SSL.

4\. Data centers and data storage
---------------------------------

Ant Media partners with several hosting platforms to host Ant Media Enterprise Server. Most of those global hosting companies are monitored 24/7 by high-resolution interior and exterior cameras that can detect and track intruders. Access logs, activity records, and camera footage are available in case an incident occurs. Access to their data center floor is only possible via a security corridor that implements multi-factor access control using security badges and biometrics.

### 4.1 Data backups

Customers are responsible to back up all instances they own. Since we don’t host customer data, self-hosted customers can take advantage of the database backup utilities, or use their existing organizational procedures in order to back up their data-bearing disks.

  

### 4.2 User roles

Ant Media supports different user roles. You can, for example, create an admin user to virtually do everything to read-only users, who only can view the dashboard and not do something harmful like removing an application. These permission models are both in API and Ant Media administration tool.

5\. Corporate policies
----------------------

We run background checks as much as we can, on all incoming employees, or contractors who will be working with Ant Media. Additionally, all employees sign confidentiality agreements to protect client information. Ant Media works with 3rd party companies only for the development of some of the SDKs, and all of the SDK source code is publicly open to everyone. The Community Edition and the Enterprise Edition code are developed by full-time company employees in our payroll.

  

If someone finds a security vulnerability in AMS Enterprise Edition or one of its SDK’s source code, we encourage them to let us know right away. In such a case, we investigate all legitimate reports and do our best to quickly fix the problem.

In Ant Media, access to secure services and data is strictly logged, and audit logs are reviewed regularly. Only those who need access to Ant Media services have the necessary credentials.

### 5.1 Compliance

Ant Media complies with applicable legal, regulatory and contract requirements as well as industry best practices. The customer information is stored for as long as it is needed to meet the operational needs of the client, together with contractual legal and regulatory requirements. Cryptographic controls are used in compliance with all relevant agreements, laws, and regulations.