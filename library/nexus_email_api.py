#!/usr/bin/python
# -*- coding: utf-8 -*-

# Copyright: (c) 2013, Romeo Theriault <romeot () hawaii.edu>
# GNU General Public License v3.0+ (see COPYING or https://www.gnu.org/licenses/gpl-3.0.txt)

from __future__ import absolute_import, division, print_function

__metaclass__ = type

ANSIBLE_METADATA = {'metadata_version': '1.1',
                    'status': ['preview'],
                    'supported_by': 'community'}

DOCUMENTATION = r'''
---
module: nexus_email_api
short_description: Interacts with Nexus Rest API
description:
  - Extends the URI module in order to interact with the Nexus Rest API.
  - Not writing a windows option. Delegate to linux machine
version_added: "1.1"
options:
  url:
    description:
      - HTTP or HTTPS URL in the form (http|https)://host.domain[:port]
    type: str
    required: true
  endpoint_version:
    description:
        - Version of the endpoint. Defaults to v1, but can be overloaded
        - Can be found in API after server has been setup.
    type: str
    default: v1
  url_username:
    description:
      - A username for the module to use for Digest, Basic or WSSE authentication.
    type: str
    aliases: [ user ]
  url_password:
    description:
      - A password for the module to use for Digest, Basic or WSSE authentication.
    type: str
    aliases: [ password ]
  return_content:
    description:
      - Whether or not to return the body of the response as a "content" key in
        the dictionary result.
      - Independently of this option, if the reported Content-type is "application/json", then the JSON is
        always loaded into a key called C(json) in the dictionary results.
    type: bool
    default: no
  force_basic_auth:
    description:
      - Force the sending of the Basic authentication header upon initial request.
      - The library used by the uri module only sends authentication information when a webservice
        responds to an initial request with a 401 status. Since some basic auth services do not properly
        send a 401, logins will fail.
    type: bool
    default: no
  status_code:
    description:
      - A list of valid, numeric, HTTP status codes that signifies success of the request.
    type: list
    default: [ 200 ]
  timeout:
    description:
      - The socket level timeout in seconds
    type: int
    default: 30
  use_proxy:
    description:
      - If C(no), it will not use a proxy, even if one is defined in an environment variable on the target hosts.
    type: bool
    default: yes
  email_info:
  description: Information for configuring the server.
    required:
      'GET': No
      'PUT': Yes
      'DELETE': No
      'Post': Yes
  type: dict
  elements: dict
  suboptions:
    enabled:
      description: Should the email server be enabled or disabled?
      type: bool
      required: no
      default: False
    host:
      description: URL of email server
      type: str
      required: yes
    port: 
      description: Port cannot be 0
      type: int
      required yes
    from_address:
      description: The address for the email to come from
      type: str
      required: yes
    subject_prefix:
      description: Pre-Pend string for email subject
      type: str
      required: no
    start_tls_enabled: 
      description: Is start tls enabled on mail server
      type: bool
      required: no
      default: False
    start_tls_required:
      description: is start tls required on mail server
      type: bool
      required: no
      default: False
    ssl_on_connect_enabled:
      description: is ssl on connect enabled on mail server
      type: bool
      required: no
      default: False
    ssl_server_identity_check_enabled:
      description: is ssl server identity check enabled on mail server
      type: bool
      required: no
      default: False
    nexus_trust_store_enabled:
      description: Is the nexus trust store enabled
      type: bool
      required: no
      default: False
    username:
      description: Email server username
      type: str
      required: no
    password:
      description: Email server username's password
      type: str
      required: no 
notes:
#  - The dependency on httplib2 was removed in Ansible 2.1.
  - The module returns all the HTTP headers in lower-case.
author:
- David Good (@serienmorder)
extends_documentation_fragment: files
'''
EXAMPLES = r'''
- name: Configure Email Server
  nexus_email_api:
    url: http://{{nexus_application_host}}
    endpoint_version: 'beta'
    url_username: "{{nexus_administrator_username}}"
    url_password: "{{current_nexus_admin_password}}"
    headers: 
      accept: "application/json"
    force_basic_auth: yes
    validate_certs: false
    method: PUT
    email_info:
      enabled: "{{ nexus_email_server_enabled }}"
      host: "{{ nexus_email_server_host }}"
      port: "{{ nexus_email_server_port }}"
      from_address: "{{ nexus_email_from_address }}"
      subject_prefix: "{{ nexus_email_subject_prefix }}"
      start_tls_enabled: "{{ nexus_email_tls_enabled }}"
      start_tls_required: "{{ nexus_email_tls_required }}"
      ssl_on_connect_enabled: "{{ nexus_email_ssl_on_connect_enabled }}"
      ssl_server_identity_check_enabled: "{{ nexus_email_ssl_check_server_identity_enabled }}"
      nexus_trust_store_enabled: "{{ nexus_email_trust_store_enabled }}"
      username: "{{ nexus_email_server_username }}"
      password: "{{ nexus_email_server_password }}"
'''

