# Copyright (C) 2007-2010 IBM and Others. All Rights Reserved

import re

from trac.core import *
from trac.util import Markup
from trac.web import IRequestHandler
from trac.web.chrome import add_stylesheet, INavigationContributor, \
                            ITemplateProvider
from trac.web.href import Href
#from trac.versioncontrol.web_ui.changeset import IChangesetRangeLink
from trac.wiki import wiki_to_html, wiki_to_oneliner, IWikiSyntaxProvider, \
                      Formatter


class DcutModule(Component):

    implements(IRequestHandler)
#    implements(IRequestHandler,IChangesetRangeLink)

    def revision_range_link(self, req, base, start, end):
        return ('DCUT Helper', req.href('dcut',old_path=base,new_path=base,old=start,new=end))

    _request_re = re.compile(r"/dcut(?:/([^/]+))?(/.*)?$")

    def match_request(self, req):
        match = re.match(self._request_re, req.path_info)
        if match:
            return True

    def process_request(self, req):

        idx = 0
        # srl
#        show_files = self.timeline_show_files
        db = self.env.get_db_cnx()
        ticketlist = {}  # dict of ticket->???
        revlist = {} # dict of revision->
        # well. check it again,.
            
        srldebug=False
        
        repos = self.env.get_repository(req.authname)
        
        new_path = req.args.get('new_path')
        new_rev = req.args.get('new')
        old_path = req.args.get('old_path')
        old_rev = req.args.get('old')

        new_path = repos.normalize_path(new_path)
        new_rev = repos.normalize_rev(new_rev)
        old_path = repos.normalize_path(old_path)
        old_rev = repos.normalize_rev(old_rev)

        old_rev = int(old_rev)
        new_rev = int(new_rev)
        
        req.hdf['changeset.diff_href'] = req.href('changeset',old_path=old_path,new=new_rev,new_path=new_path,old=old_rev)

        req.hdf['is_dcut']=1
        
        req.hdf['changeset.old_rev'] = old_rev
        req.hdf['changeset.new_rev'] = new_rev
        req.hdf['changeset.old_path'] = old_path
        req.hdf['changeset.new_path'] = new_path

        if True:
            req.hdf['target_path'] = '.';
            # okay. manually tromp through 'em
            nrev = old_rev+1
            while nrev <= new_rev:
                chgset = repos.get_changeset(nrev)
                message = chgset.message or '--'
                # can we load a ticket from it?
                splits=message.split(':')
                if message.startswith('ticket:') and len(splits)>2:
                    tickname=splits[1]
                    try:
                        ticknum=int(tickname)
                    except Exception,e:
                        nrev = nrev+1
                        continue
                    # yes, we have a ticket #
                    files=[]
                    for chg in chgset.get_changes():
                        if not chg[0].startswith(old_path):
                            continue
                        files.append(chg)
                    if len(files)==0:
                        nrev = nrev+1
                        continue # no relevant files
                    titem=(ticknum,files,chgset)
                    if ticknum in ticketlist:
                        ticketlist[ticknum].append( titem )
                    else:
                        ticketlist[ticknum]=[ titem ]
                    revlist[nrev]=titem
                else:
                    print "malformed ticket? %s at %d" % (message,nrev) # don't know the syntax for die..
                nrev = nrev+1
            if len(ticketlist):
                tickets=ticketlist.keys()
                tickets.sort()
                for ticket in tickets:
                    aticket=ticketlist[ticket]  # (ticket,files,chg)
                    cmt = 'ticket:%d - %d revs: ' % (ticket,len(aticket))
                    for rev in aticket:
                        revn = rev[2].rev
                        filecount = len(rev[1])
                        cmt = cmt + 'r%d - %d files (' % (revn,filecount)
                        for file in rev[1]:
                            cmt = cmt + file[0] + ' '
                        cmt = cmt + ') '
                    req.hdf['tickets.%d.comment' % ticket] = wiki_to_oneliner(cmt, self.env, db,  shorten=False)
                    req.hdf['tickets.%d.number' % ticket] = ticket
            if len(revlist):
                revs=revlist.keys()
                revs.sort()
                for rev in revs:
                    arev=revlist[rev]  # (ticket,files,chg)
                    cmt = 'r%d ticket:%d ' % (arev[2].rev,arev[0])
                    filecount = len(arev[1])
                    cmt = cmt + ' - %d files (' % filecount
                    shortfiles=''
                    j = 0
                    for file in arev[1]:
                        req.hdf['revs.%d.files.%d.path' % (rev,j)] = file[0]
                        req.hdf['revs.%d.files.%d.kind' % (rev,j)] = file[1]
                        req.hdf['revs.%d.files.%d.change' % (rev,j)] = file[2]
                        shortpath=file[0][len(old_path)+1:]
                        req.hdf['revs.%d.files.%d.shortpath' % (rev,j)] = shortpath
                        cmt = cmt + file[0] + ' '
                        shortfiles = shortfiles + ' ' + shortpath
                        j=j+1
                    cmt = cmt + ') '
                    req.hdf['revs.%d.comment' % rev] = wiki_to_oneliner(cmt, self.env, db,  shorten=False)
                    req.hdf['revs.%d.number' % rev] = rev
                    req.hdf['revs.%d.shortfiles' % rev] = shortfiles
                    req.hdf['revs.%d.backnumber' % rev] = (rev-1)
                    req.hdf['revs.%d.ticket' % rev] = arev[0]


#        if isinstance(template, basestring):
#            req.hdf['admin.page_template'] = template
#        else:
#            req.hdf['admin.page_content'] = Markup(template.render())

        content_type = "text/html"
        add_stylesheet(req, 'css/icuxtn.css')
        return 'dcut.cs', content_type

