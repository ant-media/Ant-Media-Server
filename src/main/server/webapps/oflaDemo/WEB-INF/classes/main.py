"""
main.py - a translation into Python of the ofla demo Application class, a Red5 example.

@author The Red5 Project (red5@osflash.org)
@author Joachim Bauch (jojo@struktur.de)
"""

from org.red5.server.adapter import ApplicationAdapter

class Application(ApplicationAdapter):
    
    def appStart(self, app):
        ApplicationAdapter.appStart(self, app)
        print 'Python appStart', app
        self.appScope = app
        return 1

    def appConnect(self, conn, params):
        ApplicationAdapter.appConnect(self, conn, params)
        print 'Python appConnect:', conn, params
        return 1

    def toString(self):
        return 'Python:Application'

def getInstance(*args):
    print 'Arguments:', args
    return Application()
