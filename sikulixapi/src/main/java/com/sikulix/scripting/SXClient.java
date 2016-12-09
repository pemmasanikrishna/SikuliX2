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
    log.on(SXLog.TRACE);
  }

  static String urlBase = "http://localhost:8080";

  public static HttpResponse<JsonNode> post(String body) {
    return post("noroute", body);
  }

  public static HttpResponse<JsonNode> post(String urlCommand, String body) {
    log.trace("post(): %s body: %s", urlCommand, body);
    HttpResponse<JsonNode> jsonResponse = null;
    try {
      jsonResponse = Unirest.post(urlBase + urlCommand)
              .header("accept", "application/json")
              .header("content-type", "application/json")
              .body(body)
              .asJson();
    } catch (UnirestException e) {
      log.error("post(): %s", e.getMessage());
    }
    log.trace("post(): %s",jsonResponse.getBody().toString());
    return jsonResponse;
  }

  public static HttpResponse<JsonNode> get() {
    return get("/state");
  }

  public static HttpResponse<JsonNode> get(String urlCommand) {
    log.trace("get(): %s", urlCommand);
    HttpResponse<JsonNode> jsonResponse = null;
    try {
      jsonResponse = Unirest.get(urlBase + urlCommand)
              .header("accept", "application/json")
              .asJson();
      log.trace("get(): %s",jsonResponse.getBody().toString());
    } catch (UnirestException e) {
      log.error("get(): %s", e.getMessage());
    }
    return jsonResponse;
  }

  public static boolean stopServer() {
    try {
      log.trace("stopServer: trying");
      String url = urlBase + "/stop";
      GetRequest request = Unirest.get(url);
      try {
        HttpResponse<?> response = request.asString();
//      InputStream respIS = response.getRawBody();
//      String respText = IOUtils.toString(respIS, "UTF-8");
//      log.trace("%s", respText);
      } catch (Exception ex) {
        log.error("%s", ex.getMessage());
      }
    } catch (Exception e) {
      log.error("stopServer: %s", e.getMessage());
      return false;
    }
    return true;
  }
}
