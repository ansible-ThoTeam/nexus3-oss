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
module: nexus_script_api
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
  script_info:
    description:
     - object for interacting with the scripting API.
    suboptions:
        name:
            description: name of the script. Don't include extension. Required when script_info object is provided.
            required: True
            type: str
        content:
            description: Point at file you want to upload. Or just provide a string
            required: False
            type: str
        content_type:
            description: Type of script
            required: False
            type: str
notes:
#  - The dependency on httplib2 was removed in Ansible 2.1.
  - The module returns all the HTTP headers in lower-case.
author:
- David Good (@serienmorder)
extends_documentation_fragment: files
'''
EXAMPLES = r'''
- name: Get create task scripts
      nexus_script_api:
        url: http://10.10.3.144:8081
        endpoint_version: 'v1'
        url_username: 'admin'
        url_password: 'Potato1234'
        headers: 
          accept: "application/json"
        force_basic_auth: yes
        validate_certs: false
        method: GET
        script_info:
          name: create_task
    - name: Get create task scripts
      nexus_script_api:
        url: http://10.10.3.144:8081
        endpoint_version: 'v1'
        url_username: 'admin'
        url_password: 'Potato1234'
        headers: 
          accept: "application/json"
        force_basic_auth: yes
        validate_certs: false
        method: GET
        script_info:
          name: create_blobstores_from_list  
    - name: Post a script
      nexus_script_api:
        url: http://10.10.3.144:8081
        endpoint_version: 'v1'
        url_username: 'admin'
        url_password: 'Potato1234'
        headers: 
          accept: "application/json"
          Content-Type: "application/json"
        force_basic_auth: yes
        validate_certs: false
        method: POST
        script_info:
          name: create_blobstores_from_list
          content: "{{ lookup('file', 'files/groovy/create_blobstores_from_list.groovy') }}"
          content_type: 'groovy'
    - name: Put a script without it previously existing
      nexus_script_api:
        url: http://10.10.3.144:8081
        endpoint_version: 'v1'
        url_username: 'admin'
        url_password: 'Potato1234'
        headers: 
          accept: "application/json"
          Content-Type: "application/json"
        force_basic_auth: yes
        validate_certs: false
        method: POST
        script_info:
          name: a_thing_that_I_do
          content: "{{ lookup('file', 'files/groovy/create_blobstores_from_list.groovy') }}"
          content_type: 'groovy'
    - name: Delete a script
      nexus_script_api:
        url: http://10.10.3.144:8081
        endpoint_version: 'v1'
        url_username: 'admin'
        url_password: 'Potato1234'
        headers: 
          accept: "application/json"
          Content-Type: "application/json"
        force_basic_auth: yes
        validate_certs: false
        method: DELETE
        script_info:
          name: a_thing_that_I_do
          content: "{{ lookup('file', 'files/groovy/create_blobstores_from_list.groovy') }}"
          content_type: 'groovy'
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
from ansible.module_utils._text import to_native, to_text, to_bytes
from ansible.module_utils.common._collections_compat import Mapping, Sequence
from ansible.module_utils.urls import fetch_url, url_argument_spec
from ansible.module_utils.nexus.script import Script


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
        force_basic_auth=dict(type='bool', default=False)
    )


def build_url(baseurl, endpoint, name, method):
    if method == 'GET' and name is not None:
        return baseurl + '/service/rest/' + endpoint + '/script/' + name
    if method == 'GET':
        return baseurl + '/service/rest/' + endpoint + '/script'
    if method == 'POST':
        return baseurl + '/service/rest/' + endpoint + '/script'
    if method == 'PUT':
        return baseurl + '/service/rest/' + endpoint + '/script/' + name
    if method == 'DELETE':
        return baseurl + '/service/rest/' + endpoint + '/script/' + name

def main():
    argument_spec = nexus_get_argument_spec()
    argument_spec.update(
        url_username=dict(type='str', aliases=['user']),
        url_password=dict(type='str', aliases=['password'], no_log=True),
        method=dict(type='str', default='GET', choices=['GET', 'DELETE', 'POST']),
        return_content=dict(type='bool', default=False),
        status_code=dict(type='list', default=[200, 204]),
        timeout=dict(type='int', default=30),
        headers=dict(type='dict', default={}),
        endpoint_version=dict(type='str', default='v1'),
        script_info=dict(type='dict', options=dict(
            name=dict(type='str', default=None, required=True),
            content=dict(type='str', default=None),
            content_type=dict(type='str', default=None)
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

    dict_headers = module.params['headers']
    body = None

    if module.params['script_info'] is None:
        script = Script()
    else:
        script = Script(**module.params['script_info'])

    url = build_url(base_url, endpoint, script.name, method)
    start = datetime.datetime.utcnow()
    if method == 'GET':
        resp, content = uri(module, url, body, body_format, method,
                            dict_headers, socket_timeout)
    if not script.is_valid() and method != 'GET' and method != 'DELETE':
        uresp['msg'] = 'Not a Valid script object'
        module.fail_json(**uresp)
    if method == 'POST' and script.is_valid():
        body = to_bytes(json.dumps(script.to_dict()))
        resp, content = uri(module, url, body, body_format, method,
                            dict_headers, socket_timeout)
        uresp, u_content = resp_cont_parsing(resp, content)
        if "found duplicated key" in u_content:
            method = 'PUT'
            url = build_url(base_url, endpoint, script.name, method)
    if method == 'PUT' and script.is_valid():
        body = to_bytes(json.dumps(script.to_dict()))
        resp, content = uri(module, url, body, body_format, method,
                            dict_headers, socket_timeout)
    if method == 'DELETE' and script.name != None:
        resp, content = uri(module, url, body, body_format, method,
                            dict_headers, socket_timeout)
    elif method == 'DELETE' and script.name == None:
        uresp['msg'] = 'Script name is required for DELETE'
        module.exit_json(**uresp)
    # Make the request

    #resp, content = uri(module, url, body, body_format, method,
    #                    dict_headers, socket_timeout)
    resp['elapsed'] = (datetime.datetime.utcnow() - start).seconds
    resp['status'] = int(resp['status'])
    resp['changed'] = False

    uresp, u_content = resp_cont_parsing(resp, content)
    # Don't Hard fail if unable to locate item.
    if method == "DELETE" and resp['status'] == 404:
        uresp['msg'] = 'Component does not exist'
        uresp['changed'] = False
        module.exit_json(**uresp)
    if resp['status'] not in status_code:
        uresp['msg'] = 'Status code was %s and not %s: %s' % (resp['status'], status_code, uresp.get('msg', ''))
        module.fail_json(content=u_content, **uresp)
    elif return_content:
        module.exit_json(content=u_content, **uresp)
    else:
        module.exit_json(**uresp)

def resp_cont_parsing(resp, content):
    # Transmogrify the headers, replacing '-' with '_', since variables don't
    # work with dashes.
    # In python3, the headers are title cased.  Lowercase them to be
    # compatible with the python2 behaviour.
    uresp = {}
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

    return uresp, u_content


if __name__ == '__main__':
    main()