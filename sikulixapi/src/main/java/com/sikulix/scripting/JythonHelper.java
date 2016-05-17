/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.scripting;

import com.sikulix.core.Content;
import com.sikulix.core.SX;
import com.sikulix.core.SXLog;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class JythonHelper {

  public static SXLog log = SX.getLogger("SX.JythonHelper");

  static JythonHelper jh = null;
  private JythonHelper() {}

  static Object interpreter = null;
  List<String> sysPath = new ArrayList<String>();
  List<String> sysArgv = new ArrayList<String>();
  int nPathAdded = 0;
  int nPathSaved = -1;
  static Class[] nc = new Class[0];
  static Class[] nc1 = new Class[1];
  static Class cInterpreter = null;
  static Class cPyException = null;
  static Class cList = null;
  static Class cPy = null;
  static Class cPyFunction = null;
  static Class cPyMethod = null;
  static Class cPyInstance = null;
  static Class cPyObject = null;
  static Class cPyString = null;
  static Method mLen, mGet, mSet, mAdd, mRemove, mClear;
  static Method mGetSystemState, mExec, mExecfile;
  static Field PI_path;

  static boolean isJythonReady = false;

  public static JythonHelper get() {
    if (jh == null) {
      jh = new JythonHelper();
      log.trace("init: starting");
      try {
        cInterpreter = Class.forName("org.python.util.PythonInterpreter");
      } catch (Exception ex) {
        Content.addClassPath("Jython");
      }
      try {
        cInterpreter = Class.forName("org.python.util.PythonInterpreter");
        mGetSystemState = cInterpreter.getMethod("getSystemState", nc);
        mExec = cInterpreter.getMethod("exec", new Class[]{String.class});
        mExecfile = cInterpreter.getMethod("execfile", new Class[]{String.class});
        Constructor PI_new = cInterpreter.getConstructor(nc);
        interpreter = PI_new.newInstance((Object) null);
        cPyException = Class.forName("org.python.core.PyException");
        cList = Class.forName("org.python.core.PyList");
        cPy = Class.forName("org.python.core.Py");
        cPyFunction = Class.forName("org.python.core.PyFunction");
        cPyMethod = Class.forName("org.python.core.PyMethod");
        cPyInstance = Class.forName("org.python.core.PyInstance");
        cPyObject = Class.forName("org.python.core.PyObject");
        cPyString = Class.forName("org.python.core.PyString");
        mLen = cList.getMethod("__len__", nc);
        mClear = cList.getMethod("clear", nc);
        mGet = cList.getMethod("get", new Class[]{int.class});
        mSet = cList.getMethod("set", new Class[]{int.class, Object.class});
        mAdd = cList.getMethod("add", new Class[]{Object.class});
        mRemove = cList.getMethod("remove", new Class[]{int.class});
      } catch (Exception ex) {
        cInterpreter = null;
      }
      log.trace("init: success");
    }
    if (cInterpreter == null) {
      log.error("no Jython on classpath");
    }
    isJythonReady = true;
    return jh;
  }

  private void noOp() {
  } // for debugging as breakpoint

  class PyException {
    Object inst = null;
    Field fType = null;
    Field fValue = null;
    Field fTrBack = null;

    public PyException(Object i) {
      inst = i;
      cPyException.cast(inst);
      try {
        fType = cPyException.getField("type");
        fValue = cPyException.getField("value");
        fTrBack = cPyException.getField("traceback");
      } catch (Exception ex) {
        noOp();
      }
    }

    public int isTypeExit() {
      try {
        if (fType.get(inst).toString().contains("SystemExit")) {
          return Integer.parseInt(fValue.get(inst).toString());
        }
      } catch (Exception ex) {
        return -999;
      }
      return -1;
    }
  }

  class PyInstance {
    Object inst = null;
    Method mGetAttr = null;
    Method mInvoke = null;

    public PyInstance(Object i) {
      inst = i;
      cPyInstance.cast(inst);
      try {
        mGetAttr = cPyInstance.getMethod("__getattr__", String.class);
        mInvoke = cPyInstance.getMethod("invoke", String.class, cPyObject);
      } catch (Exception ex) {
        noOp();
      }
    }

    public Object get() {
      return inst;
    }

    Object __getattr__(String mName) {
      if (mGetAttr == null) {
        return null;
      }
      Object method = null;
      try {
        method = mGetAttr.invoke(inst, mName);
      } catch (Exception ex) {
      }
      return method;
    }

    public void invoke(String mName, Object arg) {
      if (mInvoke != null) {
        try {
          mInvoke.invoke(inst, mName, arg);
        } catch (Exception ex) {
          noOp();
        }
      }
    }
  }

  class PyFunction {
    public String __name__;
    Object func = null;
    Method mCall = null;
    Method mCall1 = null;

    public PyFunction(Object f) {
      func = f;
      try {
        cPyFunction.cast(func);
        mCall = cPyFunction.getMethod("__call__");
        mCall1 = cPyFunction.getMethod("__call__", cPyObject);
      } catch (Exception ex) {
        func = null;
      }
      if (func == null) {
        try {
          func = f;
          cPyMethod.cast(func);
          mCall = cPyMethod.getMethod("__call__");
          mCall1 = cPyMethod.getMethod("__call__", cPyObject);
        } catch (Exception ex) {
          func = null;
        }
      }
    }

    void __call__(Object arg) {
      if (mCall1 != null) {
        try {
          mCall1.invoke(func, arg);
        } catch (Exception ex) {
        }
      }
    }

    void __call__() {
      if (mCall != null) {
        try {
          mCall.invoke(func);
        } catch (Exception ex) {
        }
      }
    }
  }

  class Py {
    Method mJava2py = null;

    public Py() {
      try {
        mJava2py = cPy.getMethod("java2py", Object.class);
      } catch (Exception ex) {
        noOp();
      }
    }

    Object java2py(Object arg) {
      if (mJava2py == null) {
        return null;
      }
      Object pyObject = null;
      try {
        pyObject = mJava2py.invoke(null, arg);
      } catch (Exception ex) {
        noOp();
      }
      return pyObject;
    }
  }

  class PyString {
    String aString = "";
    Object pyString = null;

    public PyString(String s) {
      aString = s;
      try {
        pyString = cPyString.getConstructor(String.class).newInstance(aString);
      } catch (Exception ex) {
      }
    }

    public Object get() {
      return pyString;
    }
  }

  public boolean exec(String code) {
    try {
      mExec.invoke(interpreter, code);
    } catch (Exception ex) {
      PyException pex = new PyException(ex.getCause());
      if (pex.isTypeExit() < 0) {
        log.error("exec: returns:\n%s", ex.getCause());
      }
      return false;
    }
    return true;
  }

  public int execfile(String fpScript) {
    int retval = -999;
    try {
      mExecfile.invoke(interpreter, fpScript);
    } catch (Exception ex) {
      PyException pex = new PyException(ex.getCause());
      if ((retval = pex.isTypeExit()) < 0) {
        log.error("execFile: returns:\n%s", ex.getCause());
      }
    }
    return retval;
  }

  //TODO check signature (jh method)
  public boolean checkCallback(Object[] args) {
    PyInstance inst = new PyInstance(args[0]);
    String mName = (String) args[1];
    Object method = inst.__getattr__(mName);
    if (method == null || !method.getClass().getName().contains("PyMethod")) {
      log.error("checkCallback: Object: %s, Method not found: %s", inst, mName);
      return false;
    }
    return true;
  }

  public boolean runLoggerCallback(Object[] args) {
    PyInstance inst = new PyInstance(args[0]);
    String mName = (String) args[1];
    String msg = (String) args[2];
    Object method = inst.__getattr__(mName);
    if (method == null || !method.getClass().getName().contains("PyMethod")) {
      log.error("runLoggerCallback: Object: %s, Method not found: %s", inst, mName);
      return false;
    }
    try {
      PyString pmsg = new PyString(msg);
      inst.invoke(mName, pmsg.get());
    } catch (Exception ex) {
      log.error("runLoggerCallback: invoke: %s", ex.getMessage());
      return false;
    }
    return true;
  }

  public boolean runObserveCallback(Object[] args) {
    PyFunction func = new PyFunction(args[0]);
    boolean success = true;
    try {
      func.__call__(new Py().java2py(args[1]));
    } catch (Exception ex) {
//      if (!"<lambda>".equals(func.__name__)) {
      if (!func.toString().contains("<lambda>")) {
        log.error("runObserveCallback: jython invoke: %s", ex.getMessage());
        return false;
      }
      success = false;
    }
    if (success) {
      return true;
    }
    try {
      func.__call__();
    } catch (Exception ex) {
      log.error("runObserveCallback: jython invoke <lambda>: %s", ex.getMessage());
      return false;
    }
    return true;
  }

  //TODO implement generalized callback
  public boolean runCallback(Object[] args) {
    PyInstance inst = (PyInstance) args[0];
    String mName = (String) args[1];
    Object method = inst.__getattr__(mName);
    if (method == null || !method.getClass().getName().contains("PyMethod")) {
      log.error("runCallback: Object: %s, Method not found: %s", inst, mName);
      return false;
    }
    try {
      PyString pmsg = new PyString("not yet supported");
      inst.invoke(mName, pmsg.get());
    } catch (Exception ex) {
      log.error("runCallback: invoke: %s", ex.getMessage());
      return false;
    }
    return true;
  }

  public static JythonHelper set(Object ip) {
    JythonHelper.get();
    interpreter = ip;
    return jh;
  }

  public String load(String fpJarOrFolder) {
//##
//# loads a Sikuli extension (.jar) from
//#  1. user's sikuli data path
//#  2. bundle path
//#
//def load(jar):
//    def _load(abspath):
//        if os.path.exists(abspath):
//            if not abspath in sys.path:
//                sys.path.append(abspath)
//            return True
//        return False
//
//    if JythonHelper.load(jar):
//        return True
//
//    if _load(jar):
//        return True
//    path = getBundlePath()
//    if path:
//        jarInBundle = os.path.join(path, jar)
//        if _load(jarInBundle):
//            return True
//    path = ExtensionManager.getInstance().getLoadPath(jar)
//    if path and _load(path):
//        return True
//    return False
    log.debug("load: to be loaded:\n%s", fpJarOrFolder);
    if (!fpJarOrFolder.endsWith(".jar")) {
      fpJarOrFolder += ".jar";
    }
    String fpBundle = SX.getBundlePath();
    File fJar = new File(Content.normalizeAbsolute(fpJarOrFolder, false));
    if (!fJar.exists()) {
      fJar = new File(fpBundle, fpJarOrFolder);
      fJar = new File(Content.normalizeAbsolute(fJar.getPath(), false));
      if (!fJar.exists()) {
        fJar = new File(SX.getSXEXTENSIONS(), fpJarOrFolder);
        if (!fJar.exists()) {
          fJar = new File(SX.getSXLIB(), fpJarOrFolder);
          if (!fJar.exists()) {
            fJar = null;
          }
        }
      }
    }
    if (fJar != null) {
      if (Content.addClassPath(fJar.getPath())) {
        if (!hasSysPath(fJar.getPath())) {
          insertSysPath(fJar);
        }
      } else {
        log.error("load: not possible");
      }
    } else {
      log.error("load: could not be found - even not in bundle nor in Lib nor in Extensions");
    }
    if (fJar == null) {
      return null;
    }
    return fJar.getAbsolutePath();
  }

  public String findModule(String modName, Object packPath, Object sysPath) {

//  module_name = _stripPackagePrefix(module_name)
//  if module_name[0:1] == "*":
//      return None
//  if package_path:
//      paths = package_path
//  else:
//      paths = sys.path
//  for path in paths:
//      mod = self._find_module(module_name, path)
//      if mod:
//          return mod
//  if Sikuli.load(module_name +".jar"):
//      return None
//  return None

    if (modName.endsWith(".*")) {
      return null;
    }
    int nDot = modName.lastIndexOf(".");
    if (nDot > -1) {
      modName = modName.substring(nDot + 1);
    }
    String fpBundle = SX.getBundlePath();
    File fParentBundle = null;
    File fModule = null;
    if (fpBundle != null) {
      fParentBundle = new File(fpBundle).getParentFile();
      fModule = existsModule(modName, fParentBundle);
    }
    if (fModule == null && packPath != null) {
//      log.log(level, "findModule: packpath not null");
    }
    if (fModule == null) {
      fModule = existsSysPathModule(modName);
      if (fModule == null) {
        return null;
      }
    }
    log.debug("findModule: %s (%s)", fModule.getName(), packPath);
//    if (!fModule.getName().endsWith(".sikuli")) {
//      fModule = fModule.getParentFile();
//    }
    return fModule.getAbsolutePath();
  }

  public String loadModulePrepare(String modName, String modPath) {

//  module_name = _stripPackagePrefix(module_name)
//  ImagePath.add(self.path)
//  Sikuli._addModPath(self.path)
//  return self._load_module(module_name)

    log.debug("loadModulePrepare: %s in %s", modName, modPath);
    int nDot = modName.lastIndexOf(".");
    if (nDot > -1) {
      modName = modName.substring(nDot + 1);
    }
    addSysPath(modPath);
    if (modPath.endsWith(".sikuli")) {
      SX.addImagePath(modPath);
    }
    return modName;
  }

  private File existsModule(String mName, File fFolder) {
    if (mName.endsWith(".sikuli") || mName.endsWith(".py")) {
      return null;
    }
    File fSikuli = new File(fFolder, mName + ".sikuli");
    if (fSikuli.exists()) {
      return fSikuli;
    }
//    File fPython = new File(fFolder, mName + ".py");
//    if (fPython.exists()) {
//      return fPython;
//    }
    return null;
  }

  public void getSysArgv() {
    sysArgv = new ArrayList<String>();
    if (null == cInterpreter) {
      sysArgv = null;
      return;
    }
    try {
      Object aState = mGetSystemState.invoke(interpreter, (Object[]) null);
      Field fArgv = aState.getClass().getField("argv");
      Object pyArgv = fArgv.get(aState);
      Integer argvLen = (Integer) mLen.invoke(pyArgv, (Object[]) null);
      for (int i = 0; i < argvLen; i++) {
        String entry = (String) mGet.invoke(pyArgv, i);
        log.trace("sys.path[%2d] = %s", i, entry);
        sysArgv.add(entry);
      }
    } catch (Exception ex) {
      sysArgv = null;
    }
  }

  public void setSysArgv(String[] args) {
    if (null == cInterpreter || null == sysArgv) {
      return;
    }
    try {
      Object aState = mGetSystemState.invoke(interpreter, (Object[]) null);
      Field fArgv = aState.getClass().getField("argv");
      Object pyArgv = fArgv.get(aState);
      mClear.invoke(pyArgv, (Object) null);
      for (String arg : args) {
        mAdd.invoke(pyArgv, arg);
      }
    } catch (Exception ex) {
      sysArgv = null;
    }
  }

  public void getSysPath() {
    sysPath = new ArrayList<String>();
    if (null == cInterpreter) {
      sysPath = null;
      return;
    }
    try {
      Object aState = mGetSystemState.invoke(interpreter, (Object[]) null);
      Field fPath = aState.getClass().getField("path");
      Object pyPath = fPath.get(aState);
      Integer pathLen = (Integer) mLen.invoke(pyPath, (Object[]) null);
      for (int i = 0; i < pathLen; i++) {
        String entry = (String) mGet.invoke(pyPath, i);
        log.trace("sys.path[%2d] = %s", i, entry);
        sysPath.add(entry);
      }
    } catch (Exception ex) {
      sysPath = null;
    }
  }

  public void setSysPath() {
    if (null == cInterpreter || null == sysPath) {
      return;
    }
    try {
      Object aState = mGetSystemState.invoke(interpreter, (Object[]) null);
      Field fPath = aState.getClass().getField("path");
      Object pyPath = fPath.get(aState);
      Integer pathLen = (Integer) mLen.invoke(pyPath, (Object[]) null);
      for (int i = 0; i < pathLen && i < sysPath.size(); i++) {
        String entry = sysPath.get(i);
        log.trace("sys.path.set[%2d] = %s", i, entry);
        mSet.invoke(pyPath, i, entry);
      }
      if (pathLen < sysPath.size()) {
        for (int i = pathLen; i < sysPath.size(); i++) {
          String entry = sysPath.get(i);
          log.trace("sys.path.add[%2d] = %s", i, entry);
          mAdd.invoke(pyPath, entry);
        }
      }
      if (pathLen > sysPath.size()) {
        for (int i = sysPath.size(); i < pathLen; i++) {
          String entry = (String) mGet.invoke(pyPath, i);
          log.trace("sys.path.rem[%2d] = %s", i, entry);
          mRemove.invoke(pyPath, i);
        }
      }
    } catch (Exception ex) {
      sysPath = null;
    }
  }

  public void addSitePackages() {
    File fLibFolder = SX.getFile(SX.getSXLIB());
    File fSitePackages = new File(fLibFolder, "site-packages");
    if (fSitePackages.exists()) {
      addSysPath(fSitePackages);
      if (hasSysPath(fSitePackages.getAbsolutePath())) {
        log.debug("added as Jython::sys.path[0]:\n%s", fSitePackages);
      }
      File fSites = new File(fSitePackages, "sites.txt");
      String sSites = "";
      if (fSites.exists()) {
        sSites = Content.readFileToString(fSites);
        if (!sSites.isEmpty()) {
          String[] listSites = sSites.split("\n");
          for (String site : listSites) {
            String path = site.trim();
            if (!path.isEmpty()) {
              appendSysPath(path);
            }
          }
        }
      }
    }
  }

  public void addSysPath(String fpFolder) {
    if (!hasSysPath(fpFolder)) {
      sysPath.add(0, fpFolder);
      setSysPath();
      nPathAdded++;
    }
  }

  public void appendSysPath(String fpFolder) {
    if (!hasSysPath(fpFolder)) {
      sysPath.add(fpFolder);
      setSysPath();
      nPathAdded++;
    }
  }

  public void putSysPath(String fpFolder, int n) {
    if (n < 1 || n > sysPath.size()) {
      addSysPath(fpFolder);
    } else {
      sysPath.add(n, fpFolder);
      setSysPath();
      nPathAdded++;
    }
  }

  public void addSysPath(File fFolder) {
    addSysPath(fFolder.getAbsolutePath());
  }

  public void insertSysPath(File fFolder) {
    getSysPath();
    sysPath.add((nPathSaved > -1 ? nPathSaved : 0), fFolder.getAbsolutePath());
    setSysPath();
    nPathSaved = -1;
  }

  public void removeSysPath(File fFolder) {
    int n;
    if (-1 < (n = getSysPathEntry(fFolder))) {
      sysPath.remove(n);
      nPathSaved = n;
      setSysPath();
      nPathAdded = nPathAdded == 0 ? 0 : nPathAdded--;
    }
  }

  public boolean hasSysPath(String fpFolder) {
    getSysPath();
    for (String fpPath : sysPath) {
      if (Content.pathEquals(fpPath, fpFolder)) {
        return true;
      }
    }
    return false;
  }

  public int getSysPathEntry(File fFolder) {
    getSysPath();
    int n = 0;
    for (String fpPath : sysPath) {
      if (Content.pathEquals(fpPath, fFolder.getAbsolutePath())) {
        return n;
      }
      n++;
    }
    return -1;
  }

  public File existsSysPathModule(String modname) {
    getSysPath();
    File fModule = null;
    for (String fpPath : sysPath) {
      fModule = existsModule(modname, new File(fpPath));
      if (null != fModule) {
        break;
      }
    }
    return fModule;
  }

  public File existsSysPathJar(String fpJar) {
    getSysPath();
    File fJar = null;
    for (String fpPath : sysPath) {
      fJar = new File(fpPath, fpJar);
      if (fJar.exists()) {
        break;
      }
      fJar = null;
    }
    return fJar;
  }

  public void showSysPath() {
      getSysPath();
      log.debug("***** Jython sys.path");
      for (int i = 0; i < sysPath.size(); i++) {
        log.info("%2d: %s", i, sysPath.get(i));
      }
      log.debug("***** Jython sys.path end");
  }

  private static class CompileJythonFilter implements Content.FileFilter {

    JythonHelper jython = null;

    public CompileJythonFilter(JythonHelper jython) {
      this.jython = jython;
    }

    @Override
    public boolean accept(File entry) {
      if (jython != null && entry.isDirectory()) {
        jython = doCompileJythonFolder(jython, entry);
      }
      return false;
    }
  }

  /**
   * the foo.py files in the given source folder are compiled to JVM-ByteCode-classfiles foo$py.class
   * and stored in the target folder (thus securing your code against changes).<br>
   * A folder structure is preserved. All files not ending as .py will be copied also.
   * The target folder might then be packed to a jar using buildJarFromFolder.<br>
   * Be aware: you will get no feedback about any compile problems,
   * so make sure your code compiles error free. Currently there is no support for running such a jar,
   * it can only be used with load()/import, but you might provide a simple script that does load()/import
   * and then runs something based on available functions in the jar code.
   *
   * @param fpSource absolute path to a folder/folder-tree containing the stuff to be copied/compiled
   * @param fpTarget the folder that will contain the copied/compiled stuff (folder is first deleted)
   * @return false if anything goes wrong, true means should have worked
   */
  public static boolean compileJythonFolder(String fpSource, String fpTarget) {
    JythonHelper jython = JythonHelper.get();
    if (jython != null) {
      File fTarget = new File(fpTarget);
      Content.deleteFileOrFolder(fTarget);
      fTarget.mkdirs();
      if (!fTarget.exists()) {
        log.error("compileJythonFolder: target folder not available\n%", fTarget);
        return false;
      }
      File fSource = new File(fpSource);
      if (!fSource.exists()) {
        log.error("compileJythonFolder: source folder not available\n", fSource);
        return false;
      }
      if (fTarget.equals(fSource)) {
        log.error("compileJythonFolder: target folder cannot be the same as the source folder");
        return false;
      }
      Content.xcopy(fSource, fTarget);
      if (!jython.exec("import compileall")) {
        return false;
      }
      jython = doCompileJythonFolder(jython, fTarget);
      Content.traverseFolder(fTarget, new CompileJythonFilter(jython));
    }
    return false;
  }

  private static JythonHelper doCompileJythonFolder(JythonHelper jython, File fSource) {
    String fpSource = Content.slashify(fSource.getAbsolutePath(), false);
    if (!jython.exec(String.format("compileall.compile_dir(\"%s\"," + "maxlevels = 0, quiet = 1)", fpSource))) {
      return null;
    }
    for (File aFile : fSource.listFiles()) {
      if (aFile.getName().endsWith(".py")) {
        aFile.delete();
      }
    }
    return jython;
  }


}
