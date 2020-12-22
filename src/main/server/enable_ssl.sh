#!/bin/bash

INSTALL_DIRECTORY=/usr/local/antmedia


FULL_CHAIN_FILE=
PRIVATE_KEY_FILE=
domain=""
password=
renew_flag='false'

while getopts i:d:p:f:rc: option
do
  case "${option}" in
    f) FULL_CHAIN_FILE=${OPTARG};;
    c) CHAIN_FILE=${OPTARG};;
    p) PRIVATE_KEY_FILE=${OPTARG};;
    i) INSTALL_DIRECTORY=${OPTARG};;
    d) domain=${OPTARG};;
    r) renew_flag='true';;
   esac
done

ERROR_MESSAGE="There is a problem in installing SSL to Ant Media Server.\n Please take a look at the logs above and try to fix.\n If you do not have any idea, contact@antmedia.io"

usage() {
  echo "Usage:"
  echo "$0 -d {DOMAIN_NAME} [-i {INSTALL_DIRECTORY}]"
  echo "$0 -f {FULL_CHAIN_FILE} -p {PRIVATE_KEY_FILE} -c {CHAIN_FILE} -d {DOMAIN_NAME} [-i {INSTALL_DIRECTORY}]"
  echo " "
  echo "If you have any question, send e-mail to contact@antmedia.io"
}

ipt_remove() {
        iptab=`iptables -t nat -n -L PREROUTING | grep -E "REDIRECT.*dpt:80.*5080"`
        if [ "$iptab" ]; then
                iptables-save > /tmp/iptables_save
                iptables -t nat -D PREROUTING -p tcp --dport 80 -j REDIRECT --to-port 5080
                IPT="1"
        fi
}

ipt_restore() {
        if [ "$IPT" ]; then
                iptables-restore < /tmp/iptables_save
        fi
}

distro () {
  os_release="/etc/os-release"
  if [ -f "$os_release" ]; then
    . $os_release
      id=$ID
  else
      echo "Ubuntu and Centos are supported."
  fi
}


get_password() {  
  until [ ! -z "$password" ]
  do
    read -sp 'Enter Password For SSL Certificate:' password
  if [ -z "$password" ]
  then
    echo 
    echo "Password cannot be empty. "
  fi
  done         
}

SUDO="sudo"
if ! [ -x "$(command -v sudo)" ]; then
  SUDO=""
fi

output() {
  OUT=$?
      if [ $OUT -ne 0 ]; then
          echo -e $ERROR_MESSAGE
          exit $OUT
    fi
}

delete_alias() {
  if [ -f "$1" ]; then
   $SUDO keytool -delete -alias tomcat -storepass $password -keystore $file
   output
  fi
}


fullChainFileExist=false
if [ ! -z "$FULL_CHAIN_FILE" ] && [ -f "$FULL_CHAIN_FILE" ]; then
  fullChainFileExist=true
fi

privateKeyFileExist=false
if [ ! -z "$PRIVATE_KEY_FILE" ] && [ -f "$PRIVATE_KEY_FILE" ]; then
  privateKeyFileExist=true
fi

if [ "$fullChainFileExist" != "$privateKeyFileExist" ]; then
   echo "Missing full chain or private key file. Please provide both or neither of them"
   usage
   exit 1
fi

if [ ! -d "$INSTALL_DIRECTORY" ]; then
  # Control will enter here if $DIRECTORY doesn't exist.
  echo "Ant Media Server does not seem to be installed to $INSTALL_DIRECTORY"
  echo "Please install Ant Media Server with the install script or give as a parameter"
  usage
  exit 1
fi

get_new_certificate(){

  if [ "$fullChainFileExist" == false ]; then
      #  install letsencrypt and get the certificate
      echo "creating new certificate"
      distro
      if [ "$ID" == "ubuntu" ]; then
          
        $SUDO apt-get update -qq -y
        output

        $SUDO apt-get install certbot -qq -y
        output
          
      elif [ "$ID" == "centos" ]; then
        $SUDO yum -y install epel-release
        $SUDO yum -y install certbot
        output
      fi

    # Install required libraries
    
    #Get certificate
    $SUDO certbot certonly --standalone --non-interactive --agree-tos --email letsencrypt@antmedia.io -d $domain
    output
    
    file="/etc/letsencrypt/live/$domain/keystore.jks"
    delete_alias $file
    
    file="/etc/letsencrypt/live/$domain/truststore.jks"
    delete_alias $file

    FULL_CHAIN_FILE="/etc/letsencrypt/live/$domain/fullchain.pem"
    CHAIN_FILE="/etc/letsencrypt/live/$domain/chain.pem"
    PRIVATE_KEY_FILE="/etc/letsencrypt/live/$domain/privkey.pem"

 fi
}

renew_certificate(){

   echo "renewing certificate"

   $SUDO certbot renew

   output
}

# We don't need keystore and truststore for Tomcat. We can use full chain and private key file directly. 
# However we need to have keystore and truststore for rtmps.  

