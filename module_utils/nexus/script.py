class Script:
    def __init__(self, **kwargs):
        if 'name' in kwargs:
            self.name = kwargs['name']
        else:
            self.name = None
        if 'content' in kwargs:
            self.content = kwargs['content']
        else:
            self.content = None
        if 'path_to_content' in kwargs:
            self.path_to_content = kwargs['path_to_content']
        else:
            self.path_to_content = None
        if 'content_type' in kwargs:
            self.content_type = kwargs['content_type']
        else:
            self.content_type = None


    @property
    def name(self):
        return self.__name

    @name.setter
    def name(self, value):
        self.__name = value

    @property
    def content(self):
        return self.__content

    @content.setter
    def content(self, value):
        self.__content = value

    @property
    def content_type(self):
        return self.__content_type

    @content_type.setter
    def content_type(self, value):
        self.__content_type = value

    def is_valid(self):
        return self.name is not None and self.content is not None and self.content_type is not None

    def to_dict(self):
        dict = {}
        if self.name is not None:
            dict['name'] = self.name
        if self.content is not None:
            dict['content'] = self.content
        if self.content_type is not None:
            dict['type'] = self.content_type
        return dict

