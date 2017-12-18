# Ansible Role: Nexus 3 OSS

This role installs and configures Nexus Repository Manager OSS version 3.x.

All configuration can be updated by re-running the role, except forthe [blobstores](https://books.sonatype.com/nexus-book/3.0/reference/admin.html#admin-repository-blobstores)-related settings, which are immutable in nexus.

* [History / Credits](#history--credits)
* [Requirements](#requirements)
* [Role Variables](#role-variables)
* [Dependencies](#dependencies)
* [Example Playbook](#example-playbook)
* [Development, Contribution and Testing](#development-contribution-and-testing)
* [Contributions](#contributions)
* [Testing](#testing)
    * [Groovy syntax](#groovy-syntax)
    * [Full role testing with molecule](#full-role-testing-with-molecule)
    * [Testing everything](#testing-everything)
* [License](#license)
* [Author Information](#author-information)

## History / Credits

This role is a fork of [ansible-nexus3-oss](https://github.com/savoirfairelinux/ansible-nexus3-oss) by [@savoirfairelinux](https://github.com/savoirfairelinux) after they announced end of maintenance.
You can have a look at the following tickets in the original repository for explanations:
- https://github.com/savoirfairelinux/ansible-nexus3-oss/issues/36
- https://github.com/savoirfairelinux/ansible-nexus3-oss/issues/38

We would like to thank the original authors for the work done.

## Requirements

- Minimum ansible version 2.2 (see meta/main.yml). Due to the use of the ansible [synchronize module](http://docs.ansible.com/ansible/latest/synchronize_module.html) you will need _version 2.3 for tests with molecule (using docker containers)_.
- This role is tested through travis CI only on CentOS 7 + Ubuntu 16.04 (Xenial) for time being
- Java 8 (mandatory)
    - Oracle Java 8 is the official supported platform by Sonatype
    - openjdk8 is know to work and is used for deployment test on travis on the corresponding platform docker images.
    - For more information see [nexus3 system requirements](https://help.sonatype.com/display/NXRM3/System+Requirements)
- Apache HTTPD (optional)
    - Used to setup a SSL reverse-proxy
    - The following modules must be enabled in your configuration: mod_ssl, mod_rewrite, mod_proxy, mod_headers.

(see [Dependencies](#dependencies) section below for matching roles on galaxy)

## Role Variables


Ansible variables, along with the default values (see `default/main.yml`) :

```yaml
    nexus_version: '3.6.1-02'
    nexus_timezone: 'UTC'
    nexus_package: "nexus-{{ nexus_version }}-unix.tar.gz"
```

The nexus version and package to install, see available versions at https://www.sonatype.com/download-oss-sonatype . `nexus_timezone` is a Java Timezone name and can be useful in combination with `nexus_scheduled_tasks` cron expressions below.

```yaml
    nexus_download_dir: '/tmp'
```

Directory on target where the nexus package will be downloaded.

```yaml
    nexus_default_port: 8081
    nexus_default_context_path: '/'
```

Port and context path of the java nexus process. `nexus_default_context_path` has to keep the trailing slash when set, for ex. : `nexus_default_context_path: '/nexus/'`.

```yaml
    nexus_os_group: 'nexus'
    nexus_os_user: 'nexus'
```

User and group used to own the nexus files and run the service, those will be created by the role if absent.

```yaml
    nexus_installation_dir: '/opt'
    nexus_data_dir: '/var/nexus'
    nexus_tmp_dir: '/tmp/nexus'
```

Nexus directories, `nexus_installation_dir` contains the installed executable(s), `nexus_data_dir` contains all configuration, repositories and uploaded artifacts. Note: custom blobstores paths outside of `nexus_data_dir` can be configured, see `nexus_blobstores` below.

```yaml
    nexus_admin_password: 'changeme'
```

The 'admin' account password to setup. Note : admin password change subsequent to first-time provisioning/install is *not implemented* by this role yet.

```yaml
    nexus_anonymous_access: false
```

Allow [anonymous access](https://books.sonatype.com/nexus-book/3.0/reference/security.html#anonymous) to nexus.

```yaml
    public_hostname: 'nexus.vm'
```

The fully qualified domain name under which the nexus instance will be accessible to its clients.

```yaml
    nexus_branding_header: ""
    nexus_branding_footer: "Last provisionned {{ ansible_date_time.iso8601 }}"
```

Header and footer branding, those can contain HTML.

```yaml
    httpd_setup_enable: false
    httpd_ssl_certificate_file: 'files/nexus.vm.crt'
    httpd_ssl_certificate_key_file: 'files/nexus.vm.key'
```

Setup an [SSL Reverse-proxy](https://books.sonatype.com/nexus-book/3.0/reference/install.html#_example_reverse_proxy_ssl_termination_at_base_path), this needs httpd installed. Note : when `httpd_setup_enable` is set to `true`, nexus binds to 127.0.0.1:8081 thus *not* being directly accessible on HTTP port 8081 from an external IP.

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

```yaml
    ldap_connections: []
```

[LDAP connection(s)](https://books.sonatype.com/nexus-book/3.0/reference/security.html#ldap) setup, each item goes as follow :

```yaml
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

```yaml
    nexus_privileges:
      - name: all-repos-read # used as key to update a privilege
        description: 'Read & Browse access to all repos'
        repository: '*'
        actions: # can be add, browse, create, delete, edit, read or  * (all)
          - read
          - browse
```

List of the [privileges](https://books.sonatype.com/nexus-book/3.0/reference/security.html#privilegeshttps://books.sonatype.com/nexus-book/3.0/reference/security.html#privileges) to setup. Those items are combined with the following default values :

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

List of the [roles](https://books.sonatype.com/nexus-book/3.0/reference/security.html#roles) to setup.

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
    #   path: /mnt/custom/path
```

[Blobstores](https://books.sonatype.com/nexus-book/3.0/reference/admin.html#admin-repository-blobstores) to create. A blobstore path and a repository blobstore cannot be updated after initial creation (any update here will be ignored on re-provisionning).

```yaml
    nexus_scheduled_tasks: []
    #  example task to compact blobstore :
    #  - name: compact-blobstore
    #    cron: '0 0 22 * * ?'
    #    typeId: blobstore.compact
    #    taskProperties:
    #      blobstoreName: 'default' # all task attributes are stored as strings by nexus internally
```

[Scheduled tasks](https://books.sonatype.com/nexus-book/reference3/admin.html#admin-system-tasks) to setup. `typeId` and task-specific `taskProperties` can be guessed either from the java type hierarchy of `org.sonatype.nexus.scheduling.TaskDescriptorSupport` or from peeking at the browser AJAX requests while manually configuring a task.

```yaml
    nexus_repos_maven_proxy:
      - name: central
        remote_url: 'https://repo1.maven.org/maven2/'
        layout_policy: permissive
      - name: jboss
        remote_url: 'https://repository.jboss.org/nexus/content/groups/public-jboss/'
    # example with a login/password :
    # - name: secret-remote-repo
    #   remote_url: 'https://company.com/repo/secure/private/go/away'
    #   remote_username: 'username'
    #   remote_password: 'secret'
```

Maven [proxy repositories](https://books.sonatype.com/nexus-book/3.0/reference/maven.html#_proxying_maven_repositories) configuration.

```yaml
    nexus_repos_maven_hosted:
      - name: private-release
        version_policy: release
        write_policy: allow_once
```

Maven [hosted repositories](https://books.sonatype.com/nexus-book/3.0/reference/maven.html#_hosting_maven_repositories) configuration.

```yaml
    nexus_repos_maven_group:
      - name: public
        member_repos:
          - central
          - jboss
```

Maven [group repositories](https://books.sonatype.com/nexus-book/3.0/reference/maven.html#_grouping_maven_repositories) configuration.

All three repository types are combined with the following default values :

```yaml
    _nexus_repos_maven_defaults:
      blob_store: default # Note : cannot be updated once the repo has been created
      strict_content_validation: true
      version_policy: release # release, snapshot or mixed
      layout_policy: strict # strict or permissive
      write_policy: allow_once # allow_once or allow
```

Docker, Pypi, Raw, Rubygems, Bower, NPM, and Git-LFS repository types:
see `defaults/main.yml` for these options:

```yaml
      nexus_config_pypi: false
      nexus_config_docker: false
      nexus_config_raw: false
      nexus_config_rubygems: false
      nexus_config_bower: false
      nexus_config_npm: false
      nexus_config_gitlfs: false
```

These are all false unless you override them from playbook / group_var / cli, these all utilize the same mechanism as maven.

## Dependencies

This role requires Ansible 2.2 or higher.

The java and httpd requirements /can/ be fulfilled with the following galaxy roles :
  - [ansiblebit.oracle-java](https://galaxy.ansible.com/ansiblebit/oracle-java/)
  - [geerlingguy.apache](https://galaxy.ansible.com/geerlingguy/apache/)

## Example Playbook

```yaml

---
- name: Nexus
  hosts: nexus
  become: yes

  vars:
    nexus_version: '3.1.0-04'
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
  - feature_<someFeature> for features
  - fix_<someBugFix> for bug fixes
- If starting an important feature change, open a pull request early describing what you want to do so we can discuss it if needed. This will prevent you from doing a lot of hard work on a lot of code for changes that we cannot finally merge.
- If there are build error on your pull request, have a look at the travis log and fix the relevant errors.

Moreover, if you have time to devote for code review, merge for realeases, etc... drop an email to contact@thoteam.com to get in touch.


### Testing

This role includes tests and CI integration through travis. For build time sake, not all tests are run on travis. Currently, only molecule deployment tests are ran automatically on every merge request creation/upate.

#### Groovy syntax

This role contains a set of groovy files used to provision nexus. Those files seldom change and tests on travis require a lot of time for setup/run. So they are not run automatically.

If you submit changes to groovy files, please run the groovy syntax check locally before pushing your changes
```bash
./tests/test_groovySyntax.sh
```

You will need the groovy package installed locally to run this test.

#### Full role testing with molecule

The role is tested on travis with [molecule](https://pypi.python.org/pypi/molecule). You can run these tests locally. The best way to achieve this is through a python virtualenv. You can find some more details in [requirements.txt](requirements.txt).
```bash
# Note: the following path should be outside the working dir
virtualenv /path/to/some/pyenv
. /path/to/some/pyenv/bin/activate
pip install -r requirements.txt
./tests/test_molecule.sh
deactivate
```

To speed up tests, molecule uses automated docker build images on docker hub:
- https://hub.docker.com/r/thoteam/ansible-ubuntu16.04-apache-java/
- https://hub.docker.com/r/thoteam/ansible-centos7-apache-java/

#### Testing everything
As a convenience, we provide a script to run all test as once:
```bash
./tests/test_all.sh
```

License
-------

GNU GPLv3

Author Information
------------------

See: https://github.com/ansible-ThoTeam
