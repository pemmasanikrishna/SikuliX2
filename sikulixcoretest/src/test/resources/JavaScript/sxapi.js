var Region = Java.type("com.sikulix.api.Region");
var SX = Java.type("com.sikulix.core.SX");
var SXLog = Java.type("com.sikulix.core.SXLog");

var log = SX.getLogger("JavaScriptAPI");
log.on(SXLog.TRACE);

function click(visual1, visual2) {
  log.trace("click(%s, %s)", visual1, visual2);
  return Commands.click(visual1, visual2);
}
