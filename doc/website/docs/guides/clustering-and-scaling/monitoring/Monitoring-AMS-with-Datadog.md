# Monitoring AMS with Datadog

In this document, you'll learn how to monitor Ant Media Servers with Datadog. First, we need to install Datadog. Here is a step-by-step guide to installing your monitoring system.

### Step 1 - Install Datadog

Firstly, you need to create a Datadog account in [this link](https://www.datadoghq.com/). After that just go ```Integrations / Agent``` section like this URL: ```https://app.datadoghq.com/account/settings#agent/ubuntu```. Select ```Ubuntu```. Run installation command as below:

    DD_AGENT_MAJOR_VERSION=7 DD_API_KEY=XXXXXXXXXXXXXXXXXXXX DD_SITE="datadoghq.com" bash -c "$(curl -L https://s3.amazonaws.com/dd-agent/scripts/install_script.sh)"
    

Check this link for more details: https://docs.datadoghq.com/agent/

### Step 2 - Configure Datadog settings

**Enable process monitoring:**  
Edit ```process_config``` parameter in ```/etc/datadog-agent/datadog.yaml```

    process_config:
        enabled: 'true'
    

Check this link for more details: https://docs.datadoghq.com/infrastructure/process/?tab=linuxwindows

**Enable network performance monitoring:**  
Create ```system-probe.yaml``` file with example settings.

    sudo -u dd-agent cp /etc/datadog-agent/system-probe.yaml.example /etc/datadog-agent/system-probe.yaml
    

Add network config parameter in ```/etc/datadog-agent/system-probe.yaml```

    network_config: 
        enabled: true
    

Check this link for more details: https://docs.datadoghq.com/network\_monitoring/performance/setup/?tab=agentlinux

![Screenshot from 2022-02-26 01-17-44](https://user-images.githubusercontent.com/9084130/155810678-ffffc331-c44a-4892-a6f5-4c1f6bf0e48b.png)

![Screenshot from 2022-02-26 01-18-59](https://user-images.githubusercontent.com/9084130/155810785-7806210c-3bf2-4866-8fb3-007095679343.png)