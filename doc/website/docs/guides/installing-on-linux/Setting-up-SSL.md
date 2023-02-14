---
title: Setting up SSL
---
Setting up SSL is a mandatory task when requesting access to the microphone and camera. Also, you need to enable HTTPS and WSS (WebSocket Secure) for Chrome to run WebRTC and WebSocket applications.

In addition, developers want to serve their content with a secure connection as well. The script in this document installs **Let's Encrypt** SSL certificate, however you can also use your own SSL installation method.

First, create an `A` record for your domain name in your DNS records. This way, your domain name will be resolved to your server's public IP address. Note that this guide is for Ubuntu systems, but there are several guides on the internet for other Linux distributions as well.

Go to the folder where Ant Media Server is installed. Default directory is `/usr/local/antmedia`

```shell
cd /usr/local/antmedia
```

 If there is a service that uses 80 port, you need to disable it. For example, if your system has Apache web server, you need to disable it using:

```shell
sudo service apache2 stop
```

There are several options to get the SSL certificate. Please choose the one appropriate for you.

## Option 1: Gets a free subdomain and install SSL with Let's Encrypt

If you do not have a domain name and want to install an SSL certificate, you can use this feature. With this feature, **enterprise users** will have a free domain name with the extension **ams-[id].antmedia.cloud** and the Let's Encrypt certificate will be automatically installed. This feature is available in versions after 2.5.2

```shell
sudo ./enable_ssl.sh
```

## Option 2: Create Let's Encrypt certificate with HTTP-01 challenge

Call enable_ssl.sh with your domain name.

```shell
sudo ./enable_ssl.sh -d example.com
```

## Option 3: Import your custom certificate

`enable_ssl.sh` script supports external fullchain.pem, chain.pem and privkey.pem files in the following format.

```shell
sudo ./enable_ssl.sh -f {FULL_CHAIN_FILE} -p {PRIVATE_KEY_FILE} -c {CHAIN_FILE} -d {DOMAIN_NAME} 
```

Example:

```shell
sudo ./enable_ssl.sh -f yourdomain.crt -p yourdomain.key -c yourdomainchain.crt -d yourdomain.com
sudo ./enable_ssl.sh -f yourdomain.pem -p yourdomain.key -c yourdomainchain.pem -d yourdomain.com
```

## Option 4: Create Let's Encrypt certificate with DNS-01 challenge

In this method, there will be no HTTP requests back to your server. This method is useful to create an SSL certificate in restricted environments such AWS Wavelength. This feature is available in versions after 2.4.0.2.

Run `enable_ssl.sh` with `-v custom` as follows.

```shell
sudo ./enable_ssl.sh -d {DOMAIN_NAME}  -v custom
```

The script will ask you to create a TXT record for your domain name.

Text

```comments
- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
Please deploy a DNS TXT record under the name
_acme-challenge.subdomain.yourdomain.com with the following value:

ziB3UjMMSSO-La7jgqPXXXXeK-r2Ja80HluNJVvkg

Before continuing, verify the record is deployed.
- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
```

Create a TXT record in your DNS records as instructed above. For the sample above, we created a TXT **record _acme-challenge.subdomain.yourdomain.com** having a value **ziB3UjMMSSO-La7jgqPXXXXeK-r2Ja80HluNJVvkg**

After you create the TXT record, press Enter to continue.

The process should be completed successfully if you set everything correctly.

## Option 5: Create Let's Encrypt certificate with DNS-01 challenge and Route 53

Let's Encrypt have some plugins to simplify the authorization. Route 53 plugin creates TXT records and deletes them after authorization is done. It's useful while creating instances in AWS Wavelength Zones, as HTTP-01 challenge does not work in AWS Wavelength zone due to its nature.

-   Create a Policy (i.e. dns-challenge-policy) in IAM service with the following content. [Check this out if you don't know how to create a Policy](https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-create-and-attach-iam-policy.html).


```json
{
  "Version": "2012-10-17",
  "Id": "certbot-dns-route53 sample policy",
  "Statement": [
      {
          "Effect": "Allow",
          "Action": [
              "route53:ListHostedZones",
              "route53:GetChange"
          ],
          "Resource": [
              "*"
          ]
      },
      {
          "Effect" : "Allow",
          "Action" : [
              "route53:ChangeResourceRecordSets"
          ],
          "Resource" : [
              "arn:aws:route53:::hostedzone/*"
          ]
      }
  ]
}
```

-   Create a Role (i.e. dns-challenger) in IAM user for EC2 and attach the policy above to that role
-   Assign this oole to the EC2 instance that you plan to install SSL
-   Create **A** record for your domain name in Route 53 that resolves to your IP address.
-   Run the `enable_ssl.sh` as follows:

```shell
sudo ./enable_ssl.sh -d {DOMAIN_NAME}  -v route53
```

-   If everything is set up properly, you can access the server via **http://{DOMAIN_NAME}:5443**

If you disable a service that binds to 80 port such as Apache Web Server, enable it again.

```shell
sudo service apache2 start
```

If the scripts above return successfully, SSL will be installed on your server, and you can use HTTPS through 5443, as follows:

```link
https://example.com:5443
```

Note that if port 80 is used by another process or it's forwarded to another port, `enable_ssl.sh` command will not be successful. Please disable the process or delete the port forwarding temporarily before running the `enable_ssl.sh` script above.