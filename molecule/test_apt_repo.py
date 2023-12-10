"""testinfra file for apt specific tests."""

import os
import testinfra.utils.ansible_runner

"""
These test should only run on debian based distributions
"""

testinfra_hosts = testinfra.utils.ansible_runner.AnsibleRunner(
    os.environ["MOLECULE_INVENTORY_FILE"]
).get_hosts("nexus")

apt_pub_key = """
-----BEGIN PGP PUBLIC KEY BLOCK-----

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
=Oq1K
-----END PGP PUBLIC KEY BLOCK-----
"""
apt_gpg_target = "/usr/share/keyrings/private_nexus.gpg"
apt_private_repo = "private_ubuntu_18.04"

nexushello_version = "1.0.2"
nexushello_distribution = "bionic"


def test_apt_package_upload(host: testinfra.host.Host):
    """Test we can upload an apt package to repository."""
    # Copy debian test package
    host.ansible(
        "get_url",
        "url=https://github.com/ansible-ThoTeam/nexushello-apt-package/releases"
        f"/download/v{nexushello_version}/nexushello_{nexushello_version}_all.deb dest=/tmp",
        check=False,
    )

    upload = host.run(
        'curl -k -u "admin:changeme" -H "Content-Type: multipart/form-data" '
        f'--data-binary "@./nexushello_{nexushello_version}_all.deb" '
        f'"https://localhost/repository/{apt_private_repo}/"'
    )

    assert upload.exit_status == 0

    # Import gpg key of our repo
    host.run(f'echo "{apt_pub_key}" | gpg --dearmor > {apt_gpg_target}')

    # Configure our private repo for apt
    host.run(
        f"echo '[arch=all signed-by={apt_gpg_target}] "
        f"deb https://localhost/repository/{apt_private_repo} {nexushello_distribution} main "
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

    # Install package
    host.ansible(
        "apt",
        "name=nexushello state=present update-cache=true",
        check=False,
        become=True,
    )

    assert host.run("nexushello").stdout == "Hello nexus!\n"
