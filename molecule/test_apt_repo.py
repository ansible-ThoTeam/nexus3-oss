import os
import testinfra.utils.ansible_runner

"""
These test should only run on debian based destributions
"""

testinfra_hosts = testinfra.utils.ansible_runner.AnsibleRunner(
    os.environ['MOLECULE_INVENTORY_FILE']).get_hosts('nexus')

apt_pub_key="""
-----BEGIN PGP PUBLIC KEY BLOCK-----

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
wfP1H4kG2SRX0Zj07jIqPOvnddF+
=0w7O
-----END PGP PUBLIC KEY BLOCK-----
"""

def test_apt_package_upload(host: testinfra.host.Host):
    # Copy debian test package
    host.ansible(
        "get_url",
        "url=https://github.com/ansible-ThoTeam/nexushello-apt-package/releases/download/v1.0.1/nexushello_1.0.1_all.deb dest=/tmp",
        check=False
    )

    upload = host.run(
        'curl -X POST "https://localhost/service/rest/v1/components?repository=private_ubuntu_18.04" -k '
        '-u admin:changeme '
        '-H  "accept: application/json" '
        '-H  "Content-Type: multipart/form-data" '
        '-F "apt.asset=@/tmp/nexushello_1.0.1_all.deb;type=application/vnd.debian.binary-package"'
    )
    assert upload.exit_status == 0

    # Install hello-world package on host from uploaded file
    host.run(
        "echo deb https://localhost/repository/private_ubuntu_18.04 bionic main "
        "> /etc/apt/sources.list.d/nexushello.list"
    )

    # Disable ssl verification as we use a self signed cert for tests
    host.run(
        """cat > /etc/apt/apt.conf.d/no-verify-ssl << EOF
Acquire::https {
      Verify-Peer \"false\";
      Verify-Host \"false\";
}
EOF"""
    )

    # Import gpg key of our repo
    #host.run('echo "{}" | apt-key add -'.format(apt_pub_key))
    host.run('echo "{}" > /tmp/pub.key'.format(apt_pub_key))
    host.run("apt-key add /tmp/pub.key")

    # Install package
    install_package = host.ansible(
        "apt",
        "name=nexushello state=present update-cache=true",
        check=False,
        become=True
    )

    assert host.run("nexushello").stdout == "Hello nexus !\n"



