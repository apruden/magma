package org.obiba.meta.js;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextAction;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.obiba.meta.Initialisable;
import org.obiba.meta.Value;
import org.obiba.meta.ValueSet;
import org.obiba.meta.ValueSource;
import org.obiba.meta.ValueType;

import com.google.common.collect.Iterables;

/**
 * 
 * 
 */
public class JavascriptValueSource implements ValueSource, Initialisable {

  private ValueType type;

  private String script;

  private ScriptableObject sharedScope;

  public void setScript(String script) {
    this.script = script;
  }

  public String getScript() {
    return script;
  }

  public void setValueType(ValueType type) {
    this.type = type;
  }

  @Override
  public Value getValue(final ValueSet valueSet) {
    return (Value) ContextFactory.getGlobal().call(new ContextAction() {
      public Object run(Context ctx) {
        Scriptable scope = ctx.newObject(sharedScope);

        scope.setPrototype(sharedScope);
        scope.setParentScope(null);

        enterContext(ctx, sharedScope, valueSet);
        Object value = ctx.evaluateString(sharedScope, getScript(), "source", 1, null);
        return getValueType().valueOf(value);
      }
    });
  }

  @Override
  public ValueType getValueType() {
    return type;
  }

  @Override
  public void initialise() {
    Context ctx = Context.enter();
    try {
      sharedScope = ctx.initStandardObjects();
      // Register engine methods and custom methods
      sharedScope.defineFunctionProperties(Iterables.toArray(DateTimeMethods.exposedMethods, String.class), DateTimeMethods.class, ScriptableObject.DONTENUM);
      EngineMethods.registerMethods(sharedScope);
    } finally {
      Context.exit();
    }
  }

  protected void enterContext(Context ctx, Scriptable scope, ValueSet valueSet) {
    ctx.putThreadLocal(ValueSet.class, valueSet);
  }

  protected void exitContext(Context ctx) {

  }
}
