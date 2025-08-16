#!/bin/bash

# This script lets you install SSL(HTTPS) to your Ant Media Server.
# - Free Domain: If you don't have any domain and you're an enterprise user, just type:
#   `sudo ./enable_ssl.sh `.
#   It will give you an auto-generated subdomain of antmedia.cloud and you'll have the SSL installed
#   with Let's Encrypt
#
# - Custom Domain: If you have your own domain name, you can install with your custom domain name
#   easily as well. Assign your domain to your server and Just type:
#   `sudo ./enable_ssl.sh -d {TYPE_YOUR_DOMAIN}`
#   It will give you the SSL with Let's Encrpt
#
# - Custom Certificate: If you have certificate from your provider, assing your domain and Just type:
#   `sudo ./enable_ssl.sh -f {FULL_CHAIN_FILE} -p {PRIVATE_KEY_FILE} -c {CHAIN_FILE} -d {DOMAIN_NAME}
#
# For information type
# `./enable_ssl.sh -h`

INSTALL_DIRECTORY=/usr/local/antmedia

FULL_CHAIN_FILE=
PRIVATE_KEY_FILE=
CHAIN_FILE=
domain=""
password=
renew_flag='false'
freedomain=""
restart_service='true' #if this is true, it restarts the service at the end of the script otherwise it does not

helpRequest='false'

while getopts i:d:v:p:e:f:rshc: option
do
  case "${option}" in
    f) FULL_CHAIN_FILE=${OPTARG};;
    c) CHAIN_FILE=${OPTARG};;
    p) PRIVATE_KEY_FILE=${OPTARG};;
    i) INSTALL_DIRECTORY=${OPTARG};;
    d) domain=${OPTARG};;
    v) dns_validate=${OPTARG};;
    r) renew_flag='true';;
    e) email=${OPTARG};;
    h) helpRequest='true';;
    s) restart_service=${OPTARG};;
   esac
done

ERROR_MESSAGE="There is a problem in installing SSL to Ant Media Server.\n Please take a look at the logs above and try to fix.\n If you do not have any idea, contact@antmedia.io"
usage() {

  echo "Usage commands for different scenarios:"
  echo " "
  echo "- Gets free subdomain of antmedia.cloud and install SSL with Let's Encrypt. Just type:"
  echo "  $0"
  echo " "
  echo "- Install SSL for your custom domain with Let's Encrypt. Just type:"
  echo "  $0 -d {DOMAIN_NAME} [-i {INSTALL_DIRECTORY}] [-e {YOUR_EMAIL}]"
  echo " "
  echo "- Install SSL for your custom domain and authenticate options with Let's Encrypt. Just type:"
  echo "  $0 -d {DOMAIN_NAME} [-i {INSTALL_DIRECTORY}] [-v {route53 or custom}] [-e {YOUR_EMAIL}]"
  echo " "
  echo "- Install SSL with your own certificate and your custom domain. Just type:"
  echo "  $0 -f {FULL_CHAIN_FILE} -p {PRIVATE_KEY_FILE} -c {CHAIN_FILE} -d {DOMAIN_NAME} [-i {INSTALL_DIRECTORY}]"
  echo " "
  echo -e "If you have any question, send e-mail to contact@antmedia.io\n"
}

