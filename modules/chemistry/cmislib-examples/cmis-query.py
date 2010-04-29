#!/usr/bin/env python
#
# Submit CMIS-SQL queries against the repository set in CMIS_URL
#

from cmis_utils import *
import sys, os

if len(sys.argv) < 2:
    print 'cmis-query.py - submit CMIS SQL queries'
    print 'Usage: cmis-query.py [--download] <query>'
    print
    print 'Parameters:'
    print '    --download       Download documents to the current folder'
    print
    print 'For example: cmis-query.py "select * from cmis:document order by cmis:name"'
    print
    exit(1)

repo = cmis_connect_default().getDefaultRepository()

download = False
if sys.argv[1] == '--download':
    download = True
    query = sys.argv[2]
else:
    query = sys.argv[1]

print '** Submitting query: %s' % query
results = repo.query(query)
cmis_print_results(results)

if download:
    for result in results:
        cmis_download_document(repo.getObject(result.properties['cmis:objectId']))

