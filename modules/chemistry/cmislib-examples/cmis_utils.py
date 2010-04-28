import os
from cmislib.model import CmisClient, Repository

# Connect to the CMIS repository, return a CmisClient
def cmis_connect(url, username = '', password = ''):
    print
    print "** Connecting to %s" % url
    print "You can set the repository in the environment variable CMIS_URL"

    client = CmisClient(url, username, password)

    print '** Connected to repository %s' % (client.getDefaultRepository().getRepositoryName())
    print
    return client

def cmis_connect_default(username = '', password = ''):
    url = os.environ['CMIS_URL'] if os.environ.has_key('CMIS_URL') \
        else 'http://localhost:8080/flexive-atompub/cmis/repository'
    return cmis_connect(url, username, password)

def cmis_get_folder(repository, path):
    if path == None or len(path) == 0:
        return repository.rootFolder
    else:
        return repository.getObjectByPath(path)

def cmis_filter_by_base_type(documents, base_type_id):
    return filter(lambda c : c.properties.has_key('cmis:baseTypeId') and c.properties['cmis:baseTypeId'] == base_type_id, documents)
