package org.obiba.magma.datasource.hibernate.support;

import java.io.File;

import org.obiba.magma.AbstractDatasourceFactory;
import org.obiba.magma.Datasource;
import org.obiba.magma.Disposable;
import org.obiba.magma.Initialisable;
import org.obiba.magma.datasource.hibernate.HibernateDatasource;
import org.obiba.magma.datasource.hibernate.SessionFactoryProvider;
import org.obiba.magma.support.Disposables;
import org.obiba.magma.support.Initialisables;
import org.obiba.magma.support.Placeholders;

public class HibernateDatasourceFactory extends AbstractDatasourceFactory implements Initialisable, Disposable {

  private SessionFactoryProvider sessionFactoryProvider;

  private String binariesDirectory;

  public HibernateDatasourceFactory() {

  }

  public HibernateDatasourceFactory(String name, SessionFactoryProvider sessionFactoryProvider) {
    setName(name);
    this.sessionFactoryProvider = sessionFactoryProvider;
  }

  public Datasource internalCreate() {
    if(binariesDirectory != null) {
      String path = Placeholders.replaceAll(binariesDirectory);
      return new HibernateDatasource(getName(), sessionFactoryProvider.getSessionFactory(), new File(path, getName()));
    } else {
      return new HibernateDatasource(getName(), sessionFactoryProvider.getSessionFactory());
    }
  }

  public void setSessionFactoryProvider(SessionFactoryProvider sessionFactoryProvider) {
    this.sessionFactoryProvider = sessionFactoryProvider;
  }

  @Override
  public void initialise() {
    Initialisables.initialise(sessionFactoryProvider);
  }

  @Override
  public void dispose() {
    Disposables.dispose(sessionFactoryProvider);
  }

  public SessionFactoryProvider getSessionFactoryProvider() {
    return sessionFactoryProvider;
  }

  /**
   * @param dir
   */
  public void setBinariesDirectory(String dir) {
    this.binariesDirectory = dir;
  }
}
