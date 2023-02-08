# Creating a new application

Ant Media Server supports application deployment on the fly. This feature actually resolves the multi-tenancy problem for online video platforms.Ant Media Server has 2 built-in applications. Users want to able to use different settings for each application and able to specify the name of the application.

Because of that, customers want to add/create new applications. There are several ways to create/delete application. Let us tell how to do that.

### Web Panel

The fastest and easies way to create new applications on the web panel.

#### Step 1

Login to the web panel & Click the New Application

![](@site/static/img/gpu(1).png)

#### Step 2

Enter application name and click the ```Create``` button

![](@site/static/img/new_application_form.png)

The good thing is that if the server is cluster mode, it will deploy the application to all nodes in the cluster. If the application is deleted, it'll also be deleted in all nodes.

### Shell Script

There a script that creates new applications in Ant Media Server easily. You just need to type a few simple commands.

Let’s have a look at the steps:

#### Step 1

Go to the folder where Ant-Media-Server is installed. Default directory is ```/usr/local/antmedia```

    cd /usr/local/antmedia

#### Step 2

create\_app.sh usage in below.

    sudo ./create_app.sh -n applicationName -p AMS-Installation-Directory

For example:

```sudo ./create_app.sh -n streamHive -p /usr/local/antmedia```

You can add some parameters in ```create_app``` script. Here are the options:

       -n:  Name of the application that you want to have. It's mandatory
       -p: (Optional) Path is the install location of Ant Media Server which is /usr/local/antmedia by default.
       -w: (Optional) The flag to deploy application as war file. Default value is false
       -c: (Optional) The flag to deploy application in cluster mode. Default value is false
       -m:  Mongo DB host. If it's a cluster, it's mandatory. Otherwise optional
       -u:  Mongo DB user. If it's a cluster, it's mandatory. Otherwise optional
       -s:  Mongo DB password. If it's a cluster, it's mandatory. Otherwise optional
       -h: print this usage

Please check it for more detail: [Create App Script](https://github.com/ant-media/Ant-Media-Server/blob/master/src/main/server/create_app.sh#L5)

![](@site/static/img/image-1645437714786.png)

#### Step 3

Restart Ant Media Service

    sudo service antmedia restart

\*This feature is available in Ant Media Server 1.9.0+ versions.

### Rest Method

#### Create Application

Web panel has the following REST method to create application.

    @POST
    @Path("/applications/{appName}")
    @Produces(MediaType.APPLICATION_JSON)

It means that you can call the following method to create an application with curl. Please take a look at the answer here to learn [how to access web panel REST methods programmatically](https://stackoverflow.com/questions/64444673/ant-media-dashboard-settings-through-rest-api/64458222#64458222)

    curl -X POST -H "Content-Type: application/json" "https://{YOUR_SERVER_ADDRESS}:5443/rest/v2/applications/myapp"

### Delete Application

Web panel has the following REST Method to delete the application.

    @DELETE
    @Path("/applications/{appName}")
    @Produces(MediaType.APPLICATION_JSON)

Call the following method to delete an application(apptest) with curl.

    curl -X DELETE -H "https://{YOUR_SERVER_ADDRESS}:5443/rest/v2/applications/myapp"