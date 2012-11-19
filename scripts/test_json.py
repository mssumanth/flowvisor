#!/usr/bin/python

import sys

import xmlrpclib

import re

from time import sleep

import json

from jsonrpc import ServiceProxy, JSONParam



class JSONParamEncoder(json.JSONEncoder):

    def default(self, obj):

      if isinstance(obj, JSONParam):

        return obj.getValueAsJson()

      elif isinstance(obj, FlowSpaceChangeRequest):

        return {'entry': obj.getEntry(), 'changeType': obj.getChangeType()}

      elif isinstance(obj, FlowSpaceEntry):

        ruleMatchStr = "OFMatch["

        for rule in obj.rules:

          ruleMatchStr = "%s%s=%s," % (ruleMatchStr, rule.key, rule.val)

        ruleMatchStr = ruleMatchStr[0:len(ruleMatchStr)-1] + "]"

        actionsList = []

        for action in obj.actions:

          actionDict = {}

          actionsList.append(actionDict)

          if action.type == 'Slice':

            actionDict['vendor'] = (1 - pow(2, 31))

          actionDict['type'] = "VENDOR"

          actionDict['length'] = 8

          actionDict['sliceName'] = action.key

          actionDict['slicePerms'] = action.val

          print "hello"

        return {'id':obj.id, 'ruleMatch':ruleMatchStr, 'actionsList':actionsList, 'dpid':obj.dpid, 'priority':obj.priority}

      else:

        return json.JSONEncoder.default(self, obj)



def jconn():

    try:

        jsonconnect = ServiceProxy(jcnt, JSONParamEncoder)

    except:

        print "Connection to Flowvisor using %s failed" % jcnt

        sys.exit()

    return jsonconnect



###

def test_failed(str):

    s = "TEST FAILED!!!: " + str

    print s

    raise Exception(s)



### changeFlowSpace

def changeFS(id, dpid, pri, match, sname):

    if debug == 1:

        print "Change FS[%s] %s %s %s Slice:%s=4" % (id,dpid,pri,match,sname)

    chg={"operation":"CHANGE", "id":id, "dpid":dpid, "priority": pri, "match": match, "actions": "Slice:" + sname + "=4"}

    if rpc == 1:

        try:

            if not s.api.changeFlowSpace([chg]):

                test_failed("illegal flow space")

        except xmlrpclib.Fault:

            print "     RPC failure"

    if debug == 1:

        print "     done"



###

def changeto(fromSN,toSN):

    fs = jconn().listFlowSpace()

    flowspace = json.loads(fs['valueAsJson'])

    for entry in flowspace:

        sname = entry["actionsList"][0]["sliceName"]

        if sname == fromSN:

            id = str(entry["id"])

            priority = str(entry["priority"])

            dpid = entry["dpid"]

            match = entry["ruleMatch"]

            changeFS (id, dpid, priority, match, toSN)



#################################### Start RPC

try:

    user    = "fvadmin"

    passwd  = "0fw0rk"

    userps  = user + ":" + passwd + "@"

    host    = "localhost"

    rpcport = 18080

    jsonport= 18081

    debug = 0

    rpc = 0

    fm = sys.argv[2]

    jcnt = "https://%s:%s@%s:%d/" % (user, passwd, host, int(jsonport))

    xcnt = "https://%s:%s@%s:%d/xmlrpc" % (user, passwd, host, int(rpcport))



    if len(sys.argv) < 3:

        raise Exception("arguments too short")



    if sys.argv[1] == "dry":

        debug = 1

    elif sys.argv[1] == "rpc":

        rpc = 1

    elif sys.argv[1] == "dbg":

        debug = 1

        rpc = 1

    else:

        raise Exception("no command")



    if rpc == 1:

        s = xmlrpclib.ServerProxy(xcnt)

    changeto(fm, "cleaner")

    #changeto(fm + "_r", fm)

    #changeto("cleaner", fm + "_r")



finally:

    print "Configuration finished."


