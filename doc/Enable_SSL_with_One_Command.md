# Enable SSL for Ant Media Server

HTTPS and WSS(WebSocket Secure) is mandatory for Google Chrome to run WebRTC and WebSocket applications.
In addition, developers want to serve their content with secure connection as well. The script in this document
install Let's Encrypt SSL certificate


## Enabling SSL in Linux(Ubuntu)

Go to the folder where Ant-Media-Server is installed. Default directory is /usr/local/antmedia

```
cd /usr/local/antmedia
```

There should be a `enable_ssl.sh` file in the installation directory. 
Call the enable_ssl.sh with your domain name

```
sudo ./enable_ssl.sh  example.com
```

Make sure that your domain points to your server public IP address in the DNS records 

If the above scripts returns successfully, SSL will be installed your server, 
you can use https through 5443, wss through 8082 ports. Like below

```
https://example.com:5443
```



#### References
- [Blog Post: Enable SSL with Just One Command](https://antmedia.io/enable-ssl-on-ant-media-server/)
- [Let's Encrypt](https://letsencrypt.org/)



