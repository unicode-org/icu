#!/usr/bin/python

# Copyright (C) 2014 IBM Corporation and Others. All Rights Reserved.
#

# This script should be invoked from the subversion pre-commit hook just like the trac plugin
#
#  REPOS="$1"
#  TXN="$2"
#  TRAC_ENV="/somewhere/trac/project/"
#  LOG=`/usr/bin/svnlook log -t "$TXN" "$REPOS"`
#  /path/to/traccheck "$TRAC_ENV" "$LOG" >&2 || exit 1

import sys, re
from trac.core import TracError
from trac.env import open_environment
from trac.resource import ResourceNotFound
from trac.ticket.model import Ticket
from trac.util.text import exception_to_unicode

okstatus = ['new','accepted','reviewing']

def run(args=None):
    """trac check script"""
    if args is None:
        args = sys.argv[1:]
    env = open_environment(args[0])

    ticket_pattern = env.config.get('icucodetools', 'ticket_pattern', 'NoneFound')
    ticket_match = None

    def lusage():
        print "Please make your message match /%s/\n and use an open ticket (One of these: %s)" % (ticket_pattern, str(okstatus))
        print "See %s/wiki/TracCheck for more details."  % env.base_url

    try:
        ticket_match = re.compile(ticket_pattern)
    except Exception, e:
        # not sorry?
        raise TracError('*** INTERNAL ERROR: Could not compile icucodetools.ticket_pattern=/%s/: %s' % (ticket_pattern, exception_to_unicode(e, traceback=True)))
    res = ticket_match.match(args[1].strip())
    if res:
        tickname = res.group(1)
        try:
            int(res.group(1)) # should be int
        except Exception, e:
            print('*** Sorry, "%s" is not a valid number when parsing "%s": %s.' %
                             (tickname, args[1], e))
            lusage()
            sys.exit(1)
    else:
        print('*** Sorry, could not parse a ticket number from your commit message "%s".' %
              (args[1]))
        lusage()
        sys.exit(1)
    id = int(res.group(1))
    try:
        ticket = Ticket(env, id)
        status = ticket.values['status']
        if status in okstatus:
            # print "Okay! You are committing against ticket #%d which is in state '%s': %s" % (id,status,ticket.values['summary']) # (fails with codec error- and, unneeded. )
            sys.exit(0)
        else:
            print "*** Sorry, ticket #%d is '%s' and is not open for commits: %s" % (id,status,ticket.values['summary'])
            lusage()
            sys.exit(1)
    except (ResourceNotFound):
        print "*** Sorry, ticket #%d does not exist." % (id)
        lusage()
        sys.exit(1)
    sys.exit(0)

# make this file runnable
if __name__ == '__main__':
    sys.exit(run())