ipt_remove() {
        iptab=`iptables -t nat -n -L PREROUTING 2>/dev/null  | grep -E "REDIRECT.*dpt:80.*5080"`
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
      echo "Ubuntu, Centos, Rocky Linux and ALmaLinux are supported."
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

install_pkgs() {
    if [ -f /etc/debian_version ]; then
       
        REQUIRED_PKG=("jq" "dnsutils" "iptables")
		MISSING_PKG=()
		
		for pkg in "${REQUIRED_PKG[@]}"; do
		    if ! dpkg -s "$pkg" &> /dev/null; then
		        MISSING_PKG+=("$pkg")
		    fi
		done
		
		if [ ${#MISSING_PKG[@]} -ne 0 ]; then
		    echo "Missing packages: ${MISSING_PKG[*]}"
		    $SUDO apt update -qq
		    $SUDO apt install -y "${MISSING_PKG[@]}"
		else
		    echo "All required packages are already installed."
		fi

    elif [ -f /etc/redhat-release ]; then
        OS_VERSION=$(rpm -E %rhel)
        pkgs="jq bind-utils iptables"
        if [[ "$OS_VERSION" == "8" ]]; then
            $SUDO yum install -y https://dl.fedoraproject.org/pub/epel/epel-release-latest-8.noarch.rpm
        elif [[ "$OS_VERSION" == "9" ]]; then
            $SUDO yum install -y https://dl.fedoraproject.org/pub/epel/epel-release-latest-9.noarch.rpm
        else
            exit 1
        fi
        $SUDO yum install -y $pkgs || sudo dnf install -y $pkgs
    fi
}

# Check if there is a Container and install necessary packages
is_docker_container() {
    if [ -f /.dockerenv ]; then
        return 0
    fi

    return 1
}

SUDO="sudo"
if ! [ -x "$(command -v sudo)" ]; then
  SUDO=""
fi

if is_docker_container; then
    SUDO=""
fi

install_pkgs

output() {
  OUT=$?
      if [ $OUT -ne 0 ]; then
          echo -e $ERROR_MESSAGE
    if [ -d $TEMP_DIR ]; then
       rm -rf $TEMP_DIR
    fi
          exit $OUT
    fi
}

delete_alias() {
  if [ -f "$1" ]; then
   $SUDO keytool -delete -alias tomcat -storepass $password -keystore $file
   output
  fi
}

wait_for_dns_validation() {
  local hostname=$1

  while [ -z $(dig +short $hostname.antmedia.cloud @8.8.8.8) ]; do
    now=$(date +"%H:%M:%S")
    echo "$now > Waiting for DNS validation."
    sleep 10
  done
}


fullChainFileExist=false
if [ ! -z "$FULL_CHAIN_FILE" ] && [ -f "$FULL_CHAIN_FILE" ]; then
  fullChainFileExist=true
fi


privateKeyFileExist=false
if [ ! -z "$PRIVATE_KEY_FILE" ] && [ -f "$PRIVATE_KEY_FILE" ]; then
  privateKeyFileExist=true
fi

chainFileExist=false
if [ ! -z "$CHAIN_FILE" ] && [ -f "$CHAIN_FILE" ]; then
  chainFileExist=true
fi


if [ "$fullChainFileExist" != "$privateKeyFileExist" ]; then
   echo "Missing full chain or private key file. Please provide both or neither of them"
   usage
   exit 1
fi

# private key file should exist if it's customer ssl
if [ "$chainFileExist" != "$privateKeyFileExist" ]; then
   usage
   echo -e "Missing chain file. Please check this link: https://github.com/ant-media/Ant-Media-Server/wiki/Frequently-Asked-Questions#how-to-install-custom-ssl-by-building-full-chain-certificate-\n"
   exit 1
fi

source $INSTALL_DIRECTORY/conf/jwt_generator.sh "$INSTALL_DIRECTORY"
generate_jwt

get_freedomain(){
  hostname="ams-$RANDOM"
  #Refactor: It seems that result_marketplace is not used. On the other hand, JWT_KEY is a variable in generate_jwt
  #it's better to return JWT_KEY in generate_jwt and don't use any variable other script 
  result_marketplace=$(generate_jwt)
  get_license_key=`cat $INSTALL_DIRECTORY/conf/red5.properties  | grep  "server.licence_key=*" | cut -d "=" -f 2`
  ip=`curl -s http://checkip.amazonaws.com`
  if [ ! -z $get_license_key ]; then
    if [ `cat $INSTALL_DIRECTORY/conf/red5.properties | egrep "rtmps.keystorepass=ams-[0-9]*.antmedia.cloud"|wc -l` == "0" ]; then   
      check_api=`curl -s -X POST -H "Content-Type: application/json" "https://route.antmedia.io/create?domain=$hostname&ip=$ip&license=$get_license_key"`
      if [ $? != 0 ]; then
        echo "There is a problem with the script. Please re-run the enable_ssl.sh script."
        exit 1
      elif [ $check_api == 400 ]; then
        echo "The domain exists, please re-run the enable_ssl.sh script."
        exit 400
      elif [ $check_api == 401 ]; then
        echo "The license key is invalid."
        exit 401
      fi
      wait_for_dns_validation "$hostname"
      domain="$hostname"".antmedia.cloud"
      echo "DNS success, installing the SSL certificate."
      freedomain="true"
    else
      domain=`cat $INSTALL_DIRECTORY/conf/red5.properties |egrep "ams-[0-9]*.antmedia.cloud" -o | uniq`
    fi
  elif [ $(curl -s -L "$REST_URL" --header "ProxyAuthorization: $JWT_KEY" | jq -e '.buildForMarket' 2>/dev/null) == "true" ]; then
    check_api=`curl -s -X POST -H "Content-Type: application/json" "https://route.antmedia.io/create?domain=$hostname&ip=$ip&license=marketplace"`
    wait_for_dns_validation "$hostname"
    domain="$hostname"".antmedia.cloud"
    freedomain="true" 
  else
    echo "Please make sure you enter your license key and use the Enterprise edition."
    exit 1
  fi
}

get_new_certificate(){

  if [ "$fullChainFileExist" == false ]; then
      #  install letsencrypt and get the certificate
      echo "creating new certificate"
      distro
      if [[ "$ID" == "ubuntu" || "$ID" == "debian" ]]; then

        
        REQUIRED_PKG=("cron" "certbot" "python3-certbot-dns-route53")
		MISSING_PKG=()
		
		for pkg in "${REQUIRED_PKG[@]}"; do
		    if ! dpkg -s "$pkg" &> /dev/null; then
		        MISSING_PKG+=("$pkg")
		    fi
		done
		
		if [ ${#MISSING_PKG[@]} -ne 0 ]; then
		    echo "Missing packages: ${MISSING_PKG[*]}"
		    $SUDO apt update -qq
		    $SUDO apt install -y "${MISSING_PKG[@]}"
		else
		    echo "All required packages are already installed."
		fi
		

      elif [ "$ID" == "centos" ] || [ "$ID" == "rocky" ] || [ "$ID" == "almalinux" ] || [ "$ID" == "rhel" ]; then
        $SUDO yum -y install epel-release
        $SUDO yum -y install certbot
        output
      fi

    # Install required libraries

    #Get certificate
    if [ -z "$email" ]; then
      if [ "$dns_validate" == "route53" ]; then
        echo -e "\033[0;31mPlease make sure you have entered the AWS access key and secret key.\033[0m"
        $SUDO certbot certonly --dns-route53 --agree-tos --register-unsafely-without-email -d "$domain"
      elif [ "$dns_validate" == "custom" ]; then
        $SUDO certbot --agree-tos --register-unsafely-without-email --manual --preferred-challenges dns --manual-public-ip-logging-ok --force-renewal certonly --cert-name $domain -d $domain
      elif [ "$freedomain" == "true" ]; then
        $SUDO certbot certonly --standalone --non-interactive --agree-tos --register-unsafely-without-email --cert-name "$domain" -d "$domain"
      else
        $SUDO certbot certonly --standalone --non-interactive --agree-tos --register-unsafely-without-email -d "$domain"
      fi
    else
      if [ "$dns_validate" == "route53" ]; then
        echo -e "\033[0;31mPlease make sure you have entered the AWS access key and secret key.\033[0m"
        $SUDO certbot certonly --dns-route53 --agree-tos --email $email -d $domain
      elif [ "$dns_validate" == "custom" ]; then
        $SUDO certbot --agree-tos --email $email --manual --preferred-challenges dns --manual-public-ip-logging-ok --force-renewal certonly --cert-name $domain -d $domain
      elif [ "$freedomain" == "true" ]; then
        $SUDO certbot certonly --standalone --non-interactive --agree-tos --email "$email" --cert-name "$domain" -d "$domain"
      else
        $SUDO certbot certonly --standalone --non-interactive --agree-tos --email "$email" -d "$domain"
      fi
    fi

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

renew_certificate() {

    echo "renewing certificate"

    if [ -n "$domain" ]; then
        $SUDO certbot certonly --standalone --non-interactive --agree-tos -d "$domain"
    else
        $SUDO certbot renew
    fi

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
    if [ -z "$domain" ]; then
      get_freedomain
    fi
}


if [ "$helpRequest" == "true" ]
then
  usage
  exit 0
fi

if [ ! -d "$INSTALL_DIRECTORY" ]; then
  # Control will enter here if $DIRECTORY doesn't exist.
  echo "Ant Media Server does not seem to be installed to $INSTALL_DIRECTORY"
  echo "Please install Ant Media Server with the install script or give as a parameter"
  usage
  exit 1
fi

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


$SUDO sed -i "/server.name=/c\server.name=$domain"  "$INSTALL_DIRECTORY/conf/red5.properties"

#change rtmps.enabled if it exits. If it does not exist, add it
$SUDO sed -i "/rtmps.enabled=/c\rtmps.enabled=true" "$INSTALL_DIRECTORY/conf/red5.properties" || echo "rtmps.enabled=true" | $SUDO tee -a "$INSTALL_DIRECTORY/conf/red5.properties"

#restore iptables redirect rule
ipt_restore

echo ""

if is_docker_container; then
    echo "You are running Ant Media Server in a Docker container. Please restart the container to apply the changes."
    kill -HUP 1
elif [ "$restart_service" == "true" ]; then 
    echo "Restarting Ant Media Server..."
    $SUDO service antmedia restart
else
    echo "Ant Media Server is not restarted because script is called just to install the ssl. Please restart it manually to apply the changes."
fi

output

echo "SSL certificate is installed."
echo "Https port: 5443"
echo "You can use this url: https://$domain:5443/"

#remove temp dir
$SUDO rm -rf $TEMP_DIR
