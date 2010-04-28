#!/usr/bin/env python
#
# Download all binary files to the current directory, creating subdirectories
# for tree folders. If a folder path is set on the command line, only
# children of the folder are downloaded.
#

from cmis_utils import *
import sys, os

# download all items in folder
def download_folder(folder):
    children = folder.getChildren() 
    for child in cmis_filter_by_base_type(children, 'cmis:document'):
        try:
            stream = child.getContentStream()
            print "Downloading: " + child.name
            f = open(child.name, 'wb')
            f.write(stream.read())
            stream.close()
            f.close()
        except:
            pass    # ignore, happens if content has no content stream

    for child in cmis_filter_by_base_type(children, 'cmis:folder'):
        os.mkdir(child.name)
        os.chdir(child.name)
        download_folder(child)
        os.chdir("..")

repo = cmis_connect_default().getDefaultRepository()
if len(sys.argv) <= 1:
    download_folder(repo.rootFolder)
else:
    download_folder(repo.getObjectByPath(sys.argv[1]))



