var Region = Java.type("com.sikulix.api.Region");
var SX = Java.type("com.sikulix.core.SX");
var SXLog = Java.type("com.sikulix.core.SXLog");

var log = SX.getLogger("JavaScriptAPI");
log.on(SXLog.TRACE);

var refRegion = new Region();

function toJson(obj) {
  return JSON.stringify(obj);
}

function fromJson(str) {
  return JSON.parse(str);
}

function jsonToJava(str) {
  return refRegion.fromJson(str);
}

function use(arg1) {
  log.trace("use(%s)", arg1);
  return Commands.use(arg1);
}

function find(arg1, arg2) {
  log.trace("find(%s, %s)", arg1, arg2);
  return Commands.find(arg1, arg2);
}

function wait(arg1, arg2, arg3) {
  log.trace("wait(%s, %s, %s)", arg1, arg2, arg3);
  return Commands.wait(arg1, arg2, arg3);
}

function exists(arg1, arg2, arg3) {
  log.trace("exists(%s, %s, %s)", arg1, arg2, arg3);
  return Commands.exists(arg1, arg2, arg3);
}

function findAll(arg1, arg2) {
  log.trace("findAll(%s, %s)", arg1, arg2);
  return Commands.findAll(arg1, arg2);
}

function findAllByRow(arg1, arg2) {
  log.trace("findAllByRow(%s, %s)", arg1, arg2);
  return Commands.findAllByRow(arg1, arg2);
}

function getMatchesByRow(arg1) {
  log.trace("getMatchesByRow(%s)", arg1);
  return Commands.getMatchesByRow(arg1);
}

function findAllByColumn(arg1, arg2) {
  log.trace("findAllByColumn(%s, %s)", arg1, arg2);
  return Commands.findAllByColumn(arg1, arg2);
}

function getMatchesByColumn(arg1) {
  log.trace("getMatchesByColumn(%s)", arg1);
  return Commands.getMatchesByColumn(arg1);
}

function waitAll(arg1, arg2, arg3) {
  log.trace("waitAll(%s, %s, %s)", arg1, arg2, arg3);
  return Commands.waitAll(arg1, arg2, arg3);
}

function findAny(arg1, arg2) {
  log.trace("type(%s, %s)", arg1, arg2);
  return Commands.findAny(arg1, arg2);
}

function waitAny(arg1, arg2, arg3) {
  log.trace("waitAny(%s, %s, %s)", arg1, arg2, arg3);
  return Commands.waitAny(arg1, arg2, arg3);
}

function click(arg1, arg2) {
  log.trace("click(%s, %s)", arg1, arg2);
  return Commands.click(arg1, arg2);
}

function doubleClick(arg1, arg2) {
  log.trace("doubleClick(%s, %s)", arg1, arg2);
  return Commands.doubleClick(arg1, arg2);
}

function rightClick(arg1, arg2) {
  log.trace("rightClick(%s, %s)", arg1, arg2);
  return Commands.rightClick(arg1, arg2);
}

function hover(arg1, arg2) {
  log.trace("hover(%s, %s)", arg1, arg2);
  return Commands.hover(arg1, arg2);
}

function drag(arg1, arg2) {
  log.trace("drag(%s, %s)", arg1, arg2);
  return Commands.drag(arg1, arg2);
}

function drop(arg1, arg2) {
  log.trace("drop(%s, %s)", arg1, arg2);
  return Commands.drop(arg1, arg2);
}

function mouseMove(arg1, arg2) {
  log.trace("mouseMove(%s, %s)", arg1, arg2);
  return Commands.mouseMove(arg1, arg2);
}

function dragDrop(arg1, arg2, arg3, arg4) {
  log.trace("dragDrop(%s, %s, %s, %s)", arg1, arg2, arg3, arg4);
  return Commands.dragDrop(arg1, arg2, arg3, arg4);
}

function wheel(arg1, arg2, arg3, arg4) {
  log.trace("wheel(%s, %s, %s, %s)", arg1, arg2, arg3, arg4);
  return Commands.wheel(arg1, arg2, arg3, arg4);
}

function type(arg1, arg2, arg3) {
  log.trace("type(%s, %s, %s)", arg1, arg2, arg3);
  return Commands.type(arg1, arg2, arg3);
}

function write(arg1, arg2, arg3) {
  log.trace("write(%s, %s, %s)", arg1, arg2, arg3);
  return Commands.write(arg1, arg2, arg3);
}

function paste(arg1, arg2, arg3) {
  log.trace("paste(%s, %s, %s)", arg1, arg2, arg3);
  return Commands.paste(arg1, arg2, arg3);
}

