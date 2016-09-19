/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.scripting;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;
import org.apache.commons.io.IOUtils;

import com.sikulix.core.SX;
import com.sikulix.core.SXLog;

import java.io.IOException;
import java.io.InputStream;

public class SXClient {

  static SXLog log;
  static {
    log = SX.getLogger("SXClient");
    log.isSX();
    log.on(SXLog.INFO);
  }

  static String urlBase = "http://localhost:8080";

  public static boolean postJSON(String body) {
    HttpResponse<JsonNode> jsonResponse = null;
    try {
      jsonResponse = Unirest.post(urlBase)
              .header("accept", "application/json")
              .header("content-type", "application/json")
              .body(body)
              .asJson();
    } catch (UnirestException e) {
      e.printStackTrace();
    }
    log.trace("jsonResponse.getBody(): %s",jsonResponse.getBody().toString());
    return true;
  }

  public static boolean stopServer() {
    try {
      doStopServer();
    } catch (Exception e) {
      log.error("stopServer: %s", e.getMessage());
      return false;
    }
    return true;
  }

  private static void doStopServer() throws UnirestException, IOException {
    String url = urlBase + "/stop";
    GetRequest request = Unirest.get(url);
    try {
      HttpResponse<?> response = request.asString();
      InputStream respIS = response.getRawBody();
      String respText = IOUtils.toString(respIS, "UTF-8");
      log.trace("%s", respText);
    } catch (Exception ex) {
      log.error("%s", ex.getMessage());
    }
  }
}