auth_tomcat(){
    echo ""

  TEMP_DIR=$INSTALL_DIRECTORY/$domain
  if [ ! -d "$TEMP_DIR" ]; then
    $SUDO mkdir $TEMP_DIR
  fi

  if [ "$fullChainFileExist" == false ]; then
    PRIVATE_KEY_FILE="/etc/letsencrypt/live/$domain/privkey.pem"
    FULL_CHAIN_FILE="/etc/letsencrypt/live/$domain/fullchain.pem"
    CHAIN_FILE="/etc/letsencrypt/live/$domain/chain.pem"
  fi

  EXPORT_P12_FILE=$TEMP_DIR/fullchain_and_key.p12
  
  DEST_KEYSTORE=$TEMP_DIR/keystore.jks
  
  TRUST_STORE=$TEMP_DIR/truststore.jks
  
  CER_FILE=$INSTALL_DIRECTORY/$domain/tomcat.cer
  
  $SUDO openssl pkcs12 -export \
      -in $FULL_CHAIN_FILE \
      -inkey $PRIVATE_KEY_FILE \
      -out $EXPORT_P12_FILE \
      -name tomcat \
      -password pass:$password
  output
  
  
  
  $SUDO keytool -importkeystore \
         -deststorepass $password \
         -destkeypass $password \
         -destkeystore $DEST_KEYSTORE \
         -srckeystore $EXPORT_P12_FILE \
         -srcstoretype pkcs12 \
         -srcstorepass $password \
         -alias tomcat \
         -deststoretype pkcs12
  output
  
  
  $SUDO keytool -export  \
           -alias tomcat \
           -deststorepass $password \
           -file $CER_FILE \
           -keystore $DEST_KEYSTORE
  output


  $SUDO keytool -import -trustcacerts -alias tomcat \
    -file $CER_FILE \
    -keystore $TRUST_STORE \
    -storepass $password -noprompt
  output
  
  
  $SUDO cp $TRUST_STORE $INSTALL_DIRECTORY/conf/
  output


  $SUDO cp $DEST_KEYSTORE $INSTALL_DIRECTORY/conf/
  output
  
  
  $SUDO sed -i "/rtmps.keystorepass=/c\rtmps.keystorepass=$password"  $INSTALL_DIRECTORY/conf/red5.properties
  output
  
  $SUDO sed -i "/rtmps.truststorepass=/c\rtmps.truststorepass=$password"  $INSTALL_DIRECTORY/conf/red5.properties
  output
  
  
  $SUDO cp $FULL_CHAIN_FILE $INSTALL_DIRECTORY/conf/fullchain.pem
  output
  $SUDO chown antmedia:antmedia $INSTALL_DIRECTORY/conf/fullchain.pem
  
  $SUDO cp $CHAIN_FILE $INSTALL_DIRECTORY/conf/chain.pem
  output
  $SUDO chown antmedia:antmedia $INSTALL_DIRECTORY/conf/chain.pem
  
  $SUDO cp $PRIVATE_KEY_FILE $INSTALL_DIRECTORY/conf/privkey.pem
  output
  $SUDO chown antmedia:antmedia $INSTALL_DIRECTORY/conf/privkey.pem
  
  #uncomment ssl part in jee-container.xml
  $SUDO sed -i -E -e 's/(<!-- https start|<!-- https start -->)/<!-- https start -->/g' $INSTALL_DIRECTORY/conf/jee-container.xml
  output
  $SUDO sed -i -E -e 's/(https end -->|<!-- https end -->)/<!-- https end -->/g' $INSTALL_DIRECTORY/conf/jee-container.xml
  output
}

create_cron_job(){

    $SUDO crontab -l > /tmp/cronfile

    if [ $(grep -E "enable_ssl.sh" /tmp/cronfile | wc -l) -ne "0" ]; then
        sed -i '/enable_ssl.sh/d' /tmp/cronfile
        echo "00 03 */85 * * cd $INSTALL_DIRECTORY && ./enable_ssl.sh -d $domain -r" >> /tmp/cronfile
        crontab /tmp/cronfile
    else
        echo "00 03 */85 * * cd $INSTALL_DIRECTORY && ./enable_ssl.sh -d $domain -r" >> /tmp/cronfile
        crontab /tmp/cronfile
    fi

}

generate_password(){

    #user may define his own password
    #password=$(echo -n "$domain" | sha256sum)

    password="$domain"

    echo "domain: $domain"
    #echo "generated password: $password"
}

check_domain_name(){
    #check domain name exists
    if [ -z "$domain" ]; then
    echo "Missing parameter. Domain name is not set"
    usage
    exit 1
    fi
}

#check domain name
check_domain_name

#generate password using domain name
generate_password

#remove iptables redirect rule
ipt_remove

if [ "$renew_flag" == "true" ]
then

    #renew certificate
    renew_certificate

    #authenticate tomcat with certificate
    auth_tomcat

elif [ "$renew_flag" == "false" ]
then

    #install letsencrypt and get the certificate
    get_new_certificate

    #authenticate tomcat with certificate
    auth_tomcat

    #create cron job for auto renew
    if [ "$fullChainFileExist" == false ]; then
      create_cron_job
    fi
    
fi

#restore iptables redirect rule
ipt_restore

echo ""

$SUDO service antmedia stop

output

$SUDO service antmedia start

output

echo "SSL certificate is installed."
echo "Https port: 5443"
echo "You can use this url: https://$domain:5443/"

#remove temp dir
$SUDO rm -rf $TEMP_DIR
