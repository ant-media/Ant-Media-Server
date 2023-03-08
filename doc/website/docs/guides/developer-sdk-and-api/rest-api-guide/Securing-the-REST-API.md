# Securing the REST API

This guide explains how to control REST API security on Ant Media Server. You could secure your REST services with the IP Filter feature.

## IP Filter for the REST API

If you want only some IP addresses to be able to access REST APIs, you should add IP’s or IP Ranges in `Dashboard > {Application} > Settings > IP Filtering Settings` panel.

![](@site/static/img/image-1645195915640.png)

**If 127.0.0.1 is deleted, requests on the server (localhost) are disabled. Devices in the same network can access but other devices that are not, cannot access the REST API when 127.0.0.1 is on the list.**

If you want to remove the REST Filter in AMS, you should delete the below codes in `/usr/local/antmedia/webapps/Application-Name/WEB-INF/web.xml`

```
<filter>
<filter-name>AuthenticationFilter</filter-name>
<filter-class>io.antmedia.console.rest.AuthenticationFilter</filter-class>
</filter>

<filter-mapping>
<filter-name>AuthenticationFilter</filter-name>`
<url-pattern>/rest/*</url-pattern>
</filter-mapping>
```

If you delete the AuthenticationFilter code block in the application, everyone can access the REST API.

## IP Filter for the Web Panel

* Open ```/usr/local/antmedia/conf/red5.properties``` file.
* The default configuration lets all IPs access the Web panel.

  `server.allowed_dashboard_CIDR=0.0.0.0/0`
* Change the configuration according to your CIDR notation. You can add comma-separated CIDR notations as well.

  `server.allowed_dashboard_CIDR=13.197.23.11/16,87.22.34.66/8`

Save the file and restart the server.

Now only the IPs that are in the CIDR block can access the Web panel.

**Here is the IP Filter Demo:**

![](@site/static/img/ip-filter(1).gif)

For more details, you can check this link [IP Filter Gif](https://raw.githubusercontent.com/wiki/ant-media/Ant-Media-Server/images/ip-filter.gif)