RETURN = r'''
# The return information includes all the HTTP headers in lower-case.
content:
  description: The response body content.
  returned: status not in status_code or return_content is true
  type: str
  sample: "{}"
cookies:
  description: The cookie values placed in cookie jar.
  returned: on success
  type: dict
  sample: {"SESSIONID": "[SESSIONID]"}
  version_added: "2.4"
cookies_string:
  description: The value for future request Cookie headers.
  returned: on success
  type: str
  sample: "SESSIONID=[SESSIONID]"
  version_added: "2.6"
elapsed:
  description: The number of seconds that elapsed while performing the download.
  returned: on success
  type: int
  sample: 23
msg:
  description: The HTTP message from the request.
  returned: always
  type: str
  sample: OK (unknown bytes)
status:
  description: The HTTP status code from the request.
  returned: always
  type: int
  sample: 200
url:
  description: The actual URL used for the request.
  returned: always
  type: str
  sample: https://www.ansible.com/
'''
import cgi
import datetime
import json
import os
import re
import shutil
import sys
import tempfile

from ansible.module_utils.basic import AnsibleModule
from ansible.module_utils.six import PY2, iteritems, string_types
from ansible.module_utils.six.moves.urllib.parse import urlencode, urlsplit
from ansible.module_utils._text import to_native, to_text
from ansible.module_utils.common._collections_compat import Mapping, Sequence
from ansible.module_utils.urls import fetch_url, url_argument_spec
from ansible.module_utils.nexus.email import Email

JSON_CANDIDATES = ('text', 'json', 'javascript')


def format_message(err, resp):
    msg = resp.pop('msg')
    return err + (' %s' % msg if msg else '')


def absolute_location(url, location):
    """Attempts to create an absolute URL based on initial URL, and
    next URL, specifically in the case of a ``Location`` header.
    """

    if '://' in location:
        return location

    elif location.startswith('/'):
        parts = urlsplit(url)
        base = url.replace(parts[2], '')
        return '%s%s' % (base, location)

    elif not location.startswith('/'):
        base = os.path.dirname(url)
        return '%s/%s' % (base, location)

    else:
        return location


def uri(module, url, body, body_format, method, headers, socket_timeout):
    # is dest is set and is a directory, let's check if we get redirected and
    # set the filename from that url
    redirected = False
    redir_info = {}
    r = {}

    src = module.params['src']
    if src:
        try:
            headers.update({
                'Content-Length': os.stat(src).st_size
            })
            data = open(src, 'rb')
        except OSError:
            module.fail_json(msg='Unable to open source file %s' % src, elapsed=0)
    else:
        data = body

    kwargs = {}

    resp, info = fetch_url(module, url, data=data, headers=headers,
                           method=method, timeout=socket_timeout,
                           **kwargs)

    try:
        content = resp.read()
    except AttributeError:
        # there was no content, but the error read()
        # may have been stored in the info as 'body'
        content = info.pop('body', '')

    if src:
        # Try to close the open file handle
        try:
            data.close()
        except Exception:
            pass

    r['redirected'] = redirected or info['url'] != url
    r.update(redir_info)
    r.update(info)

    return r, content


def nexus_get_argument_spec():
    '''
    Creates an argument spec that can be used with any module
    that will be requesting content via urllib/urllib2
    '''
    return dict(
        url=dict(type='str'),
        http_agent=dict(type='str', default='ansible-httpget'),
        use_proxy=dict(type='bool', default=True),
        validate_certs=dict(type='bool', default=True),
        url_username=dict(type='str'),
        url_password=dict(type='str', no_log=True),
        force_basic_auth=dict(type='bool', default=False),
    )


    ### If asset ID is passed, we don't care about the repository.
def build_url(baseurl, endpoint, method):
    if method == 'POST':
        return baseurl + '/service/rest/' + endpoint + '/email/verify'
    return baseurl + '/service/rest/' + endpoint + '/email'


