var Region = Java.type("com.sikulix.api.Region");
var SX = Java.type("com.sikulix.core.SX");
var SXLog = Java.type("com.sikulix.core.SXLog");

var log = SX.getLogger("JavaScriptAPI");
log.on(SXLog.TRACE);

function click(arg1, arg2) {
  log.trace("click(%s, %s)", arg1, arg2);
  return Commands.click(arg1, arg2);
}
