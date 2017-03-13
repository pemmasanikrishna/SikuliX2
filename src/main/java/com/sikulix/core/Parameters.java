/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.core;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class Parameters {

  private static SXLog log = SX.getLogger("SX.Parameters");

  public Parameters(String theNames, String theClasses) {
    String[] names = theNames.split(",");
    String[] classes = theClasses.split(",");
    if (names.length == classes.length) {
      for (int n = 0; n < names.length; n++) {
        String clazz = classes[n];
        if (clazz.length() == 1) {
          clazz = clazz.toLowerCase();
          if ("s".equals(clazz)) {
            clazz = "String";
          } else if ("i".equals(clazz)) {
            clazz = "Integer";
          } else if ("d".equals(clazz)) {
            clazz = "Double";
          }
        }
        if ("String".equals(clazz) || "Integer".equals(clazz) || "Double".equals(clazz)) {
          parameterTypes.put(names[n], clazz);
        }
      }
      parameterNames = names;
    } else {
      log.error("different length: names: %s classes: %s", theNames, theClasses);
    }
  }

  public boolean isValid() {
    return parameterTypes.size() > 0;
  }

  private Map<String, String> parameterTypes = new HashMap<>();
  private String[] parameterNames = null;

  private Map<String, Object> parameters = new HashMap<>();

  public Map<String, Object> asParameters(Object... args) {
    Map<String, Object> params = new HashMap<>();
    if (args.length > 0) {
      for (int n = 0; n < args.length; n++) {
        if (isParameter(args[n])) {
          String parameterName = (String) args[n];
          Object value = null;
          if (n + 1 < args.length) {
            if (!isParameter(args[n + 1])) {
              value = getParameter(args[n + 1], parameterName);
              n++;
            }
            params.put(parameterName, value);
          }
        }
      }
    }
    return params;
  }

  public void initParameters(Object... args) {
    int argsLength = args.length;
    if (argsLength > 1) {
      if (args[1] instanceof Map) {
        try {
          setParameters(args[0], (Map<String, Object>) args[1]);
        } catch (Exception ex) {
          log.error("start(): invalid parameter list");
        }
      } else {
        guessParameters(args[0], (Object[]) args[1]);
      }
    }
  }

  private boolean isParameter(Object parameter) {
    if (parameter instanceof String) {
      if (SX.isNotNull(parameterTypes.get((String) parameter))) {
        return true;
      }
    }
    return false;
  }

  private Object getParameter(Object possibleValue, String parameterName) {
    String clazz = parameterTypes.get(parameterName);
    Object value = null;
    if ("String".equals(clazz)) {
      if (possibleValue instanceof String) {
        value = possibleValue;
      }
    } else if ("Integer".equals(clazz)) {
      if (possibleValue instanceof Integer) {
        value = possibleValue;
      }
    } else if ("Double".equals(clazz)) {
      if (possibleValue instanceof Double) {
        value = possibleValue;
      }
    }
    return value;
  }

  public void setParameters(Object instance, Map<String, Object> parameters) {
    for (String parameter : parameters.keySet()) {
      setParameter(instance, parameter, parameters.get(parameter));
    }
  }

  private void setParameter(Object instance, String parameter, Object value) {
    String methodName = String.format("set%s%s",
            parameter.substring(0, 1).toUpperCase(),
            parameter.substring(1, parameter.length()));
    try {
      Method method = instance.getClass().getMethod(methodName, new Class[]{Object.class});
      method.invoke(instance, value);
    } catch (Exception e) {
      log.error("setParameter(): did not work: %s (%s = %s)", e.getMessage(), parameter, value);
    }
  }

  public void guessParameters(Object instance, Object[] args) {
    if (args.length > 0 && SX.isNotNull(parameterNames)) {
      int n = 0;
      for (String parameterName : parameterNames) {
        setParameter(instance, parameterName, getParameter(args[n], parameterName));
        n++;
        if (n >= args.length) {
          break;
        }
      }
    }
  }
}
