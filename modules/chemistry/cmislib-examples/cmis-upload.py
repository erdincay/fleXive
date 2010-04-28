#!/usr/bin/env python
# 
# Upload file(s) to the repository under the given path
#

from cmis_utils import *
import sys, os, os.path

if len(sys.argv) < 3:
    print "upload.py - Upload local files to a CMIS repository"
    print "Usage: upload.py target-folder file(s)..."
    print
    print "The CMIS repository URL can be set in the CMIS_URL environment variable."
    print

    exit(1)

repo = cmis_connect_default().getDefaultRepository()
   
# make paths
path = ""
folder = repo.rootFolder
for name in sys.argv[1].split('/'):    
    if len(name) > 0:
        path = path + '/' + name
        try:
            folder = repo.getObjectByPath(path)
        except:
            # folder does not exist, create
            folder = folder.createFolder(name)

# upload documents
for file_name in sys.argv[2:]:
    if not os.path.isdir(file_name):
        print 'Uploading %s to %s...' % (file_name, path)
        stripped_name = file_name[file_name.rfind(os.sep) + 1:]
        folder.createDocument(stripped_name, contentFile = open(file_name, 'rb'))

