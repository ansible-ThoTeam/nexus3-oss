class Email:
    def __init__(self, **kwargs):
        if 'start_tls_enabled' in kwargs:
            self.start_tls_enabled = kwargs['start_tls_enabled']
        else:
            self.start_tls_enabled = None
        if 'start_tls_required' in kwargs:
            self.start_tls_required = kwargs['start_tls_required']
        else:
            self.start_tls_required = True
        if 'ssl_on_connect_enabled' in kwargs:
            self.ssl_on_connect_enabled = kwargs['ssl_on_connect_enabled']
        else:
            self.ssl_on_connect_enabled = True
        if 'ssl_server_identity_check_enabled' in kwargs:
            self.ssl_server_identity_check_enabled = kwargs['ssl_server_identity_check_enabled']
        else:
            self.ssl_server_identity_check_enabled = True
        if 'nexus_trust_store_enabled' in kwargs:
            self.nexus_trust_store_enabled = kwargs['nexus_trust_store_enabled']
        else:
            self.nexus_trust_store_enabled = True
        if 'enabled' in kwargs:
            self.enabled = kwargs['enabled']
        else:
            self.enabled = True
        if 'host' in kwargs:
            self.host = kwargs['host']
        else:
            self.host = None
        if 'port' in kwargs:
            self.port = int(kwargs['port'])
        else:
            self.port = 0
        if 'username' in kwargs:
            self.username = kwargs['username']
        else:
            self.username = None
        if 'password' in kwargs:
            self.password = kwargs['password']
        else:
            self.password = None
        if 'from_address' in kwargs:
            self.from_address = kwargs['from_address']
        else:
            self.from_address = None
        if 'subject_prefix' in kwargs:
            self.subject_prefix = kwargs['subject_prefix']
        else:
            self.subject_prefix = None
        if 'verify_address' in kwargs:
            self.verify_address = kwargs['verify_address']
        else:
            self.verify_address = None


    @property
    def enabled(self):
        return self.__enabled

    @enabled.setter
    def enabled(self, value):
        self.__enabled = value

    @property
    def host(self):
        return self.__host

    @host.setter
    def host(self, value):
        self.__host = value

    @property
    def port(self):
        return self.__port

    @port.setter
    def port(self, value):
        if self.start_tls_enabled and value == 0:
            self.__port = 587
        else:
            self.__port = value

    @property
    def username(self):
        return self.__username

    @username.setter
    def username(self, value):
        self.__username = value

    @property
    def password(self):
        return self.__password

    @password.setter
    def password(self, value):
        self.__password = value

    @property
    def from_address(self):
        return self.__from_address

    @from_address.setter
    def from_address(self, value):
        self.__from_address = value

    @property
    def subject_prefix(self):
        return self.__subject_prefix

    @subject_prefix.setter
    def subject_prefix(self, value):
        self.__subject_prefix = value

    @property
    def start_tls_enabled(self):
        return self.__start_tls_enabled

    @start_tls_enabled.setter
    def start_tls_enabled(self, value):
        self.__start_tls_enabled = value

    @property
    def start_tls_required(self):
        return self.__start_tls_required

    @start_tls_required.setter
    def start_tls_required(self, value):
        self.__start_tls_required = value

    @property
    def ssl_on_connect_enabled(self):
        return self.__ssl_on_connect_enabled

    @ssl_on_connect_enabled.setter
    def ssl_on_connect_enabled(self, value):
        self.__ssl_on_connect_enabled = value

    @property
    def ssl_server_identity_check_enabled(self):
        return self.__ssl_server_identity_check_enabled

    @ssl_server_identity_check_enabled.setter
    def ssl_server_identity_check_enabled(self, value):
        self.__ssl_server_identity_check_enabled = value

    @property
    def nexus_trust_store_enabled(self):
        return self.__nexus_trust_store_enabled

    @nexus_trust_store_enabled.setter
    def nexus_trust_store_enabled(self, value):
        self.__nexus_trust_store_enabled = value

    @property
    def verify_address(self):
        return self.__verify_address

    @verify_address.setter
    def verify_address(self, value):
        self.__verify_address = value

    # the bool vars have to be lower case strings.
    def to_dict(self):
        dict = {}
        if self.enabled:
            dict['enabled'] = 'true'
        else:
            dict['enabled'] = 'false'
        if self.host is not None:
            dict['host'] = self.host
        if self.port != 0:
            dict['port'] = self.port
        if self.username is not None:
            dict['username'] = self.username
        if self.password is not None:
            dict['password'] = self.password
        if self.from_address is not None:
            dict['fromAddress'] = self.from_address
        if self.subject_prefix is not None:
            dict['subjectPrefix'] = self.subject_prefix
        if self.start_tls_enabled:
            dict['startTlsEnabled'] = 'true'
        else:
            dict['startTlsEnabled'] = 'false'
        if self.start_tls_required:
            dict['startTlsRequired'] = 'true'
        else:
            dict['startTlsRequired'] = 'false'
        if self.ssl_on_connect_enabled:
            dict['sslOnConnectEnabled'] = 'true'
        else:
            dict['sslOnConnectEnabled'] = 'false'
        if self.ssl_server_identity_check_enabled:
            dict['sslServerIdentityCheckEnabled'] = 'true'
        else:
            dict['sslServerIdentityCheckEnabled'] = 'false'
        if self.nexus_trust_store_enabled:
            dict['nexusTrustStoreEnabled'] = 'true'
        else:
            dict['nexusTrustStoreEnabled'] = 'false'

        return dict

    def is_valid(self):
        if self.host is None or self.port == 0 or self.from_address is None:
            return False
        return True
