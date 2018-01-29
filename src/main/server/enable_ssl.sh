#!/bin/bash


if [ -z "$1" ]; then
  echo "Please give domain name as the parameter."
  echo "Sample usage:"
  echo "$0 example.com "
  exit 1
fi

domain=$1

INSTALL_DIRECTORY=/usr/local/antmedia

if [ ! -z "$2" ]; then
  INSTALL_DIRECTORY=$2
fi



if [ ! -d "$INSTALL_DIRECTORY" ]; then
  # Control will enter here if $DIRECTORY doesn't exist.
  echo "Ant Media Server does not seem to be installed to $INSTALL_DIRECTORY"
  echo "Please install Ant Media Server with the install script or give second parameter as installation path"
  echo "Sample usage:"
  echo "$0 example.com  /home/ubuntu/antmedia"
  echo "If you need help, please contact@antmedia.io"
  exit 1
fi



read -sp 'Create Password For SSL Certificate:' password

echo ""

ERROR_MESSAGE="There is a problem in installing SSL to Ant Media Server.\n Please take a look at the logs above and try to fix.\n If you do not have any idea, contact@antmedia.io"

SUDO="sudo"
if ! [ -x "$(command -v sudo)" ]; then
  SUDO=""
fi

# Install required libraries
$SUDO apt-get update -y -qq
OUT=$?
if [ $OUT -ne 0 ]; then
  echo -e $ERROR_MESSAGE
  exit $OUT
fi

$SUDO apt-get install software-properties-common -y -qq
OUT=$?
if [ $OUT -ne 0 ]; then
  echo -e $ERROR_MESSAGE
  exit $OUT
fi

$SUDO add-apt-repository ppa:certbot/certbot -y 
OUT=$?
if [ $OUT -ne 0 ]; then
  echo -e $ERROR_MESSAGE
  exit $OUT
fi

$SUDO apt-get update -qq -y
OUT=$?
if [ $OUT -ne 0 ]; then
  echo -e $ERROR_MESSAGE
  exit $OUT
fi

$SUDO apt-get install certbot -qq -y
OUT=$?
if [ $OUT -ne 0 ]; then
  echo -e $ERROR_MESSAGE
  exit $OUT
fi

#Get certificate
sudo certbot certonly --standalone -d $domain
OUT=$?
if [ $OUT -ne 0 ]; then
  echo -e $ERROR_MESSAGE
  exit $OUT
fi

file="/etc/letsencrypt/live/$domain/keystore.jks"
if [ -f "$file" ]; then
   $SUDO keytool -delete -alias tomcat -storepass $password -keystore $file
   OUT=$?
   if [ $OUT -ne 0 ]; then
     echo -e $ERROR_MESSAGE
     exit $OUT
   fi
fi


file="/etc/letsencrypt/live/$domain/truststore.jks"
if [ -f "$file" ]; then
   $SUDO keytool -delete -alias tomcat -storepass $password -keystore $file
   OUT=$?
   if [ $OUT -ne 0 ]; then
      echo -e $ERROR_MESSAGE
      exit $OUT
   fi
fi   


$SUDO openssl pkcs12 -export \
    -in /etc/letsencrypt/live/$domain/fullchain.pem \
    -inkey /etc/letsencrypt/live/$domain/privkey.pem \
    -out /etc/letsencrypt/live/$domain/fullchain_and_key.p12 \
    -name tomcat \
    -password pass:$password
OUT=$?
if [ $OUT -ne 0 ]; then
  echo -e $ERROR_MESSAGE
  exit $OUT
fi


$SUDO keytool -importkeystore \
       -deststorepass $password \
       -destkeypass $password \
       -destkeystore /etc/letsencrypt/live/$domain/keystore.jks \
       -srckeystore /etc/letsencrypt/live/$domain/fullchain_and_key.p12 \
       -srcstoretype pkcs12 \
       -srcstorepass $password \
       -alias tomcat \
       -deststoretype pkcs12
OUT=$?
if [ $OUT -ne 0 ]; then
  echo -e $ERROR_MESSAGE
  exit $OUT
fi

$SUDO keytool -export  \
         -alias tomcat \
         -deststorepass $password \
         -file /etc/letsencrypt/live/$domain/tomcat.cer \
         -keystore /etc/letsencrypt/live/$domain/keystore.jks
OUT=$?
if [ $OUT -ne 0 ]; then
 echo -e $ERROR_MESSAGE
 exit $OUT
fi

$SUDO keytool -import -trustcacerts -alias tomcat \
  -file /etc/letsencrypt/live/$domain/tomcat.cer \
  -keystore /etc/letsencrypt/live/$domain/truststore.jks \
  -storepass $password -noprompt
OUT=$?
if [ $OUT -ne 0 ]; then
 echo -e $ERROR_MESSAGE
 exit $OUT
fi

$SUDO cp /etc/letsencrypt/live/$domain/truststore.jks $INSTALL_DIRECTORY/conf/
OUT=$?
if [ $OUT -ne 0 ]; then
 echo -e $ERROR_MESSAGE
 exit $OUT
fi

$SUDO cp /etc/letsencrypt/live/$domain/keystore.jks $INSTALL_DIRECTORY/conf/
OUT=$?
if [ $OUT -ne 0 ]; then
 echo -e $ERROR_MESSAGE
 exit $OUT
fi


$SUDO sed -i "/rtmps.keystorepass=password/c\rtmps.keystorepass=$password"  $INSTALL_DIRECTORY/conf/red5.properties
OUT=$?
if [ $OUT -ne 0 ]; then
 echo -e $ERROR_MESSAGE
 exit $OUT
fi

$SUDO sed -i "/rtmps.truststorepass=password/c\rtmps.truststorepass=$password"  $INSTALL_DIRECTORY/conf/red5.properties
OUT=$?
if [ $OUT -ne 0 ]; then
 echo -e $ERROR_MESSAGE
 exit $OUT
fi

#cp default jee-container to jee-container-nossl
$SUDO cp $INSTALL_DIRECTORY/conf/jee-container.xml $INSTALL_DIRECTORY/conf/jee-container-nossl.xml
OUT=$?
if [ $OUT -ne 0 ]; then
 echo -e $ERROR_MESSAGE
 exit $OUT
fi

#cp jee-container-ssl to jee-container
$SUDO cp $INSTALL_DIRECTORY/conf/jee-container-ssl.xml $INSTALL_DIRECTORY/conf/jee-container.xml
OUT=$?
if [ $OUT -ne 0 ]; then
 echo -e $ERROR_MESSAGE
 exit $OUT
fi

$SUDO service antmedia stop

OUT=$?
if [ $OUT -ne 0 ]; then
 echo -e $ERROR_MESSAGE
 exit $OUT
fi

$SUDO service antmedia start

OUT=$?
if [ $OUT -ne 0 ]; then
 echo -e $ERROR_MESSAGE
 exit $OUT
fi


echo "SSL certificate is installed."
echo "Https port: 5443"
echo "WebSocket Secure port: 8082"
echo "You can use this url: https://$domain:5443/"