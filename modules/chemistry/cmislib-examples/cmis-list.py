#!/usr/bin/env python
#
# List the contents of a repository. A folder path can be passed on the command
# line to show only children of a specific folder
#

from cmis_utils import *
import sys

def print_folder(folder, path_so_far = '/'):
    if path_so_far[-1] != '/':
        path_so_far += '/'

    print
    print path_so_far

    folders = []
    for child in folder.getChildren():
        base_type = child.properties['cmis:baseTypeId']
        if 'cmis:document' == base_type:
            print child.name
        elif 'cmis:folder' == base_type:
            print child.name + '/'
            folders.append(child)
        else:
            print '%s (%s)' % (child.name, base_type)

    for child in folders:
        print_folder(child, "%s%s/" % (path_so_far, child.properties['cmis:name']))

repo = cmis_connect_default().getDefaultRepository()
if len(sys.argv) <= 1:
    print_folder(cmis_get_folder(repo, '/'))
else:
    print_folder(cmis_get_folder(repo, sys.argv[1]))



