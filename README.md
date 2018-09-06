# Ansible Role: Nexus 3 OSS

This role installs and configures Nexus Repository Manager OSS version 3.x.

All configuration can be updated by re-running the role, except for the [blobstores](https://help.sonatype.com/display/NXRM3/Repository+Management#RepositoryManagement-BlobStores) related settings, which are immutable in nexus.

## Table of Contents
_(Created with [gh-md-toc](https://github.com/ekalinin/github-markdown-toc))_
<!-- Run gh-md-toc --insert README.md to update -->
<!--ts-->
   * [Ansible Role: Nexus 3 OSS](#ansible-role-nexus-3-oss)
      * [Table of Contents](#table-of-contents)
      * [History / Credits](#history--credits)
      * [Requirements](#requirements)
      * [Role Variables](#role-variables)
         * [General variables](#general-variables)
         * [Download dir for nexus package](#download-dir-for-nexus-package)
         * [Nexus port and context path](#nexus-port-and-context-path)
         * [Nexus OS user and group](#nexus-os-user-and-group)
         * [Nexus instance directories](#nexus-instance-directories)
         * [Nexus JVM Ram setting](#nexus-jvm-ram-setting)
         * [Admin password](#admin-password)
         * [Default anonymous access](#default-anonymous-access)
         * [Public hostname](#public-hostname)
         * [Branding capabalities](#branding-capabalities)
         * [Reverse proxy setup](#reverse-proxy-setup)
         * [LDAP configuration](#ldap-configuration)
         * [Privileges, roles and users](#privileges-roles-and-users)
         * [Content selectors](#content-selectors)
         * [Blobstores and repositories](#blobstores-and-repositories)
         * [Scheduled tasks](#scheduled-tasks)
         * [Backups](#backups)
            * [Restore procedure](#restore-procedure)
            * [Possible limitations](#possible-limitations)
      * [Dependencies](#dependencies)
      * [Example Playbook](#example-playbook)
      * [Development, Contribution and Testing](#development-contribution-and-testing)
         * [Contributions](#contributions)
         * [Testing](#testing)
            * [Groovy syntax](#groovy-syntax)
            * [Molecule default-xxxx scenarii](#molecule-default-xxxx-scenarii)
            * [Molecule selinux scenario](#molecule-selinux-scenario)
      * [License](#license)
      * [Author Information](#author-information)

<!-- Added by: olcla, at: 2018-09-06T16:28+02:00 -->

<!--te-->

## History / Credits

This role is a fork of [ansible-nexus3-oss](https://github.com/savoirfairelinux/ansible-nexus3-oss) by [@savoirfairelinux](https://github.com/savoirfairelinux) after they announced end of maintenance.
You can have a look at the following tickets in the original repository for explanations:
- https://github.com/savoirfairelinux/ansible-nexus3-oss/issues/36
- https://github.com/savoirfairelinux/ansible-nexus3-oss/issues/38

We would like to thank the original authors for the work done.

## Requirements

- Fairly Up-to-date version of ansible. We follow ansible versions during maintenance/development and will take advantage of new features if needed (and update meta/main.yml for minimum version)
- Compatible OS. Although this role should also work on debian stretch, it is only tested through travis CI on CentOS 7, Ubuntu Xenial (16.04) and Bionic (18.04), and Debian Jessie for time being.
- Java 8 (mandatory)
    - Oracle Java 8 is the official supported platform by Sonatype
    - openjdk8 is know to work and is used for deployment test on travis on the corresponding platform docker images.
    - For more information see [nexus3 system requirements](https://help.sonatype.com/display/NXRM3/System+Requirements)
- Apache HTTPD (optional)
    - Used to setup a SSL reverse-proxy
    - The following modules must be enabled in your configuration: mod_ssl, mod_rewrite, mod_proxy, mod_proxy_http, mod_headers.

(see [Dependencies](#dependencies) section below for matching roles on galaxy)

## Role Variables

Ansible variables, along with the default values (see `default/main.yml`) :

### General variables
```yaml
    nexus_version: ''
    nexus_timezone: 'UTC'
    nexus_download_url: "http://download.sonatype.com/nexus/3"
```

The role will install/upgrade-to latest nexus available version by default. You may fix the version by tuning the `nexus_version` variable. See available versions at https://www.sonatype.com/download-oss-sonatype.

If you use an older version of nexus, you should make sure you do not use features which are not available (e.g. yum hosted repositories for nexus < 3.8.0, git lfs repo for nexus < 3.3.0, etc.)

`nexus_timezone` is a Java Timezone name and can be useful in combination with `nexus_scheduled_tasks` cron expressions below.

You may change the download site for packages by tuning `nexus_download_url` (e.g. closed environment, proxy/cache on your network...). **In this case, the automatic detection of the latest version will most likelly fail and you will have to set the version to download.** If you still want to take advantage of automatic latest version detection, a call to `<your_custom_location>/latest-unix.tar.gz` must return and HTTP 302 redirect to the latest available version in your cache/proxy.

### Download dir for nexus package
```yaml
    nexus_download_dir: '/tmp'
```

Directory on target where the nexus package will be downloaded.

### Nexus port and context path
```yaml
    nexus_default_port: 8081
    nexus_default_context_path: '/'
```

Port and context path of the java nexus process. `nexus_default_context_path` has to keep the trailing slash when set, for ex. : `nexus_default_context_path: '/nexus/'`.

### Nexus OS user and group
```yaml
    nexus_os_group: 'nexus'
    nexus_os_user: 'nexus'
```

User and group used to own the nexus files and run the service, those will be created by the role if absent.

### Nexus instance directories
```yaml
    nexus_installation_dir: '/opt'
    nexus_data_dir: '/var/nexus'
    nexus_tmp_dir: '/tmp/nexus'
```

Nexus directories, `nexus_installation_dir` contains the installed executable(s), `nexus_data_dir` contains all configuration, repositories and uploaded artifacts. Note: custom blobstores paths outside of `nexus_data_dir` can be configured, see `nexus_blobstores` below.

### Nexus JVM Ram setting
```yaml
    nexus_min_heap_size: "1200M"
    nexus_max_heap_size: "{{ nexus_min_heap_size }}"
    nexus_max_direct_memory: "2G"
```
These are the defaults for Nexus. **Please do not modify those values** _unless you have read [the memory section of nexus system requirements](https://help.sonatype.com/repomanager3/system-requirements#SystemRequirements-Memory)_ and you understand what you are doing.

As a second warning, here is an extract from the above document:
> Increasing the JVM heap memory larger than recommended values in an attempt to improve performance is not recommended. This actually can have the opposite effect, causing the operating system to thrash needlessly.

### Admin password
```yaml
    nexus_admin_password: 'changeme'
```

The 'admin' account password to setup. Note : admin password change subsequent to first-time provisioning/install is *not implemented* by this role yet.
**It is strongly advised that you do not keep your password in clear text in you playbook and include it from a separate ansible-vault encrypted files (loaded with include_vars for example)**

### Default anonymous access
```yaml
    nexus_anonymous_access: false
```

Allow [anonymous access](https://help.sonatype.com/display/NXRM3/Anonymous+Access) to nexus.

### Public hostname
```yaml
    public_hostname: 'nexus.vm'
```

The fully qualified domain name under which the nexus instance will be accessible to its clients.

### Branding capabalities
```yaml
    nexus_branding_header: ""
    nexus_branding_footer: "Last provisionned {{ ansible_date_time.iso8601 }}"
```

Header and footer branding, those can contain HTML.

### Reverse proxy setup
```yaml
    httpd_setup_enable: false
    httpd_ssl_certificate_file: 'files/nexus.vm.crt'
    httpd_ssl_certificate_key_file: 'files/nexus.vm.key'
```

Setup an [SSL Reverse-proxy](https://help.sonatype.com/display/NXRM3/Run+Behind+a+Reverse+Proxy#RunBehindaReverseProxy-Example:ReverseProxySSLTerminationatBasePath), this needs httpd installed. Note : when `httpd_setup_enable` is set to `true`, nexus binds to 127.0.0.1:8081 thus *not* being directly accessible on HTTP port 8081 from an external IP.

```yaml
    httpd_copy_ssl_files: true  # Default is false
    # These specifies to the vhost where to find on the remote server file
    # system the certificate files.
    httpd_ssl_cert_file_location: "/etc/pki/tls/certs/wildcard.vm.crt"
    httpd_ssl_cert_key_location: "/etc/pki/tls/private/wildcard.vm.key"
```

Use already existing SSL certificates on the server file system for the https reverse proxy

```yaml
    httpd_default_admin_email: "admin@example.com"
```

Set httpd default admin email address

### LDAP configuration

Ldap connections and security realm are disabled by default

```yaml
    nexus_ldap_realm: false
    ldap_connections: []
```

[LDAP connection(s)](https://help.sonatype.com/display/NXRM3/LDAP) setup, each item goes as follow :

```yaml
    nexus_ldap_realm: true
  - ldap_name: 'My Company LDAP' # used as a key to update the ldap config
    ldap_protocol: 'ldaps' # ldap or ldaps
    ldap_hostname: 'ldap.mycompany.com'
    ldap_port: 636
    ldap_search_base: 'dc=mycompany,dc=net'
    ldap_auth: 'none' # or simple
    ldap_auth_username: 'username' # if auth = simple
    ldap_auth_password: 'password' # if auth = simple
    ldap_user_base_dn: 'ou=users'
    ldap_user_filter: '(cn=*)' # (optional)
    ldap_user_object_class: 'inetOrgPerson'
    ldap_user_id_attribute: 'uid'
    ldap_user_real_name_attribute: 'cn'
    ldap_user_email_attribute: 'mail'
    ldap_user_subtree: false
    ldap_map_groups_as_roles: false
    ldap_group_base_dn: 'ou=groups'
    ldap_group_object_class: 'posixGroup'
    ldap_group_id_attribute: 'cn'
    ldap_group_member_attribute: 'memberUid'
    ldap_group_member_format: '${username}'
    ldap_group_subtree: false
```

Example LDAP config for anonymous authentication (anonymous bind), this is also the "minimal" config :

```yaml
    nexus_ldap_realm: true
  - ldap_name: 'Simplest LDAP config'
    ldap_protocol: 'ldaps'
    ldap_hostname: 'annuaire.mycompany.com'
    ldap_search_base: 'dc=mycompany,dc=net'
    ldap_port: 636
    ldap_user_id_attribute: 'uid'
    ldap_user_real_name_attribute: 'cn'
    ldap_user_email_attribute: 'mail'
    ldap_user_object_class: 'inetOrgPerson'
```

Example LDAP config for simple authentication (using a DSA account) :

```yaml
    nexus_ldap_realm: true
  - ldap_name: 'LDAP config with DSA'
    ldap_protocol: 'ldaps'
    ldap_hostname: 'annuaire.mycompany.com'
    ldap_port: 636
    ldap_auth: 'simple'
    ldap_auth_username: 'cn=mynexus,ou=dsa,dc=mycompany,dc=net'
    ldap_auth_password: "{{ vault_ldap_dsa_password }}" # better keep passwords in an ansible vault
    ldap_search_base: 'dc=mycompany,dc=net'
    ldap_user_base_dn: 'ou=users'
    ldap_user_object_class: 'inetOrgPerson'
    ldap_user_id_attribute: 'uid'
    ldap_user_real_name_attribute: 'cn'
    ldap_user_email_attribute: 'mail'
    ldap_user_subtree: false
```

Example LDAP config for simple authentication (using a DSA account) + groups mapped as roles :

```yaml
    nexus_ldap_realm: true
  - ldap_name: 'LDAP config with DSA'
    ldap_protocol: 'ldaps'
    ldap_hostname: 'annuaire.mycompany.com'
    ldap_port: 636
    ldap_auth: 'simple'
    ldap_auth_username: 'cn=mynexus,ou=dsa,dc=mycompany,dc=net'
    ldap_auth_password: "{{ vault_ldap_dsa_password }}" # better keep passwords in an ansible vault
    ldap_search_base: 'dc=mycompany,dc=net'
    ldap_user_base_dn: 'ou=users'
    ldap_user_object_class: 'inetOrgPerson'
    ldap_user_id_attribute: 'uid'
    ldap_user_real_name_attribute: 'cn'
    ldap_user_email_attribute: 'mail'
    ldap_map_groups_as_roles: true
    ldap_group_base_dn: 'ou=groups'
    ldap_group_object_class: 'groupOfNames'
    ldap_group_id_attribute: 'cn'
    ldap_group_member_attribute: 'member'
    ldap_group_member_format: 'uid=${username},ou=users,dc=mycompany,dc=net'
    ldap_group_subtree: false
```

Example LDAP config for simple authentication (using a DSA account) + groups mapped as roles dynamically :

```yaml
    nexus_ldap_realm: true
  - ldap_name: 'LDAP config with DSA'
    ldap_protocol: 'ldaps'
    ldap_hostname: 'annuaire.mycompany.com'
    ldap_port: 636
    ldap_auth: 'simple'
    ldap_auth_username: 'cn=mynexus,ou=dsa,dc=mycompany,dc=net'
    ldap_auth_password: "{{ vault_ldap_dsa_password }}" # better keep passwords in an ansible vault
    ldap_search_base: 'dc=mycompany,dc=net'
    ldap_user_base_dn: 'ou=users'
    ldap_user_object_class: 'inetOrgPerson'
    ldap_user_id_attribute: 'uid'
    ldap_user_real_name_attribute: 'cn'
    ldap_user_email_attribute: 'mail'
    ldap_map_groups_as_roles: true
    ldap_map_groups_as_roles_type: 'dynamic'
    ldap_user_memberof_attribute: 'memberOf'
```

### Privileges, roles and users
```yaml
    nexus_privileges:
      - name: all-repos-read # used as key to update a privilege
        description: 'Read & Browse access to all repos'
        repository: '*'
        actions: # can be add, browse, create, delete, edit, read or  * (all)
          - read
          - browse
```

List of the [privileges](https://help.sonatype.com/display/NXRM3/Privileges) to setup. Those items are combined with the following default values :

```yaml
    _nexus_privilege_defaults:
      type: repository-view
      format: maven2
      actions:
        - read
```

```yaml
    nexus_roles:
      - id: Developpers # can map to a LDAP group id, also used as a key to update a role
        name: developers
        description: All developers
        privileges:
          - nx-search-read
          - all-repos-read
        roles: [] # references to other role names
```

List of the [roles](https://help.sonatype.com/display/NXRM3/Roles) to setup.

```yaml
    nexus_local_users: []
```

Local (non-LDAP) users/accounts to create in nexus, items go as follow :

```yaml
  - username: jenkins # used as key to update
    first_name: Jenkins
    last_name: CI
    email: support@company.com
    password: "s3cr3t"
    roles:
      - developers # role ID
```

If you want to remove old account, provide only username and set state to absent :
```yaml
 - username: olduser
   state: absent
```

### Content selectors
```yaml
  nexus_content_selectors:
  - name: docker-login
    description: Selector for docker login privilege
    search_expression: format=="docker" and path=~"/v2/"
```

For more info on Content selector see [documentation](https://help.sonatype.com/repomanager3/configuration/repository-management#RepositoryManagement-ContentSelectors)

To use content selector add new privilege with `type: repository-content-selector` and proper `contentSelector` 
```yaml
- name: docker-login-privilege
  type: repository-content-selector
  contentSelector: docker-login
  description: 'Login to Docker registry'
  repository: '*'
  actions:
  - read
  - browse
```

### Blobstores and repositories
```yaml
    nexus_delete_default_repos: false
```

Delete the repositories from the nexus install initial default configuration. This step is only executed on first-time install (when `nexus_data_dir` has been detected empty).

```yaml
    nexus_delete_default_blobstore: false
```

Delete the default blobstore from the nexus install initial default configuration. This can be done only if `nexus_delete_default_repos: true` and all configured repositories (see below) have an explicit `blob_store: custom`. This step is only executed on first-time install (when `nexus_data_dir` has been detected empty).

```yaml
    nexus_blobstores: []
    # example blobstore item :
    # - name: separate-storage
    #   type: file
    #   path: /mnt/custom/path
    # - name: s3-blobstore
    #   type: S3
    #   config:
    #     bucket: s3-blobstore
    #     accessKeyId: "{{ VAULT_ENCRYPTED_KEY_ID }}"
    #     secretAccessKey: "{{ VAULT_ENCRYPTED_ACCESS_KEY }}"
```

[Blobstores](https://help.sonatype.com/display/NXRM3/Repository+Management#RepositoryManagement-BlobStores) to create. A blobstore path and a repository blobstore cannot be updated after initial creation (any update here will be ignored on re-provisionning).

Configuring blobstore on S3 is provided as a convenience and is not part of the automated tests we run on travis. Please note that storing on S3 is only recommended for instances deployed on AWS.

```yaml
    nexus_repos_maven_proxy:
      - name: central
        remote_url: 'https://repo1.maven.org/maven2/'
        layout_policy: permissive
        # maximum_component_age: -1
        # maximum_metadata_age: 1440
      - name: jboss
        remote_url: 'https://repository.jboss.org/nexus/content/groups/public-jboss/'
        # maximum_component_age: -1
        # maximum_metadata_age: 1440
    # example with a login/password :
    # - name: secret-remote-repo
    #   remote_url: 'https://company.com/repo/secure/private/go/away'
    #   remote_username: 'username'
    #   remote_password: 'secret'
    #   # maximum_component_age: -1
    #   # maximum_metadata_age: 1440
```

Maven [proxy repositories](https://help.sonatype.com/display/NXRM3/Repository+Management#RepositoryManagement-ProxyRepository) configuration.

```yaml
    nexus_repos_maven_hosted:
      - name: private-release
        version_policy: release
        write_policy: allow_once  # one of "allow", "allow_once" or "deny"
        # negativeCacheEnabled: true
        # timeToLive: 1440
```

Maven [hosted repositories](https://help.sonatype.com/display/NXRM3/Repository+Management#RepositoryManagement-HostedRepository) configuration. Negative cache config is optionnal and will default to the above values if omitted.

```yaml
    nexus_repos_maven_group:
      - name: public
        member_repos:
          - central
          - jboss
```

Maven [group repositories](https://help.sonatype.com/display/NXRM3/Repository+Management#RepositoryManagement-RepositoryGroup) configuration.

All three repository types are combined with the following default values :

```yaml
    _nexus_repos_maven_defaults:
      blob_store: default # Note : cannot be updated once the repo has been created
      strict_content_validation: true
      version_policy: release # release, snapshot or mixed
      layout_policy: strict # strict or permissive
      write_policy: allow_once # one of "allow", "allow_once" or "deny"
      maximum_component_age: -1  # Nexus gui default. For proxies only
      maximum_metadata_age: 1440  # Nexus gui default. For proxies only
```

Docker, Pypi, Raw, Rubygems, Bower, NPM, Git-LFS and yum repository types:
see `defaults/main.yml` for these options:

```yaml
      nexus_config_pypi: false
      nexus_config_docker: false
      nexus_config_raw: false
      nexus_config_rubygems: false
      nexus_config_bower: false
      nexus_config_npm: false
      nexus_config_gitlfs: false
      nexus_config_yum: false
```

These are all false unless you override them from playbook / group_var / cli, these all utilize the same mechanism as maven.

Note that you might need to enable certain security realms if you want to use other repository types than maven. These are
false by default

```yaml
nexus_nuget_api_key_realm: false
nexus_npm_bearer_token_realm: false
nexus_docker_bearer_token_realm: false  # required for docker anonymous access
```

The Remote User Realm can also be enabled with 

```yaml
nexus_rut_auth_realm: true
```

and the header can be configured by defining

```yaml
nexus_rut_auth_header: "CUSTOM_HEADER"
```

### Scheduled tasks
```yaml
    nexus_scheduled_tasks: []
    #  #  Example task to compact blobstore :
    #  - name: compact-docker-blobstore
    #    cron: '0 0 22 * * ?'
    #    typeId: blobstore.compact
    #    task_alert_email: alerts@example.org  # optional
    #    taskProperties:
    #      blobstoreName: {{ nexus_blob_names.docker.blob }} # all task attributes are stored as strings by nexus internally
    #  #  Example task to purge maven snapshots
    #  - name: Purge-maven-snapshots
    #    cron: '0 50 23 * * ?'
    #    typeId: repository.maven.remove-snapshots
    #    task_alert_email: alerts@example.org  # optional
    #    taskProperties:
    #      repositoryName: "*"  # * for all repos. Change to a repository name if you only want a specific one
    #      minimumRetained: "2"
    #      snapshotRetentionDays: "2"
    #      gracePeriodInDays: "2"
    #    booleanTaskProperties:
    #      removeIfReleased: true
    #  #  Example task to purge unused docker manifest and images
    #  - name: Purge unused docker manifests and images
    #    cron: '0 55 23 * * ?'
    #    typeId: "repository.docker.gc"
    #    task_alert_email: alerts@example.org  # optional
    #    taskProperties:
    #      repositoryName: "*"  # * for all repos. Change to a repository name if you only want a specific one
    #  #  Example task to purge incomplete docker uploads
    #  - name: Purge incomplete docker uploads
    #    cron: '0 0 0 * * ?'
    #    typeId: "repository.docker.upload-purge"
    #    task_alert_email: alerts@example.org  # optional
    #    taskProperties:
    #      age: "24"
```

[Scheduled tasks](https://help.sonatype.com/display/NXRM3/System+Configuration#SystemConfiguration-ConfiguringandExecutingTasks) to setup. `typeId` and task-specific `taskProperties`/`booleanTaskProperties` can be guessed either:
* from the java type hierarchy of `org.sonatype.nexus.scheduling.TaskDescriptorSupport`
* by inspecting the task creation html form in your browser
* from peeking at the browser AJAX requests while manually configuring a task.

**Task properties must be declared in the correct yaml block depending on their type**:
* `taskProperties` for all string properties (i.e. repository names, blobstore names, time periods...).
* `booleanTaskProperties` for all boolean properties (i.e. mainly checkboxes in nexus create task GUI).

### Backups
```yaml
      nexus_backup_configure: false
      nexus_backup_cron: '* 0 21 * * ?'  # See cron expressions definition in nexus create task gui
      nexus_backup_dir: '/var/nexus-backup'
      nexus_backup_log: '{{ nexus_backup_dir }}/nexus-backup.log'
      nexus_restore_log: '{{ nexus_backup_dir }}/nexus-restore.log'
      nexus_backup_rotate: false
      nexus_backup_keep_rotations: 4  # Keep 4 backup rotation by default (current + last 3)
```

Backup will not be configured unless you switch `nexus_backup_configure: true`.
In this case, a scheduled script task will be configured in nexus to run
at interval specified by `nexus_backup_cron` (defaults to 21:00 every day).
See [the groovy template for this task](templates/backup.groovy.j2) for details.
This scheduled task is independent from the other `nexus_scheduled_tasks` you
declare in your playbook

If you want to rotate backups, set `nexus_backup_rotate: true` and adjust
the number of rotations you would like to keep with `nexus_backup_keep_rotations`
(defaults to 4)

Note that `nexus_backup_log` _must be writable_ by the nexus user or the **backup
task will fail**

#### Restore procedure
Run your playbook with parameter `-e nexus_restore_point=<YYYY-MM-dd-HH-mm-ss>`
(e.g. 2017-12-17-21-00-00 for 17th of December 2017 at 21h00m00s)

#### Possible limitations
Blobstore copies are made directly from nexus by the script scheduled task.
This has only been tested on rather small blobstores (less than 50Go) and should
be used with caution and tested carefully on larger installations before moving
to production. In any case, you are free to implement your own backup scenario
outside of this role.


## Dependencies

The java and httpd requirements /can/ be fulfilled with the following galaxy roles :
  - [ansiblebit.oracle-java](https://galaxy.ansible.com/ansiblebit/oracle-java/)
  - [geerlingguy.apache](https://galaxy.ansible.com/geerlingguy/apache/)

Feel free to use them or implement your own install scenario at your convenience.

## Example Playbook

```yaml

---
- name: Nexus
  hosts: nexus
  become: yes

  vars:
    nexus_timezone: 'Canada/Eastern'
    nexus_admin_password: "{{ vault_nexus_admin_password }}"
    httpd_server_name: 'nexus.vm'
    httpd_setup_enable: true
    httpd_ssl_certificate_file: "{{ vault_httpd_ssl_certificate_file }}"
    httpd_ssl_certificate_key_file: "{{ vault_httpd_ssl_certificate_key_file }}"
    ldap_connections:
      - ldap_name: 'Company LDAP'
        ldap_protocol: 'ldaps'
        ldap_hostname: 'ldap.company.com'
        ldap_port: 636
        ldap_search_base: 'dc=company,dc=net'
        ldap_user_base_dn: 'ou=users'
        ldap_user_object_class: 'inetOrgPerson'
        ldap_user_id_attribute: 'uid'
        ldap_user_real_name_attribute: 'cn'
        ldap_user_email_attribute: 'mail'
        ldap_group_base_dn: 'ou=groups'
        ldap_group_object_class: 'posixGroup'
        ldap_group_id_attribute: 'cn'
        ldap_group_member_attribute: 'memberUid'
        ldap_group_member_format: '${username}'
    nexus_privileges:
      - name: all-repos-read
        description: 'Read & Browse access to all repos'
        repository: '*'
        actions:
          - read
          - browse
      - name: company-project-deploy
        description: 'Deployments to company-project'
        repository: company-project
        actions:
          - add
          - edit
    nexus_roles:
      - id: Developpers # maps to the LDAP group
        name: developers
        description: All developers
        privileges:
          - nx-search-read
          - all-repos-read
          - company-project-deploy
        roles: []
    nexus_local_users:
      - username: jenkins # used as key to update
        first_name: Jenkins
        last_name: CI
        email: support@company.com
        password: "s3cr3t"
        roles:
          - Developpers # role ID here
    nexus_blobstores:
      - name: company-artifacts
        path: /var/nexus/blobs/company-artifacts
    nexus_scheduled_tasks:
      - name: compact-blobstore
        cron: '0 0 22 * * ?'
        typeId: blobstore.compact
        taskProperties:
          blobstoreName: 'company-artifacts'
    nexus_repos_maven_proxy:
      - name: central
        remote_url: 'https://repo1.maven.org/maven2/'
        layout_policy: permissive
      - name: alfresco
        remote_url: 'https://artifacts.alfresco.com/nexus/content/groups/private/'
        remote_username: 'secret-username'
        remote_password: "{{ vault_alfresco_private_password }}"
      - name: jboss
        remote_url: 'https://repository.jboss.org/nexus/content/groups/public-jboss/'
      - name: vaadin-addons
        remote_url: 'https://maven.vaadin.com/vaadin-addons/'
      - name: jaspersoft
        remote_url: 'https://jaspersoft.artifactoryonline.com/jaspersoft/jaspersoft-repo/'
        version_policy: mixed
    nexus_repos_maven_hosted:
      - name: company-project
        version_policy: mixed
        write_policy: allow
        blob_store: company-artifacts
    nexus_repos_maven_group:
      - name: public
        member_repos:
          - central
          - jboss
          - vaadin-addons
          - jaspersoft

  roles:
    - { role: ansiblebit.oracle-java, oracle_java_set_as_default: yes, tags: ['ansiblebit.oracle-java'] }
    # Debian/Ubuntu only
    # - { role: geerlingguy.apache, apache_create_vhosts: no, apache_mods_enabled: ["proxy_http.load", "headers.load"], apache_remove_default_vhost: true, tags: ["geerlingguy.apache"] }
    # RedHat/CentOS only
    - { role: geerlingguy.apache, apache_create_vhosts: no, apache_remove_default_vhost: true, tags: ["geerlingguy.apache"] }
    - { role: ansible-ThoTeam.nexus3-oss, tags: ['savoirfairelinux.nexus3-oss'] }
```

## Development, Contribution and Testing

### Contributions

All contributions to this role are welcome, either for bugfixes, new features or documentation.

If you wish to contribute:
- Fork the repo under your own name/organisation through github interface
- Create a branch in your own repo with a meaningfull name. We suggest the following naming convention:
  - `feature_<someFeature>` for features
  - `fix_<someBugFix>` for bug fixes
  - `docfix_<someDocFix>` for documentation only fixes
- If starting an important feature change, open a pull request early describing what you want to do so we can discuss it if needed. This will prevent you from doing a lot of hard work on a lot of code for changes that we cannot finally merge.
- If there are build error on your pull request, have a look at the travis log and fix the relevant errors.

Moreover, if you have time to devote for code review, merge for realeases, etc... drop an email to contact@thoteam.com to get in touch.


### Testing

This role includes tests and CI integration through travis. At time being, we test:
* groovy scripts syntax
* yaml syntax and coding standard (yamllint)
* ansible good practices (ansible lint)
* a set of basic deployments on 5 different linux platforms
    * centos7
    * debian jessie
    * debian stretch
    * ubuntu xenial (16.04)
    * ubuntu bionic (18.04)

#### Groovy syntax

This role contains a set of groovy files used to provision nexus.

If you submit changes to groovy files, please run the groovy syntax check locally before pushing your changes
```bash
./tests/test_groovySyntax.sh
```
This will ensure you push groovy files with correct syntax limiting the number of check errors on travis.

You will need the groovy package installed locally to run this test.

#### Molecule default-xxxx scenarii

The role is tested on travis with [molecule](https://pypi.python.org/pypi/molecule). You can run these tests locally. The best way to achieve this is through a python virtualenv. You can find some more details in [requirements.txt](requirements.txt).
```bash
# Note: the following path should be outside the working dir
virtualenv /path/to/some/pyenv
. /path/to/some/pyenv/bin/activate
pip install -r requirements.txt
molecule [create|converge|destroy|test] -s <scenario name>
deactivate
```
Please have a look at molecule documentation (a good start is `molecule --help`) for further usage.

The current proposed scenarii refer to the tested platforms (see `molecule/` directory). If you launch a scenario ans leave the container running (i.e. using `converge` for a simple deploy), you can access the running instance from your browser at https://localhost:<linkedPort>. See the `molecule/<scenario>/molecule.yml` file for detail. As a convenience, here is the correspondence between scenarii and configured ports:
* default-centos7 => https://localhost:8090
* default-debian_jessie => https://localhost:8091
* default-debian_stretch => https://localhost:8092
* default-ubuntu_16.04 => https://localhost:8093
* default-ubuntu_18.04 => https://localhost:8094

To speed up tests, molecule uses automated docker build images on docker hub:
* https://hub.docker.com/r/thoteam/ansible-centos7-apache-java/
* https://hub.docker.com/r/thoteam/ansible-debian_jessie-apache-java/
* https://hub.docker.com/r/thoteam/ansible-debian_stretch-apache-java/
* https://hub.docker.com/r/thoteam/ansible-ubuntu16_04-apache-java/
* https://hub.docker.com/r/thoteam/ansible-ubuntu18_04-apache-java/


#### Molecule selinux scenario

We included a second molecule `selinux` scenario. This one is not run on travis but can be used locally to:
* test selinux integration (on centos).
* run test and access the running vms under VirtualBox on you local machine.

If you wish to use this scenario you will need
* VirtualBox
* Vagrant
* molecule

A typical workflow runs like this:
* `molecule create -s selinux`. Once this is complete, you will see two vagrant vms (centos7 and debian-stretch) in your VirtualBox console.
These Vagrant box are taken from http://vagrant.thoteam.com
* `molecule converge -s selinux` will run the [scenario test playbook](molecule/selinux/playbook.yml) against the two vms.
You can pass additionnal variables to ansible on the command line to override playbook or default vars
(e.g. `molecule converge -s selinux -- -e nexus_backup_rotate=true`). You can converge as many times as you want.
* You can now access the gui with https://localhost:9101 (centos7) or https://localhost:9102 (debian-stretch).
You will need to add a security exception for the self signed ssl certificate. If you did not change it with a
command line var above, the default role admin password is "changeme"
* When you're happy with your testing, you can recycle the used space with `molecule destroy -s selinux`


## License

GNU GPLv3

## Author Information

See: https://github.com/ansible-ThoTeam