def main():
    argument_spec = nexus_get_argument_spec()
    argument_spec.update(
        url_username=dict(type='str', aliases=['user']),
        url_password=dict(type='str', aliases=['password'], no_log=True),
        method=dict(type='str', default='GET', choices=['GET', 'DELETE', 'PUT', 'POST']),
        return_content=dict(type='bool', default=False),
        status_code=dict(type='list', default=[200, 204]),
        timeout=dict(type='int', default=30),
        src=dict(type='path'),
        headers=dict(type='dict', default={}),
        endpoint_version=dict(type='str', default='v1'),
        email_info=dict(type='dict', options=dict(
            enabled=dict(type='bool', default=True),
            host=dict(type='str', default=None),
            port=dict(type='int', default=0),
            username=dict(type='str', default=None),
            password=dict(type='str', default=None, no_log=True),
            subject_prefix=dict(type='str', default=None),
            from_address=dict(type='str', default=None),
            start_tls_enabled=dict(type='bool', default=True),
            start_tls_required=dict(type='bool', default=True),
            ssl_on_connect_enabled=dict(type='bool', default=True),
            ssl_server_identity_check_enabled=dict(type='bool', default=True),
            nexus_trust_store_enabled=dict(type='bool', default=True),
            verify_address=dict(type='str', default=None)
        ))
    )

    module = AnsibleModule(
        argument_spec=argument_spec,
        add_file_common_args=True,
        mutually_exclusive=[['body', 'src']],
    )

    base_url = module.params['url']
    endpoint = module.params['endpoint_version'].lower()
    body_format = 'json'
    method = module.params['method'].upper()
    return_content = module.params['return_content']
    status_code = [int(x) for x in list(module.params['status_code'])]
    socket_timeout = 30
    uresp = {}


    if module.params['email_info'] is None:
        email = Email()
    else:
        email = Email(**module.params['email_info'])

    dict_headers = module.params['headers']
    body = None
    if method == 'PUT':
        email_failure = email.is_valid()
        if not email_failure:
            uresp['msg'] = 'host, port, and from address must always be defined, even with enabled False'
            module.fail_json(**uresp)
        else:
            body = email.to_dict()
    if method == 'POST':
        body = email.verify_address
    url = build_url(base_url, endpoint, method)

    if body_format == 'json':
        # Encode the body unless its a string, then assume it is pre-formatted JSON
        if not isinstance(body, string_types):
            body = json.dumps(body)
        if 'content-type' not in [header.lower() for header in dict_headers]:
            dict_headers['Content-Type'] = 'application/json'

    # Make the request
    start = datetime.datetime.utcnow()
    resp, content = uri(module, url, body, body_format, method,
                        dict_headers, socket_timeout)
    resp['elapsed'] = (datetime.datetime.utcnow() - start).seconds
    resp['status'] = int(resp['status'])
    resp['changed'] = False

    # Transmogrify the headers, replacing '-' with '_', since variables don't
    # work with dashes.
    # In python3, the headers are title cased.  Lowercase them to be
    # compatible with the python2 behaviour.

    for key, value in iteritems(resp):
        ukey = key.replace("-", "_").lower()
        uresp[ukey] = value
    if 'location' in uresp:
        uresp['location'] = absolute_location(url, uresp['location'])

    # Default content_encoding to try
    content_encoding = 'utf-8'
    if 'content_type' in uresp:
        # Handle multiple Content-Type headers
        charsets = []
        content_types = []
        for value in uresp['content_type'].split(','):
            ct, params = cgi.parse_header(value)
            if ct not in content_types:
                content_types.append(ct)
            if 'charset' in params:
                if params['charset'] not in charsets:
                    charsets.append(params['charset'])

        if content_types:
            content_type = content_types[0]
            if len(content_types) > 1:
                module.warn(
                    'Received multiple conflicting Content-Type values (%s), using %s' % (
                        ', '.join(content_types), content_type)
                )
        if charsets:
            content_encoding = charsets[0]
            if len(charsets) > 1:
                module.warn(
                    'Received multiple conflicting charset values (%s), using %s' % (
                        ', '.join(charsets), content_encoding)
                )

        u_content = to_text(content, encoding=content_encoding)
        if any(candidate in content_type for candidate in JSON_CANDIDATES):
            try:
                js = json.loads(u_content)
                uresp['json'] = js
            except Exception:
                if PY2:
                    sys.exc_clear()  # Avoid false positive traceback in fail_json() on Python 2
    else:
        u_content = to_text(content, encoding=content_encoding)
    # Don't Hard fail if unable to locate item.
    if method == "POST" and resp['status'] == 200 and uresp['json']['success'] is False:
        uresp['msg'] = 'verify email failed'
        module.fail_json(content=u_content, **uresp)
    if resp['status'] not in status_code:
        uresp['msg'] = 'Status code was %s and not %s: %s' % (resp['status'], status_code, uresp.get('msg', ''))
        module.fail_json(content=u_content, **uresp)
    elif return_content:
        module.exit_json(content=u_content, **uresp)
    else:
        module.exit_json(**uresp)


if __name__ == '__main__':
    main()