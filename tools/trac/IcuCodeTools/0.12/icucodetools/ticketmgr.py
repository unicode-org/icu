# -*- coding: utf-8 -*-
#
# Copyright (C) 2007-2012 IBM and Others. All Rights Reserved
# Author: <srl@icu-project.org>
#

# 
# Ticket management.
# This component manages the revision to ticket map.
#
# 2011-jan-27	srl	adding IRepositoryChangeListener functionality (requires trac 0.12)



from trac.core import Component, implements, TracError
from trac.env import IEnvironmentSetupParticipant
from trac.db import Table, Column, Index, DatabaseManager
from trac.config import Option
from trac.util.text import exception_to_unicode
from trac.versioncontrol.api import IRepositoryChangeListener

import re

tktmgr_schema = [
    Table('rev2ticket', key='rev')[         # map rev->ticket
        Column('rev', type='int'),          # changeset id
        Column('ticket', type='int'),       # ticket #
        Index(['ticket'])],                 # index by ticket
]

class TicketManager(Component):
    implements(IEnvironmentSetupParticipant, IRepositoryChangeListener)

    ticket_pattern = Option('icucodetools', 'ticket_pattern', '^ticket:(\d+)', 
                            """A regex matching the commit messages. Group 1 must return a number.""")
    
    def icu_tktmgr(self):
        return 1;
    
    known_youngest = -1

    def environment_created(self):
        db = self.env.get_db_cnx()
        connector, _ = DatabaseManager(self.env)._get_connector()
        cursor = db.cursor()
        for table in tktmgr_schema:
            for stmt in connector.to_sql(table):
                cursor.execute(stmt)

        cursor.execute("INSERT INTO system (name,value) "
                       "VALUES ('icu_tktmgr',%s)", (self.icu_tktmgr(),))
        db.commit()
        self.log.info('Database update: icu_tktmgr tables version %d ',
                          self.icu_tktmgr())
        print 'icucodetools.ticketmgr: Note, first review will take a while.\n'
    
    def youngest_rev(self,db):
        if (self.known_youngest < 0):
            #print('Did not know youngest value.')
            cursor = db.cursor()
            cursor.execute("SELECT value FROM system WHERE name='icu_tktmgr_youngest'")
            row = cursor.fetchone()
            if not row:
                cursor.execute("INSERT INTO system (name,value) "
                               "VALUES ('icu_tktmgr_youngest','-1')")
                db.commit()
                self.known_youngest = -2
                return -1
            else:
                known_youngest = int(row[0])
                self.known_youngest = known_youngest
        return self.known_youngest
        
    def check_sync(self, log, db, repos):
        ourYoungest = self.youngest_rev(db)
        theirYoungest = repos.get_youngest_rev()
        #log.info("TKT: check_sync %d/%d" % (ourYoungest,theirYoungest))
        if(ourYoungest <= theirYoungest):
            self.resync(log, db, repos, ourYoungest, theirYoungest)
    
    def environment_needs_upgrade(self, db):
        cursor = db.cursor()
        cursor.execute("SELECT value FROM system WHERE name='icu_tktmgr'")
        row = cursor.fetchone()
        if not row or int(row[0]) < self.icu_tktmgr():
            return True

    def upgrade_environment(self, db):
        cursor = db.cursor()
        cursor.execute("SELECT value FROM system WHERE name='icu_tktmgr'")
        row = cursor.fetchone()
        if not row:
            self.environment_created()
        else:
            self.log.info('Do not know how to upgrade icutraxctn_ticketmgr tables to %d',
                          self.icu_tktmgr())
	cursor.close()

    def resync(self, log, db, repos, ourYoungest, theirYoungest):
        self.log.info('resync: ourYoungest=%d theirYoungest=%d' % (ourYoungest, theirYoungest))
        if (ourYoungest < 0):
            # start at rev 1
            ourYoungest = 1
        
        #self.ticket_pattern = self.env.config.get('icucodetools', 'ticket_pattern', '^cldrbug (\d+):')

        #log.info("Pat: %s" % (self.ticket_pattern))
        try:
            self.ticket_match = re.compile(self.ticket_pattern)
        except Exception, e:
            found = self.env.config.get('icucodetools', 'ticket_pattern', 'NoneFound')
            raise TracError('Could not compile icucodetools.ticket_pattern=/%s/ but /%s/: %s' % (self.ticket_pattern, found, exception_to_unicode(e, traceback=True)))

