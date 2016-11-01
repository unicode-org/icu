# Copyright (C) 2007-2012 International Business Machines Corporation and Others. All Rights Reserved.

# Review module.
# TODO: refactor ticket manipulation items into ticketmgr.

import re

from trac.core import Component, implements
from trac.core import ComponentManager
from trac.core import TracError
from trac.util import Markup
from trac.web import IRequestHandler
from trac.web.chrome import add_stylesheet, add_script, ITemplateProvider, add_ctxtnav
from trac.versioncontrol import Changeset
from trac.web.api import IRequestFilter
from trac.wiki import wiki_to_html, wiki_to_oneliner, IWikiSyntaxProvider
            
from genshi.builder import tag
#from trac.env import IEnvironmentSetupParticipant
from trac.perm import IPermissionRequestor
from trac.config import ListOption
from icucodetools.ticketmgr import TicketManager
from pkg_resources import resource_filename #@UnresolvedImport

class ReviewModule(Component):

    implements(ITemplateProvider, IRequestFilter, IRequestHandler, IPermissionRequestor)

    # path to match for review
    path_match = re.compile(r'/icureview/([0-9]+)')

    voteable_paths = ListOption('icucodetools', 'paths', '/ticket*',
        doc='List of URL paths to show reviews on. Globs are supported.')

    #  search for earliest match, and how many segments to include following
    #    trunk
    #    branches/maint/maint-4-8
    #    tags/release-2-0
    branchList = [['trunk',0],['branches',2],['tags',1]]

    # IPermissionRequestor methods
    def get_permission_actions(self):
        return ['ICUREVIEW_VIEW']

    # ITemplateProvider methods
    def get_templates_dirs(self):
            try:
        	return [resource_filename(__name__, 'templates')]
            except Exception, e: 
                self.log.warning('Could not get template dir: %s: %s' %
                                 (type(e), e))
		return ""


    def get_htdocs_dirs(self):
        return [('icucodetools', resource_filename(__name__, 'htdocs'))]

    # IRequestFilter methods
    def pre_process_request(self, req, handler):
        if 'ICUREVIEW_VIEW' not in req.perm:
            return handler

        if self.match_ticketpage(req):
            self.render_reviewlink(req)

        return handler

    def post_process_request(self, req, template, data, content_type):
        return (template, data, content_type)

    def render_reviewlink(self, req):
        #add_stylesheet(req, 'icucodetools/css/icuxtn.css')

        els = []

        ticket_mgr = TicketManager(self.compmgr)

        db = self.env.get_db_cnx()
        repos = self.env.get_repository()
        if not repos:
            raise TracError("Could not get repository for %s" % (req.authname))

        revs = ticket_mgr.tkt2revs(self.log, db, repos, req, req.args['ticket'])

        if not revs:
            str = 'No commits.'
            li = tag.li(str)
            els.append(li)
        else:
            str = ' %d commits.' % len(revs)
            href = req.href.review(req.args['ticket'])
            a = tag.a('Review', href=href)
            li = tag.li(a + str)
            els.append(li)            
        
        ul = tag.ul(els, class_='review')
        className = ''
        title = "Reviews"
        add_ctxtnav(req, tag.span(tag.object(ul), id='icureview', title=title, class_=className))


    def match_request(self, req):
        match = re.match('/review(?:/([^/]+))?(?:/([^/]+))?(?:/(.*)$)?', req.path_info)
        if match:
            req.args['ticket'] = match.group(1)
            return True

    def match_ticketpage(self, req):
        match = re.match('/ticket(?:/([^/]+))?(?:/([^/]+))?(?:/(.*)$)?', req.path_info)
        if match:
            req.args['ticket'] = match.group(1)
            return True

    def pathToBranchName(self, path):
        #return '/'.join(path.split('/')[0:2])
        windex = None
        win = None
        for branch in self.branchList:
            if(path == branch[0]):  # catch changes to just 'trunk'
                idx = 0
            else:
                idx = path.find(branch[0]+'/')
            if(idx > -1 and (windex == None or windex > idx)):
                windex = idx
                win = branch
        if windex == None:
            segments = path.split('/')
            return '/'.join(segments[0:2])
        else:
            #print "found %s foll %s @ %d" % (win[0],win[1],windex)
            segments = path[windex:].split('/')
            return path[:windex] + ('/'.join(segments[0:win[1]+1])) # use specified # of following segments

    def changeToRange(self, c_new, change):
        # q: (u'trunk/Locale.java', 'file', 'add', None, u'-1')  from r3
        # q: (u'trunk/util.c',      'file', 'edit', u'trunk/util.c', u'2')  from r4
#        c_path = change[0]
#        c_itemtype = change[1]
        c_type = change[2]
        c_oldpath = change[3]
        c_old = int(change[4] or -1)
        if(c_type in (Changeset.COPY,Changeset.MOVE)):
            return (-1, c_new, c_type, c_old, c_oldpath) # ignore OLD rev for these
        elif(c_type in (Changeset.DELETE)):
            return (c_old, -1, c_type)
        else:
            return (c_old, c_new, c_type)
        
    def describeChange(self, file, change, req, db):
        what = change[2] or 'change'
        where = 'r%d:%d' % (change[0],change[1])
        if(change[0] == -1):
            if(change[1] == -1):
                url = None
                what = "noop"
                where = None
            else:
                #if change[2] == 'add+commits':
                url = req.href.browser(file, rev=change[1]) # 'add'
                where = 'r%d' % change[1]
                what = change[2]
        elif(change[1] == -1):
            url = None # deleted
            what = "deleted"
            where = None
        else:
            url = req.href.changeset(old_path=file, old=change[0], new_path=file, new=change[1])
        if url:
            what = Markup('<a href="%s">%s</a>' % (url,what))
        if where:
            return (what, tag.a(where, href=req.href.search(q=where)))
            #return (what, where)
        else:
            return (what, '')


    def process_request(self, req):
        #ok, what are we about.
        #db = self.env.get_db_cnx()
        #ticketlist = {}  # dict of ticket->???
        #revlist = {} # dict of revision->
        repos = self.env.get_repository()

        new_path = req.args.get('new_path')
        new_rev = req.args.get('new')
        old_path = req.args.get('old_path')
        old_rev = req.args.get('old')

        new_path = repos.normalize_path(new_path)
        new_rev = repos.normalize_rev(new_rev)
        old_path = repos.normalize_path(old_path)
        old_rev = repos.normalize_rev(old_rev)
        

