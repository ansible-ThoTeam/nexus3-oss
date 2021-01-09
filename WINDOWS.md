# Windows Server
There may be situations where business and/or political pressures require you deploy Nexus Repo Manager (NXRM) to a Windows server.  This role provides limited support for that situation too.  This page talks about some of the differences when deploying to a Windows server.

For a more general information about using Ansible to mange Windows servers, please refer to Ansible's [Windows Guides](https://docs.ansible.com/ansible/latest/user_guide/windows.html).  

## Execution differences
When you use this role to deploy NXRM to a Linux based server, the execution almost fully takes place on the target server (also referred to as as the managed node).  However some of the key Ansible tasks for this role cannot execute on a Windows managed node.  As a result, many aspects of the role execute directly on the Windows managed node (such as installing NXRM, updating config files on local filesystem, creating Windows firewall rules), but other activities involve the Ansible controller talking to managed node (ex. communicating with NXRM over REST API to add/configure repos).

All said, it is important your playbook file override certain variable defaults so API calls are correctly routed between controller and the node running NXRM:
```yaml
    # since NXRM node is Windows, REST API calls to configure NXRM will be issued by controller
    # this needs to be a hostname that resolves to the NXRM node from the controller
    nexus_public_hostname: 'nexus.example.com'
    nexus_api_hostname: "{{ nexus_public_hostname }}"
    nexus_api_scheme: "{{ nexus_public_scheme }}"
```

## Path Formatting
The [documents and examples](./README.md) for this role assume a Linux OS.  When using those examples to build a play book that will deploy to a Windows server, pay particular attention to the [path formatting for Windows](https://docs.ansible.com/ansible/latest/user_guide/windows_usage.html#path-formatting-for-windows)

For example:
```yaml
    # base directories on server
    nexus_installation_dir: 'C:\nexus'
    nexus_data_dir: 'D:\nexus-data'
```

## No Reverse Proxy
This role does not support the reverse proxy approach when deploying to Windows.  If you'd like to extend it include such support please see guidance on [making contributions](./README.md#contributions).

However, you can configure your playbook such that NXRM will listen directly on port 80 or, even better, port 443.

### HTTP on port 80

```yaml
    nexus_default_port: 80
```

### Setting up HTTPS

```yaml
    # 0 value will cause HTTP to be disabled
    nexus_default_port: 0
    # technically, this can be any port number
    nexus_ssl_port: 443
    nexus_public_scheme: https

    #make sure API calls from controller to node are routed correctly too
    nexus_api_port: "{{ nexus_ssl_port }}"
    nexus_api_scheme: "{{ nexus_public_scheme }}"

    # if using SSL with a cert that is self signed, you may need to disable validation
    nexus_api_validate_certs: false

    # WARNING: values for these vars should really be pulled from a secrets servers, not coded into your playbook
    
    # base64 encode of pkcs12 formatted file (containing both cert and private key)
    # the value in this example is not a valid base64 encoded cert
    nexus_ssl_certificate: >-
      MIIJoQIBAzCCCWcGCSqGSIb3DQEHAaCCCVgEgglUMIIJUDCCBAcGCSqGSIb3DQEHBqCCA/gwggP0
      AgEAMIID7QYJKoZIhvcNAQcBMBwGCiqGSIb3DQEMAQYwDgQI9PHy7N5NnGQCAggAgIIDwHUt7VXs
      d9dXHCPtPwh0tZvvUVCrJnEn8b7iBrWhtQU0RPC1XKA+g1G0oTOeCIO5psmPQukG0BDPHBe13oPX
    
    # the pkcs12 cert files must be password protected (a quirk of java keytool)
    nexus_ssl_certificate_password: "password"
```