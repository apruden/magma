package org.obiba.magma.js;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextAction;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.FunctionObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.obiba.magma.Initialisable;
import org.obiba.magma.js.methods.GlobalMethods;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

/**
 * Creates instances of {@code MagmaContext}
 */
public class MagmaContextFactory extends ContextFactory implements Initialisable {

  /**
   * The global scope shared by all evaluated scripts. Should contain top-level functions and prototypes.
   */
  private ScriptableObject sharedScope;

  private ScriptableValuePrototypeFactory factory = new ScriptableValuePrototypeFactory();

  private Set<GlobalMethodProvider> globalMethodProviders = Collections.emptySet();

  @Override
  protected Context makeContext() {
    return new MagmaContext(this);
  }

  public ScriptableObject sharedScope() {
    if(sharedScope == null) {
      throw new MagmaJsRuntimeException("Shared scope not initialised. Make sure the MagmaJsExtension has been added to the MagmaEngine before evaluating scripts.");
    }
    return sharedScope;
  }

  public void setGlobalMethodProviders(Set<GlobalMethodProvider> globalMethodProviders) {
    if(globalMethodProviders == null) throw new IllegalArgumentException("globalMethodProviders cannot be null");
    this.globalMethodProviders = ImmutableSet.copyOf(globalMethodProviders);
  }

  public ScriptableValuePrototypeFactory getScriptableValuePrototypeFactory() {
    return factory;
  }

  public void setScriptableValuePrototypeFactory(ScriptableValuePrototypeFactory factory) {
    if(factory == null) throw new IllegalArgumentException("factory cannot be null");
    this.factory = factory;
  }

  public void initialise() {
    sharedScope = (ScriptableObject) ContextFactory.getGlobal().call(new ContextAction() {
      @Override
      public Object run(Context cx) {
        ScriptableObject sharedScope = cx.initStandardObjects(null, true);

        Iterable<Method> methods = Iterables.filter(Arrays.asList(GlobalMethods.class.getMethods()), new Predicate<Method>() {
          @Override
          public boolean apply(Method input) {
            return GlobalMethods.GLOBAL_METHODS.contains(input.getName());
          }
        });

        for(Method globalMethod : methods) {
          // Rename "valueOf" to "$"
          String name = globalMethod.getName().equals("valueOf") ? "$" : globalMethod.getName();
          FunctionObject fo = new FunctionObject(name, globalMethod, sharedScope);
          sharedScope.defineProperty(name, fo, ScriptableObject.DONTENUM);
        }

        for(GlobalMethodProvider provider : globalMethodProviders) {
          for(Method globalMethod : provider.getJavaScriptExtensionMethods()) {
            String name = globalMethod.getName();
            FunctionObject fo = new FunctionObject(name, globalMethod, sharedScope);
            sharedScope.defineProperty(name, fo, ScriptableObject.DONTENUM);
          }
        }

        Scriptable valuePrototype = factory.buildPrototype();
        ScriptableObject.putProperty(sharedScope, valuePrototype.getClassName(), valuePrototype);
        sharedScope.sealObject();
        return sharedScope;
      }
    });
  }

}
