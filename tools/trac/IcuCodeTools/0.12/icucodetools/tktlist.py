# Copyright (C) 2007-2010 IBM and Others. All Rights Reserved

import re

import sys

from trac.core import *
from trac.util import Markup
from trac.web import IRequestHandler
from trac.web.chrome import add_stylesheet, INavigationContributor, \
                            ITemplateProvider
from trac.web.href import Href
#from trac.versioncontrol.web_ui.changeset import IChangesetRangeLink
from trac.wiki import wiki_to_html, wiki_to_oneliner, IWikiSyntaxProvider, \
                      Formatter
from trac.ticket import Ticket



class TicketlistModule(Component):

    implements(IRequestHandler)
#    implements(IRequestHandler,IChangesetRangeLink)

    def revision_range_link(self, req, base, start, end):
        return ('Ticket List', req.href('tktlist',old_path=base,new_path=base,old=start,new=end))

    _request_re = re.compile(r"/tktlist(?:/([^/]+))?(/.*)?$")

    def match_request(self, req):
        match = re.match(self._request_re, req.path_info)
        if match:
            return True

    def process_request(self, req):
        #ok, what are we about.
        db = self.env.get_db_cnx()
        ticketlist = {}  # dict of ticket->???
        revlist = {} # dict of revision->
        repos = self.env.get_repository(req.authname)

        if not req.perm.has_permission('TICKET_MODIFY'):
            return req.redirect(req.href.browser())

        # shortcut - if "revs" is set, just use that
        revs = req.args.get('revs')
        if revs and len(revs)>0:
           content_type = "text/html"
           add_stylesheet(req, 'css/icuxtn.css')
           req.hdf['tix.revs'] = revs
           items = revs.split()
           outstr = '1=0 '
           for item in items:
               rev = int(item) # may fail
               outstr = 'rt.rev=%d'%(rev)
           req.hdf['is_dcut']=1
           req.hdf['tix.sql'] = outstr  
           # test - get relevant revs
   #        print "otime=%s, ntime=%s"%(type(otime),type(ntime))
           #cursor.execute("select distinct t.id,t.summary from ticket as t,revision as r, rev2ticket as rt "
           #               " where t.id = rt.ticket and (%s) order by t.id"%(outstr))
           allsql = "select distinct ticket from rev2ticket as rt where %s order by rt.ticket"%(outstr)
           allsql = "select ticket from rev2ticket where rev=%s order by ticket"
           cursor = db.cursor()
          # cursor.execute("select ticket from rev2ticket where rev=%s order by ticket",("22913",))
           cursor.execute("select rt.rev from rev2ticket as rt where rt.ticket = %d order by rt.rev" % int(6010))
           ticket = 0
           req.hdf['tix.sql'] = allsql  
           req.hdf['tix.sql']="zero"
           for tkt in cursor:
               req.hdf['tix.sql']=tkt
               summ = "";
               #sys.stderr.write(" tkt %s summ %s from (d-d)" % (tkt,summ))
               ticket = ticket + 1
               try:
                   req.hdf['tickets.%d.comment' % ticket] = summ
                   #req.hdf['tickets.%d.commenthtml' % ticket] = wiki_to_oneliner( summ, self.env, db, shorten=True )
               except Exception,e:
                   req.hdf['tix.sql']=e
                   #req.hdf['tickets.%d.commenthtml' % ticket] = ''
                   req.hdf['tickets.%d.comment' % ticket] = ''
               req.hdf['tickets.%d.number' % ticket] = tkt
               #aa = Markup("<a class=\"new ticket\" href=\"%s\" title=\"Ticket x (new)\">#%s</a>"%(req.href.ticket(tkt),tkt))
               #req.hdf['tickets.%d.html' % ticket] = aa
           # set RDF here
           return 'tktrevs.cs', content_type

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
        content_type = "text/html"
        add_stylesheet(req, 'css/icuxtn.css')
        
        
        
        # first, get relevant changes.
        req.hdf['target_path'] = '.';
        # okay. manually tromp through 'em
        oset = repos.get_changeset(old_rev);
        nset = repos.get_changeset(new_rev);
        otime = int(oset.date)
        ntime = int(nset.date)
        
        norm_tr="style='border: 1px dashed green; background-color: #CFC;'"
        closed_tr="style='color: #666;'"
        norev_tr="style='background-color:#FDD; border: 1px solid #F99; font-weight: bold;'"

        req.hdf['sample.norm.tr'] = norm_tr
        req.hdf['sample.closed.tr'] = closed_tr
        req.hdf['sample.norev.tr'] = norev_tr
        
#        print " searching in  (%s-%s)" % (otime, ntime)

        # test - get relevant revs
        cursor = db.cursor()
#        print "otime=%s, ntime=%s"%(type(otime),type(ntime))
        cursor.execute("select distinct t.id,t.summary,t.owner, t.milestone, t.status "
                       " , c.value "                       
                       " from ticket as t,revision as r, rev2ticket as rt "
                       " left join ticket_custom as c "
                       " on  ( c.name = 'revw' AND c.ticket = t.id ) "
#                       " , ticket_custom as c "
                       " where t.id = rt.ticket and rt.rev = r.rev and r.time > %s and r.time <= %s "
                       "and exists ( select nc.rev from node_change as nc where nc.rev=r.rev and nc.path like %s ) "
                       "order by t.id", (str(otime), str(ntime), (old_path + "%")))
        ticket = 0
        for tkt,summ,ownr,milestone,status, revw in cursor:
#            print " tkt %s summ %s from (%d-%d)" % (tkt,summ, otime, ntime)
            ticket = ticket + 1
            try:
                req.hdf['tickets.%d.comment' % ticket] = summ
                req.hdf['tickets.%d.commenthtml' % ticket] = wiki_to_oneliner( summ, self.env, db, shorten=True )
            except Exception,e:
                req.hdf['tickets.%d.commenthtml' % ticket] = ''
                req.hdf['tickets.%d.comment' % ticket] = ''
            req.hdf['tickets.%d.number' % ticket] = tkt
            aa = Markup("<a class=\"new ticket\" href=\"%s\" title=\"Ticket x (new)\">#%s</a>"%(req.href.ticket(tkt),tkt))
            req.hdf['tickets.%d.html' % ticket] = aa
            aa = Markup("<a class=\"new ticket\" href=\"%s\" title=\"Ticket x (new)\">#%s</a>"%(req.href.ticket(tkt),tkt))
            req.hdf['tickets.%d.owner' % ticket] = ownr
            req.hdf['tickets.%d.milestone' % ticket] = wiki_to_oneliner( "milestone:%s"%milestone , self.env, db, shorten=False )
            req.hdf['tickets.%d.reviewer' % ticket] = revw
            req.hdf['tickets.%d.statushtml' % ticket] = wiki_to_oneliner( "#%s"%(tkt), self.env, db, shorten=False )
            req.hdf['tickets.%d.html' % ticket] = aa
            req.hdf['tickets.%d.tr' % ticket] = norm_tr
            if status and status.startswith('closed'):
                req.hdf['tickets.%d.tr' % ticket] = closed_tr
            if ( not revw ) or len(revw)<1:
                req.hdf['tickets.%d.tr' % ticket] = norev_tr
        
        return 'tktlist.cs', content_type

