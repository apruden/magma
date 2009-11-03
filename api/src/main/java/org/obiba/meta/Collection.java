package org.obiba.meta;

import java.util.Set;

public interface Collection {

  public String getName();

  public Set<String> getEntityTypes();

  public Set<VariableEntity> getEntities(String entityType);

  public ValueSet loadValueSet(VariableEntity entity);

  public ValueSetExtension<?, ?> getExtension(String name);

  public Set<VariableValueSource> getVariableValueSources(String entityType);

  public VariableValueSource getVariableValueSource(String entityType, String variableName) throws NoSuchVariableException;

  public Set<Variable> getVariables();

}
