class Blobstore:
    def __init__(self, **kwargs):
        if 'name' in kwargs:
            self.name = kwargs['name']
        else:
            self.name = None
        if 'path' in kwargs:
            self.path = kwargs['path']
        else:
            self.path = None
        if 'soft_quota_enabled' in kwargs:
            self.soft_quota_enabled = kwargs['soft_quota_enabled']
        else:
            self.soft_quota_enabled = 'Maintain'
        if 'soft_quota_type' in kwargs:
            self.soft_quota_type = kwargs['soft_quota_type']
        else:
            self.soft_quota_type = None
        if 'soft_quota_limit' in kwargs:
            self.soft_quota_limit = kwargs['soft_quota_limit']
        else:
            self.soft_quota_limit = 0


    @property
    def name(self):
        return self.__name

    @name.setter
    def name(self, value):
        self.__name = value

    @property
    def path(self):
        return self.__path

    @path.setter
    def path(self, value):
        self.__path = value

    @property
    def soft_quota_enabled(self):
        return self.__soft_quota_enabled

    @soft_quota_enabled.setter
    def soft_quota_enabled(self, value):
        self.__soft_quota_enabled = value

    @property
    def soft_quota_type(self):
        return self.__soft_quota_type

    @soft_quota_type.setter
    def soft_quota_type(self, value):
        if value == 'Space-Remaining' or value == 'spaceRemainingQuota':
            self.__soft_quota_type = 'spaceRemainingQuota'
        elif value == 'Space-Used' or value == 'spaceUsedQuota':
            self.__soft_quota_type = 'spaceUsedQuota'
        else:
            self.__soft_quota_type = None

    @property
    def soft_quota_limit(self):
        return self.__soft_quota_limit

    @soft_quota_limit.setter
    def soft_quota_limit(self, value):
        if value is None:
            self.__soft_quota_limit = 0
        if value < 0:
            value = 1
        if value > 0 and self.soft_quota_enabled != 'Maintain':
            self.__soft_quota_limit = value * 1000 * 1000
        if value > 0 and self.soft_quota_enabled == 'Maintain':
            self.__soft_quota_limit = value

    def is_quota_valid(self):
        if self.soft_quota_enabled == 'Enabled':
            if self.soft_quota_type is None or self.soft_quota_limit == 0:
                return False, 'Quota is Enabled, Type and Limit must be defined'
            else:
                return True, 'no failure'
        else:
            return True, 'no data to enforce'

    def build_json(self):
        data = {}
        data['path'] = self.path
        data['name'] = self.name
        if self.soft_quota_enabled == 'Enabled':
            quota = {}
            quota['type'] = self.soft_quota_type
            quota['limit'] = self.soft_quota_limit
            data['softQuota'] = quota
        return data
