# Install SSL on Kubernetes Using Let's Encrypt

Now let's move on to cert-manager installation.

    helm repo add jetstack https://charts.jetstack.iohelm repo updatehelm install cert-manager jetstack/cert-manager --namespace cert-manager --create-namespace --version v1.9.1 --set installCRDs=truekubectl apply -f https://github.com/jetstack/cert-manager/releases/download/v1.9.1/cert-manager.crds.yaml

Create a YAML file in your working directory and name it **ams-k8s-issuer-production.yaml** Add the following content (Do not forget to change the email address.)

    apiVersion: cert-manager.io/v1
    kind: ClusterIssuer
    metadata:
      name: letsencrypt-production
    spec:
      acme:
        server: https://acme-v02.api.letsencrypt.org/directory
        email: change_me
        privateKeySecretRef:
          name: letsencrypt-production
        solvers:
          - http01:
              ingress:
                class: nginx

Let's deploy the YAML file that we created.

    kubectl create -f ams-production-issuer.yaml

When you run the **kubectl get -n antmedia clusterissuers** command, you will see an output like the one below.

    letsencrypt-production   True    1m

We use the a**ntmedia-cert-edge** and **a****nt-media-cert-origin** secrets by default for the Origin and Edge sides, and we delete them because there are self-signed certificates.

    kubectl delete -n antmedia secret antmedia-cert-edge kubectl delete -n antmedia secret antmedia-cert-origin

You must add an annotation **cert-manager.io/cluster-issuer: letsencrypt-production** in the ingress configuration with the issuer or cluster issuer name.

    kubectl annotate ingress cert-manager.io/cluster-issuer=letsencrypt-production --all

If everything went well, the output of the **k****ubectl get -n antmedia certificate** command will show the value **True** as follows.

    NAME                   READY   SECRET                 AGEantmedia-cert-origin   True    antmedia-cert-origin   21mantmedia-cert-edge     True    antmedia-cert-edge     24m

And now you can access your Ant Media Server Cluster with your signed certificate.

**https://origin.{example.com}**

**https://edge.{example.com}**