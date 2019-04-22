from __future__ import (absolute_import, division, print_function)
__metaclass__ = type

from ansible.errors import AnsibleFilterError
import json


class FilterModule(object):
    """
    nexus3-oss role filters
    """
    def filters(self):
        return {
            'nexus_groovy_error': self.nexus_groovy_error,
            'nexus_groovy_changed': self.nexus_groovy_changed,
            'nexus_groovy_details': self.nexus_groovy_details
        }

    def nexus_groovy_error(self, data):
        """
        Check if the passed uri module call data has returned an error

        :param data: A registered var after calling the nexus groovy script though uri module
        :return: boolean: True if error, False otherwise
        """
        return self._nexus_groovy_result(data, 'error')

    def nexus_groovy_changed(self, data):
        """
        Check if the passed uri module call data has returned a changed state

        :param data: A registered var after calling the nexus groovy script though uri module
        :return: boolean: True if changed, False otherwise
        """
        return self._nexus_groovy_result(data, 'changed')

    def nexus_groovy_details(self, data):
        """
        Returns the action_details part of the groovy call result if available or
        some as relevant as possible info

        :param data: A registered var after calling the nexus groovy script though uri module
        :return: A list of maps for each action in the script if available or a string with the best relevant info
        """
        return self._nexus_groovy_result(data, 'action_details')

    def _nexus_groovy_result(self, data, element):
        """
        Inspect data from an uri module call to a custom groovy script in nexus
        and return the required element. This is based on a specific json
        we return in result for groovy script in this role. If the result does
        not contain the expected params or is not in json format, changed will always
        be False.

        The element can be:
        - error: True if the call did not return a 200 status or error is True in result
        - changed: True if changed is True in result
        - details: a list of maps with details for each action taken in the script

        :param data: A registered var after calling the script though uri module
        :param element: The desired element (error, changed, action_details)
        :return: True/False or a list of maps with details.
        """

        valid_elements = ['error', 'changed', 'action_details']
        if element not in valid_elements:
            raise AnsibleFilterError("The element parameter must be one of {}".format(",".join(valid_elements)))

        return self._get_script_run_results(data)[element]

    def _get_script_run_results(self, data):

        try:
            request_status = data['status']
        except KeyError:
            raise AnsibleFilterError("The input data is not valid. It does not contain the key 'status'. "
                                     "Is it a var registered from a uri: module call ?")

        try:
            json_data = data['json']
        except KeyError:
            raise AnsibleFilterError("The input data is not valid. It does not contain the key 'json'. "
                                     "Is it a var registered from a uri: module call ?")

        try:
            raw_result = json_data['result']
            if raw_result == "null":
                raise KeyError
        except KeyError:
            raw_result = None

        try:
            result = json.loads(raw_result)
            if result is None:
                raise ValueError
        except (ValueError, TypeError):
            """This is not a result in json format or result key is absent"""
            if request_status == 200:
                result = {
                    'error': False,
                    'changed': False,
                    'action_details': raw_result if raw_result else 'Script return in empty'
                }
            else:
                result = {
                    'error': True,
                    'changed': False,
                    'action_details': raw_result if raw_result else "Global script failure"
                }
        except Exception as e:
            raise AnsibleFilterError('Filter encountered an unexpected exception: {} {}'.format(type(e), e))

        return result