#        if not req.perm.has_permission('TICKET_MODIFY'):
#            return req.redirect(req.href.browser())

        old_rev = int(old_rev)
        new_rev = int(new_rev)
        
        ticket = req.args.get('ticket')
        try:
            ticket = int(ticket)
        except Exception:
            ticket = 0
#        req.hdf['review.ticket'] = ticket
#        req.hdf['review.tickethtml'] = tag.a(ticket, req.href.ticket(ticket))

        data = {}

        data['overall_y'] = 0
        data['ticket_id'] = req.args['ticket']
        data['ticket_href'] = req.href.ticket(req.args['ticket'])

        ticket_mgr = TicketManager(self.compmgr)

        db = self.env.get_db_cnx()
        repos = self.env.get_repository()

        revs = ticket_mgr.tkt2revs(self.log, db, repos, req, req.args['ticket'])
        
        if (not revs or len(revs)==0):
            # nothing to review. shouldn't happen
            return ('nothing.html', data, 'text/html')
        elif(len(revs)==1):
            # only one change - just do a changeset view
            return req.redirect(req.href.changeset(revs[0]))

        revcount = 0
        branches = {}
        files = {}
        # may be 0 revs.
        revisions = []
        
        for rev in revs:
            chgset = repos.get_changeset(rev)
            # q: (u'trunk/Locale.java', 'file', 'add', None, u'-1')  from r3
            # q: (u'trunk/util.c', 'file', 'edit', u'trunk/util.c', u'2')  from r4
            message = chgset.message or '--'
            revcount = revcount + 1
            revision = {}
            revision['rev'] =  tag.a(rev, req.href.changeset(rev))
            revision['author'] = chgset.author
            revision['num'] =  rev
            revision['comment'] =  message #wiki_to_oneliner( message, self.env, db, shorten=False )
            rbranches = revision['branches'] = []
            for chg in chgset.get_changes():
                path = chg[0]
                if path in files:
                    item = files[path]
                else:
                    item = []
                    files[path] = item;
                item.append(self.changeToRange(rev,chg))
                branch_name = self.pathToBranchName(path)
                if branch_name not in rbranches:
                    rbranches.append(branch_name)
            revisions.append(revision)
        data['revisions'] = revisions
        
        if(revcount > 0):
            data['revcount'] = revcount
        
        # print "files: %d" % len(files)
        # go throuhg each file and calculate its minimum range
        filelist = files.keys()
        filelist.sort()
#        print 'bar to %d len of %s' % (len(filelist),str(filelist))
        for file in filelist:
            changes = files[file]
            i = 0
#            print " looping from %d to %d over %d " % (i,len(changes)-1,len(changes))
            while len(changes)>1 and i<(len(changes)-1):
                if changes[i][1] == changes[i+1][0]:
                    if changes[i][0] == -1:
                        changes[i+1] = (changes[i][0],changes[i+1][1],'add+commits') # retain 'first' rev
                    else:
                        changes[i+1] = (changes[i][0],changes[i+1][1],'multiple commits') # retain 'first' rev

                    changes = changes[:i] + changes[i+1:] # and shift down
#                    print "merged: %s" % str(changes)
                    files[file] = changes
                else:
                    i = i + 1
        
        # now, write 'em out
        sera = 0
        #files_data = []
        for file in filelist:
            sera = sera+1
            file_data = {}
            file_data['name'] = Markup('<a href="%s">%s</a>' % (req.href.browser(file),file))
            branch_name = self.pathToBranchName(file)
            #print "branch is: (%s)" % (branch_name)
            branches_data = branches.get(branch_name, {})
            files_data = branches_data.get('files',[])

            changes = files[file]
            cha = 0
            changes_data = []
            for change in changes:
                cha = cha + 1
#                print "%s output %s " % (file, str(change))
                changes_data.append(self.describeChange(file, change, req, db))
            file_data['changes'] = changes_data
            if(len(changes)>1):
                whathtml = self.describeChange(file, (changes[0][0], changes[len(changes)-1][1], 'overall'), req, db)
                file_data['overall'] = whathtml
                file_data['overall_y'] = 1
                data['overall_y'] = 1
            else:
                file_data['overall_y'] = 0
            files_data.append(file_data)
            # sets
            branches_data['files'] = files_data
            branches_data['len'] = len(files_data)
            branches_data['name'] = branch_name
            branches[branch_name] = branches_data

        # .. convert dict to array.
        branch_list = []
        for branch in branches:
            branch_list.append(branches[branch])
        data['branches'] = branch_list
        data['lastbranch'] = branch
        data['branchcount'] = len(branches)
        
        content_type = "text/html"
        add_stylesheet(req, 'icucodetools/css/icuxtn.css')
        add_script(req, 'icucodetools/js/review.js')
        return 'review.html', data, content_type

