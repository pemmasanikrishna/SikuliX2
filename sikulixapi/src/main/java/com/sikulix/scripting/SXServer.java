/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.scripting;

import com.sikulix.core.SX;
import com.sikulix.core.SXLog;
import fi.iki.elonen.NanoHTTPD;

import java.io.IOException;
import java.util.Map;

public class SXServer extends NanoHTTPD {

  static SXLog log;

  static {
    log = SX.getLogger("SXServer");
    log.isSX();
    log.on(SXLog.INFO);
  }

  static boolean shouldStop = false;
  static SXServer server = null;

  public SXServer() throws IOException {
    this(8080);
  }

  public SXServer(int port) throws IOException {
    super(port);
    start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
    log.trace("started on localhost:%d", port);
  }

  static void createStopListener() {
    Runnable stopListener = new Runnable() {
      public void run() {
        log.trace("createStopListener: stop listener started");
        while (!shouldStop) {
          SX.pause(1);
        }
        if (SX.isNotNull(server)) {
          log.trace("createStopListener: stopping in 3 secs");
          SX.pause(3);
          log.trace("createStopListener: stopping now");
          server.stop();
        }
      }
    };
    new Thread(stopListener).start();
  }

  public static void start(String[] args) {
    createStopListener();
    try {
      server = new SXServer();
    } catch (IOException ioe) {
      log.error("could not start on localhost:8080 (%s)", ioe.getMessage());
      shouldStop = true;
    }
  }

  @Override
  public Response serve(IHTTPSession session) {
    String msg = "<html><body><h1>Hello from server</h1>\n";
    Map<String, String> parms = session.getParms();
    String uri = session.getUri();
    if (uri.startsWith("/stop")) {
      shouldStop = true;
      log.trace("stopping intentionally");
      msg += "server is stopping";
    } else {
      String ip = session.getRemoteIpAddress();
      String host = session.getRemoteHostName();
      String msgResp = String.format("serve: uri(%s)", uri);
      log.trace("%s", msgResp);
      msg += msgResp;
      if (parms.size() > 0) {
        for (String parm : parms.keySet()) {
          String msgParm = String.format("serve: parm: %s = %s", parm, parms.get(parm));
          log.trace("%s", msgParm);
          msg += "<br>" + msgParm;
        }
      }
    }
    return newFixedLengthResponse(msg + "</body></html>\n");
  }
}
