import os

import testinfra.utils.ansible_runner

testinfra_hosts = testinfra.utils.ansible_runner.AnsibleRunner(
    os.environ['MOLECULE_INVENTORY_FILE']).get_hosts('nexus')


def test_npm_scoped_package_download(host):
    test_package_url = \
        "https://localhost/repository/npm-public/@angular%2fcore"

    get_url_options = \
        "url='%s' dest='/tmp/testfile' force='yes' validate_certs='no'" \
        % (test_package_url)

    download = host.ansible(
               "get_url",
               get_url_options,
               check=False
               )

    assert download["status_code"] == 200
    assert download["state"] == 'file'
