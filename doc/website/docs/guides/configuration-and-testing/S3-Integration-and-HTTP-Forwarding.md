# S3 Integration and HTTP Forwarding

In this document, we are going to see how we can record streams to S3-compatible systems(AWS, OVH, Digital Ocean etc.) and Configure HTTP forwarding.

Using S3 services is more cost-effective than storing files on your own server. It’s also easy to manage. Furthermore, S3 services have a lot of API capabilities. For example, you can add CORS policies, rules, and triggers to your system.

By following this documentation, you could store MP4, WebM, HLS files and preview to your cloud storage automatically.

### Record streams to AWS S3

In order to programmatically access to S3, you should have an access token and secret keys. You can create a programmatic user to have an access token and secret key from AWS IAM(Identity and Access Management) console.

![image.png](@site/static/img/image-284429.png)

![image.png](@site/static/img/image-284529.png)

Just Add User by checking Programmatic Access box and then in the next section click Attach existing policies directly and add AmazonS3FullAccess access permission to this user. Copy access token and secret key for this user.

![image.png](@site/static/img/image-284629.png)

![image.png](@site/static/img/image-284729.png)

Right now, you should have access token, secret key, bucket name in your hand.

![image.png](@site/static/img/image-284829.png)

You also need to know the region of your bucket. If you do not have any bucket, you can create it on S3 Console

![image.png](@site/static/img/image-284929.png)

![image.png](@site/static/img/image-285029.png)

![image.png](@site/static/img/image-285129.png)

Here you see the sample S3 credentials. You need to set to yours:

Then, log in the http://your\_ams\_server:5080, enable the Record Live Streams as MP4 and Enable S3 Recording, enter the S3 credentials you have created and save the settings.

![image.png](@site/static/img/image-285229.png)

Your MP4 files and Preview files will be uploaded to your S3 Storage automatically.

### Record streams to OVH Object Storage

OVH is a cost-effective cloud provider and it is preferred by many people. If you installed Ant Media Server on an OVH cloud instance, you may want to upload your stream recordings to S3 storage. You could do that with a few steps. Let start!

Firstly, you need to generate Secret Key and Access Key with your OpenStack username/password. You can learn more about it in OVH's docs.

After generating Secret Key and Access, you just need to create an Object Storage as an image.

![image.png](@site/static/img/image-285329.png)

You will see the Dashboard below after clicking the Create the Container.

![image.png](@site/static/img/image-285429.png)

Here you see the sample S3 credentials. You need to set to yours:

Then, log in the http://your\_ams\_server:5080, enable the Record Live Streams as MP4 and Enable S3 Recording, enter the S3 credentials you have created and save the settings.

![image.png](@site/static/img/image-285529.png)

Your MP4 files and Preview files will be uploaded to your OVH Object Storage automatically.

### Record streams to Digital Ocean Spaces

DigitalOcean is another cloud provider that is preferred by many Ant Media Server users. You could integrate your DigitalOcean cloud instance easily with S3 cloud storage. Let’s see how it can be done with a few steps!

Firstly, you need to create Spaces. Just click the Space button and fill in the blanks.

![image.png](@site/static/img/image-285629.png)

After creating Spaces you need to create API keys for Access and Secret keys. Just click the API button on the left side and then click Generate New Key.

![image.png](@site/static/img/image-285729.png)

Just type the Name parameter and click the create button.

![image.png](@site/static/img/image-285829.png)

After generating Access keys and Secret keys, there is only one step left.

![image.png](@site/static/img/image-285929.png)

Then, log in the http://your\_ams\_server:5080, enable the Record Live Streams as MP4 and Enable S3 Recording, enter the S3 credentials you have created and save the settings.

![image.png](@site/static/img/image-286029.png)

Your MP4 files and Preview files will be uploaded to your Digital Ocean Spaces automatically.

### Record streams to Wasabi Storage

Wasabi is another cloud provider that is preferred by many Ant Media Server users. You could integrate your Wasabi storage. Let’s see how it can be done with a few steps!

Firstly, you need to create a new access key in your Wasabi account.

![image.png](@site/static/img/image-286129.png)

