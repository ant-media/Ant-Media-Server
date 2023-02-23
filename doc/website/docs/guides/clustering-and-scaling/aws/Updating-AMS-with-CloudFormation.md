# Updating AMS with CloudFormation

If you have installed Ant Media Server with Cloudformation and then want to update it, simply follow the steps below.

**NOTE: Please note that your old Instances will be terminated after this update.**

**1.** Log in to the AWS Dashboard and find CloudFormation in the search box. Then, click on the "stack" that you created earlier.

**2.** Please proceed by clicking “**Stack actions >` Create change set for current stack**” button  
![](@site/static/img/image-1648408340505.png)

**3.** In this part, select the “**Replace current template**” and “**Amazon S3 URL**” options. Pass the URL below as Amazon S3 input and click “**Next**” button.

[https://ams-cloudformation.s3.eu-central-1.amazonaws.com/antmedia-aws-autoscale-update-template.yaml](https://ams-cloudformation.s3.eu-central-1.amazonaws.com/antmedia-aws-autoscale-update-template.yaml)

![](@site/static/img/image-1648408390850.png)

**4.** The values in this section are your previous settings, you can also change the parts you want to change here. Then please proceed by clicking “**Next**” button.

![](@site/static/img/image-1648408489675.png)

![](@site/static/img/image-1648408511839.png)

**5.** Please proceed by clicking “**Next**” button

![](@site/static/img/image-1648408532875.png)

**6.** Please proceed by clicking “**Create change set**” button.

![](@site/static/img/image-1648408556782.png)

**7.** Click "**Create change set**" in the window that comes up.

![](@site/static/img/image-1648408574196.png)

**8.** Please proceed by clicking “**Execute**” button.

![](@site/static/img/image-1648408591135.png)

**9.** Please proceed by clicking “**Execute change set**” button then Let's start the update process.

![](@site/static/img/image-1648408610018.png)

**10.** When the update process is completed, you will see the "**UPDATE\_COMPLETE**" event as below.

![](@site/static/img/image-1648408625725.png)

If you have any questions, please just drop a line to [contact@antmedia.io](mailto:contact@antmedia.io).