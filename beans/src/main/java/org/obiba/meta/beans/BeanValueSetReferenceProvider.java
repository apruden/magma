package org.obiba.meta.beans;

import java.util.Set;

import org.obiba.meta.Collection;
import org.obiba.meta.NoSuchValueSetException;
import org.obiba.meta.ValueSet;
import org.obiba.meta.VariableEntity;
import org.obiba.meta.support.AbstractValueSetReferenceProvider;
import org.obiba.meta.support.VariableEntityBean;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;

import com.google.common.collect.ImmutableSet;

public abstract class BeanValueSetReferenceProvider<T> extends AbstractValueSetReferenceProvider {

  private String entityIdentifierPropertyPath;

  public BeanValueSetReferenceProvider(String entityType, String entityIdentifierPropertyPath) {
    super(entityType);
    this.entityIdentifierPropertyPath = entityIdentifierPropertyPath;
  }

  @Override
  public Set<VariableEntity> getVariableEntities() {
    ImmutableSet.Builder<VariableEntity> builder = new ImmutableSet.Builder<VariableEntity>();
    for(Object bean : loadBeans()) {
      BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(bean);

      Object entityId = bw.getPropertyValue(entityIdentifierPropertyPath);
      if(entityId == null) {
        throw new NullPointerException("entity identifier cannot be null");
      }
      builder.add(new VariableEntityBean(getEntityType(), entityId.toString()));
    }
    return builder.build();
  }

  @Override
  public ValueSet loadValueSet(Collection collection, VariableEntity entity) {
    T bean = loadBean(entity);
    if(bean == null) {
      throw new NoSuchValueSetException(entity);
    }
    return buildValueSet(collection, entity, bean);
  }

  protected BeanValueSet<T> buildValueSet(Collection collection, VariableEntity entity, T bean) {
    return new BeanValueSet<T>(collection, entity, bean);
  }

  protected abstract T loadBean(VariableEntity entity);

  protected abstract Iterable<T> loadBeans();
}
