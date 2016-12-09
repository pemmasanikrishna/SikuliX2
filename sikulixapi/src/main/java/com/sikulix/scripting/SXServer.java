/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.scripting;

import com.sikulix.core.SX;
import com.sikulix.core.SXLog;
import fi.iki.elonen.NanoHTTPD;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class SXServer extends NanoHTTPD {

  static SXLog log;

  static {
    log = SX.getLogger("SXServer");
    log.isSX();
    log.on(SXLog.TRACE);
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
          log.trace("StopListener: stopping in 3 secs");
          SX.pause(3);
          log.trace("StopListener: stopping now");
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
    Response.IStatus status = Response.Status.OK;
    boolean isImplemented = true;
    boolean isError = false;
    String uri = session.getUri();
    Method method = session.getMethod();
    Map<String, String> headers = session.getHeaders();

    String jsonContentTemplate = "[\"%s\", \"%s\", %s]";
    String jsonContent = "";
    String jsonContentMsg = uri;
    String jsonContentLoad = "{}";
    String jsonContentMsgSorry = "error: " + uri + " not implemented";
    String jsonContentMsgError = "error: " + uri + " ";

    String command = "";
    boolean isCommandSession = false;
    String sessionID = "";
    String subCommand = "";
    String subCommandParm = "";
    String[] route = uri.split("/");
    if (route.length > 1) {
      command = route[1].toLowerCase();
      if ("session".equals(command)) {
        isCommandSession = true;
        if (route.length > 2) {
          sessionID = route[2].toLowerCase();
        }
        if (route.length > 3) {
          subCommand = route[3].toLowerCase();
        }
        if (route.length > 4) {
          subCommandParm = route[4].toLowerCase();
        }
      }
      if (Method.GET.equals(method)) {
        if (isCommandSession) {
          if (sessionID.isEmpty()) {
            jsonContentMsgError += "sessionID missing or not valid";
            isError = true;
          } else {

          }
        } else if (command.startsWith("stop")) {
          shouldStop = true;
          log.trace("stopping intentionally");
          jsonContentMsg = "server is stopping";
        } else if (command.startsWith("state")) {
          isImplemented = false;
        } else if (command.startsWith("sessions")) {
          isImplemented = false;
        } else {
          isImplemented = false;
        }
      } else if (Method.POST.equals(method)) {
        if (isCommandSession) {
          final HashMap<String, String> bodyFiles = new HashMap<String, String>();
          if ("application/json".equals(headers.get("content-type"))) {
            try {
              session.parseBody(bodyFiles);
              jsonContentLoad = bodyFiles.get("postData");
            } catch (Exception e) {
              log.error("POST: not working: %s", e.getMessage());
              isImplemented = false;
            }
          }
        } else {
          isImplemented = false;
        }
      }
    }
    if (!isImplemented) {
      jsonContent = String.format(jsonContentTemplate, method, jsonContentMsgSorry, "");
      log.error("serve(): %s", jsonContent);
    } else if (isError) {
      jsonContent = String.format(jsonContentTemplate, method, jsonContentMsgError, "");
      log.error("serve(): %s", jsonContent);
    } else {
      jsonContent = String.format(jsonContentTemplate, method, jsonContentMsg, jsonContentLoad);
    }
    return newFixedLengthResponse(status, "application/json", jsonContent);
  }
}
