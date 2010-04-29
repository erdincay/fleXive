import os
from cmislib.model import CmisClient, Repository

# Connect to the CMIS repository, return a CmisClient
def cmis_connect(url, username = '', password = ''):
    print
    print "** Connecting to %s" % url
    print "Environment variables: CMIS_URL, CMIS_USERNAME, CMIS_PASSWORD"

    client = CmisClient(url, username, password)

    print '** Connected to repository %s' % (client.getDefaultRepository().getRepositoryName())
    print
    return client

def cmis_connect_default(username = '', password = ''):
    url = os.environ.get('CMIS_URL', 'http://localhost:8080/flexive-atompub/cmis/repository')
    username = os.environ.get('CMIS_USERNAME', username)
    password = os.environ.get('CMIS_PASSWORD', password)
    return cmis_connect(url, username, password)

def cmis_get_folder(repository, path):
    if path == None or len(path) == 0:
        return repository.rootFolder
    else:
        return repository.getObjectByPath(path)

def cmis_filter_by_base_type(documents, base_type_id):
    return filter(lambda c : c.properties.has_key('cmis:baseTypeId') and c.properties['cmis:baseTypeId'] == base_type_id, documents)

# Print the results of a repository query
def cmis_print_results(results):
    print '%d results' % len(results)
    print
    for result in results:
        print 'Object %s:' % result.properties['cmis:objectId']
        for (key, value) in result.properties.items():
            print '\t%-30s %s' % (key, value)
        print

# download the given document, fails silently if the given object
# is not a document or has no content stream
def cmis_download_document(doc):
    try:
        stream = doc.getContentStream()
        print "Downloading: " + doc.name
        f = open(doc.name, 'wb')
        f.write(stream.read())
        stream.close()
        f.close()
    except:
        pass    # ignore, happens if content has no content stream

