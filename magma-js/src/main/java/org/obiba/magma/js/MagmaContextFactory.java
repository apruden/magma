package org.obiba.magma.js;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.validation.constraints.NotNull;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.GroovyShell;
import jdk.nashorn.api.scripting.AbstractJSObject;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import org.obiba.magma.Initialisable;
import org.obiba.magma.js.methods.GlobalMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates instances of {@code MagmaContext}
 */
public class MagmaContextFactory implements Initialisable {
  private static final Logger log = LoggerFactory.getLogger(MagmaContextFactory.class);

  static ScriptEngine engine;

  static NashornScriptEngineFactory factory;

  static GroovyShell shell;

  @NotNull
  private static Set<GlobalMethodProvider> globalMethodProviders = Collections.emptySet();


  public static GroovyShell getEngine() {
    return shell;
  }

  private final static ThreadLocal<MagmaContext>  magmaContext = new ThreadLocal<MagmaContext>() {
    @Override
    protected MagmaContext initialValue() {
      MagmaContext ctx = new MagmaContext();
      initBinding(ctx);
      return ctx;
    }
  };

  private final static ThreadLocal<Scriptable> scriptableContext = new ThreadLocal<Scriptable>() { };

  public static Scriptable getScriptableContext() {
    return scriptableContext.get();
  }

  private static void setScriptableContext(Scriptable value) {
    scriptableContext.set(value);
  }

  private final static ThreadLocal<MagmaContext> scriptableValueContext = new ThreadLocal<MagmaContext>() {
    @Override
    protected MagmaContext initialValue() {
      final MagmaContext context = new MagmaContext();
      initBinding(context);
      ScriptableValue.getMembers().forEach((k, v) ->
        context.setProperty(k, v)
      );
      return context;
    }
  };

  private final static ThreadLocal<MagmaContext> scriptableVariableContext = new ThreadLocal<MagmaContext>() {
    @Override
    protected MagmaContext initialValue() {
      final MagmaContext context = new MagmaContext();
      initBinding(context);
      ScriptableVariable.getMembers().forEach((k, v) ->
          context.setProperty(k, v)
      );
      return context;
    }
  };

  public static void setGlobalMethodProviders(@NotNull Collection<GlobalMethodProvider> globalMethodProviders) {
    if(globalMethodProviders == null) throw new IllegalArgumentException("globalMethodProviders cannot be null");
    MagmaContextFactory.globalMethodProviders = ImmutableSet.copyOf(globalMethodProviders);
  }

  public static MagmaContext createContext() {
    return magmaContext.get();
  }

  public static MagmaContext createContext(Scriptable value) {
    MagmaContext context;

    if(value instanceof ScriptableValue)
      context = scriptableValueContext.get();
    else if (value instanceof ScriptableVariable)
      context = scriptableVariableContext.get();
    else
      throw new IllegalArgumentException("value");

    setScriptableContext(value);

    return context;
  }

  private static void initBinding(Binding gb) {
    Iterables.concat(Lists.newArrayList(new GlobalMethods()), globalMethodProviders).forEach(p -> {
      Collection<Method> methods = p.getJavaScriptExtensionMethods();

      for(final Method method : methods) {
        gb.setProperty(method.getName(), new Closure(null) {
          public Object doCall(Object... args) {
            try {
              return method.invoke(null, gb, args);
            } catch(IllegalAccessException | InvocationTargetException e) {
              Throwables.propagateIfInstanceOf(e.getCause(), MagmaJsEvaluationRuntimeException.class);
              throw Throwables.propagate(e);
            }
          }
        });
      }
    });
  }

  private static void initBindings(ScriptContext ctx, Bindings gb) {
    Iterables.concat(Lists.newArrayList(new GlobalMethods()), globalMethodProviders).forEach(p -> {
      Collection<Method> methods = p.getJavaScriptExtensionMethods();

      for(final Method method : methods) {
        gb.put(method.getName(), new AbstractJSObject() {
          @Override
          public Object call(Object thiz, Object... args) {
            try {
              return method.invoke(null, ctx, args);
            } catch(IllegalAccessException | InvocationTargetException e) {
              Throwables.propagateIfInstanceOf(e.getCause(), MagmaJsEvaluationRuntimeException.class);
              throw Throwables.propagate(e);
            }
          }

          @Override
          public boolean isFunction() {
            return true;
          }
        });
      }
    });
  }

  @Override
  public void initialise() {
    synchronized(this) {
      factory = new NashornScriptEngineFactory();
      engine = factory.getScriptEngine();
      shell = new GroovyShell();
    }
  }
}