After generating Access keys and Secret keys, you need to create a bucket. Just click the Create Bucket on the right side.

![image.png](@site/static/img/image-286229.png)

Then, log in the http://your\_ams\_server:5080, enable the Record Live Streams as MP4 and Enable S3 Recording, enter the S3 credentials you have created and save the settings.  
![](@site/static/img/image-1648581984499.png )

Your MP4 files and Preview files will be uploaded to your Wasabi storage automatically.

### Record streams to Google Cloud Storage

Google Cloud is another cloud provider that is preferred by many Ant Media Server users. You could integrate your Google Cloud cloud instance easily with S3 cloud storage. Let’s see how it can be done with a few steps!

Firstly, you need to create a Bucket. Just click the Create button and fill in the blanks. You should choose the access level to Fine-grained.

![](@site/static/img/image-1665067750280.png)

After creating the Bucket, go to the bucket and create a folder named streams.

![](@site/static/img/image-1665067824644.png )

After creating the streams folder, go to settings and interoperability tab. On the User account HMAC section and choose the default project for interoperability access.  
![](@site/static/img/image-1665067873135.png)

After choosing the default project, create an access key for the user account.  
![](@site/static/img/image-1665067947615.png )

Log in to the Ant media server, enable the Record Live Streams as MP4 and Enable S3 Recording options, enter your S3 credentials, and save the changes. Enter "auto" for Region Name and "https://storage.googleapis.com" for Endpoint.

![](@site/static/img/image-1665068031722.png )

Your MP4 files and Preview files will be uploaded to your Google Cloud Storage Bucket automatically.

### HTTP Forwarding

HTTP forwarding is implemented to forward incoming HTTP requests to any other place. It's generally used for forwarding incoming request to a storage like S3.

Let us tell how HTTP Forwarding works step by step

*   Open the file {AMS-DIR} / webapps / {APPLICATION} / WEB-INF / red5-web.properties with your text editor (vim, nano)
*   Add comma separated file extensions like this settings.httpforwarding.extension=mp4,png to the file.
*   Add the base URL with settings.httpforwarding.baseURL=https://{YOUR\_DOMAIN} for forwarding.

Usage Example:

*   If you are using AWS S3 bucket, {YOUR\_DOMAIN} will be like:  
    {s3BucketName}.s3.{awsLocation}.amazonaws.com  
      
    
*   If you are using Digital Ocean Spaces, {YOUR\_DOMAIN} will be like:  
    {BucketName}.{BucketLocation}.digitaloceanspaces.com

**Note:** Don't add any leading, trailing white spaces.

*   Save the file and restart the Ant Media Server with sudo service antmedia restart.  
      
    If it's configured properly, your incoming MP4 requests such as  
    https://{SERVER\_DOMAIN}:5443/{APPLICATION\_NAME}/streams/vod.mp4 will be forwarded to https://{YOUR\_DOMAIN}/streams/vod.mp4

### How to play AWS S3 VOD files with Embedded Web Player?

If you would like to embedd the VODs stored in AWS S3 bucket, you need to configure CORS parameters on AWS S3 Bucket Permissions

CORS parameters of AWS S3 bucket should be modified so that the requests that are coming from another origins to play the VODs can be processed.

Go to your AWS ->` Services ->` S3 ->` Buckets ->` "Your Bucket" ->` Permissions ->` And at the bottom of the page there is Cross-origin resource sharing (CORS). The CORS configuration, written in JSON, defines a way for client web applications that are loaded in one domain to interact with resources in a different domain."  
Click Edit->` and paste the code provided below:

    [
        {
            "AllowedHeaders": [
                "*"
            ],
            "AllowedMethods": [
                "HEAD",
                "GET",
                "PUT",
                "POST",
                "DELETE"
            ],
            "AllowedOrigins": [
                "*"
            ],
            "ExposeHeaders": []
        }
    ]
    

"\*" on the origin field as is it accepts requests from all origins, it can be used for quick-testing. However, it can be changed for allowing permissions for exact origins, such as "http://www.your-domain.com" since you only want to accept requests that are coming from your end.