#        self.ticket_match = re.compile(self.ticket_pattern.get())
#        self.ticket_match = re.compile('.*')
        for i in range(ourYoungest, theirYoungest+1):
            #log.warning('syncing: %d [%d/%d+1]', i, theirYoungest)
            cset = repos.get_changeset(i)
            self.revision_changed(log, cset, i, db.cursor())
        db.commit()
        cursor = db.cursor();
        cursor.execute("update system set value='%s' where name='icu_tktmgr_youngest'" % (theirYoungest))
        db.commit()
        #log.warn("self.known_youngest was %d [%d/%d]" % (self.known_youngest,ourYoungest,theirYoungest)) 
        # update known youngest.
        self.known_youngest = theirYoungest
        #log.warn("self.known_youngest now %d [%d/%d]" % (self.known_youngest,ourYoungest,theirYoungest)) 
        return

    # IRepositoryChangeListener methods
    # Must call with: trac-admin /home/icutrac changeset modified '(default)' 29330 29333  - ugly, http://www.mail-archive.com/trac-dev@googlegroups.com/msg04568.html
    def changeset_modified(self, repos, changeset, old_changeset):
        try:
            self.ticket_match = re.compile(self.ticket_pattern)
        except Exception, e:
            found = self.env.config.get('icucodetools', 'ticket_pattern', 'NoneFound')
            raise TracError('Could not compile icucodetools.ticket_pattern=/%s/ but /%s/: %s' % (self.ticket_pattern, found, exception_to_unicode(e, traceback=True)))
        db = self.env.get_db_cnx()
        cursor = db.cursor()
        cursor.execute("DELETE FROM rev2ticket "
                               "  "
                               "where rev= %s " %
                               str(changeset.rev))
        self.revision_changed(self.log, changeset, changeset.rev, db.cursor())
	db.commit()
	#self.log.error("changeset_added: %s\n" % changeset.rev)

    def changeset_added(self, repos, changeset):
        message = changeset.message or '--'
	#self.log.error("changeset_added: %s\n" % changeset.rev)

    # IRepositoryObserver function
    def revision_changed(self, log, cset, next_youngest, cursor):
        # sync the 'rev2ticket' table
        message = cset.message or '--'
        # can we load a ticket from it?   "ticket:1234: Message"
        res = self.ticket_match.match(message.strip())
        if res:
            tickname = res.group(1)
            try:
                int(res.group(1)) # should be int
            except Exception, e:
                self.log.warning('Revision [%s] had unparseable ticket number [%s]: [%s]' %
                                 (next_youngest, tickname, e))
                return
            try:
		#log.warning('r%s=#%s' % (str(next_youngest), tickname))
                cursor.execute("INSERT OR IGNORE INTO rev2ticket "
                               " (rev,ticket) "
                               "VALUES (%s,%s) ",
                               (str(next_youngest), tickname))
            except Exception, e: # *another* 1.1. resync attempt won 
                log.warning('rev2ticket %s could not cache: %s' %
                                 (next_youngest, e))
        else:
            log.warning('Revision %s had unmatched message "%s" for pattern /%s/' %
                        (next_youngest, cset.message, self.ticket_pattern))
    
    def repository_resync(self, cursor):
        cursor.execute("DELETE FROM rev2ticket");
        
    def tkt2revs(self, log, db, repos, req, ticket):
        """Given a ticket, return a list of revs.
        """
        
        self.check_sync(log, db, repos)
        cursor = db.cursor()
        cursor.execute("select rt.rev from rev2ticket as rt where rt.ticket = %d order by rt.rev" % int(ticket))
        revs = []
        for rev, in cursor:
            rev = int(rev)
            revs.append(rev)
	cursor.close()
        return revs
