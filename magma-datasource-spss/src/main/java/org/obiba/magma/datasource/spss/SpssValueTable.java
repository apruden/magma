/*
 * Copyright (c) 2012 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.obiba.magma.datasource.spss;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.obiba.magma.Datasource;
import org.obiba.magma.Disposable;
import org.obiba.magma.NoSuchValueSetException;
import org.obiba.magma.Timestamps;
import org.obiba.magma.Value;
import org.obiba.magma.ValueSet;
import org.obiba.magma.ValueType;
import org.obiba.magma.VariableEntity;
import org.obiba.magma.datasource.spss.support.SpssDatasourceParsingException;
import org.obiba.magma.datasource.spss.support.SpssVariableTypeMapper;
import org.obiba.magma.datasource.spss.support.SpssVariableValueFactory;
import org.obiba.magma.datasource.spss.support.SpssVariableValueSourceFactory;
import org.obiba.magma.support.AbstractValueTable;
import org.obiba.magma.support.DatasourceParsingException;
import org.obiba.magma.support.VariableEntityBean;
import org.obiba.magma.support.VariableEntityProvider;
import org.obiba.magma.type.DateTimeType;
import org.opendatafoundation.data.spss.SPSSFile;
import org.opendatafoundation.data.spss.SPSSVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;

public class SpssValueTable extends AbstractValueTable implements Disposable {

  private final static Logger log = LoggerFactory.getLogger(SpssValueTable.class);

  private final SPSSFile spssFile;

  private final String locale;

  private Map<String, Integer> entityToVariableIndex = new HashMap<>();

  public SpssValueTable(Datasource datasource, String name, String entityType, String locale, SPSSFile spssFile) {
    super(datasource, name);
    this.spssFile = spssFile;
    this.locale = locale;
    setVariableEntityProvider(new SpssVariableEntityProvider(entityType));
  }

  @Override
  public void initialise() {
    initializeVariableSources();
    super.initialise();
  }

  @Override
  public ValueSet getValueSet(VariableEntity entity) throws NoSuchValueSetException {
    return new SpssValueSet(this, entity, spssFile, entityToVariableIndex);
  }

  @NotNull
  @Override
  public Timestamps getTimestamps() {
    return new Timestamps() {
      @NotNull
      @Override
      public Value getLastUpdate() {
        Date lastModified = new Date(spssFile.file.lastModified());
        return DateTimeType.get().valueOf(lastModified);
      }

      @NotNull
      @Override
      public Value getCreated() {
        // Not currently possible to read a file creation timestamp. Coming in JDK 7 NIO.
        return DateTimeType.get().nullValue();
      }
    };
  }

  //
  // Private methods
  //

  private void initializeVariableSources() {
    loadMetadata();
    addVariableValueSources(new SpssVariableValueSourceFactory(spssFile, getEntityType(), locale, entityToVariableIndex));
  }

  private void loadMetadata() {
    if(spssFile.isMetadataLoaded) {
      return;
    }

    try {
      spssFile.loadMetadata();
    } catch(Exception e) {
      String fileName = spssFile.file.getName();
      throw new DatasourceParsingException("Failed load meta data in file " + fileName, e, "SpssFailedToLoadMetadata",
          fileName);
    }
  }

  @Override
  public void dispose() {
    if(spssFile != null) {
      try {
        spssFile.close();
      } catch(IOException e) {
        log.warn("Error occurred while closing SPSS file: {}", e.getMessage());
      }
    }
  }

  //
  // Inner Classes
  //

  private class SpssVariableEntityProvider implements VariableEntityProvider {

    @NotNull
    private final String entityType;

    private Set<VariableEntity> variableEntities;

    private SpssVariableEntityProvider(@Nullable String entityType) {
      this.entityType = entityType == null || entityType.trim().isEmpty() ? "Participant" : entityType.trim();
    }

    @NotNull
    @Override
    public String getEntityType() {
      return entityType;
    }

    @Override
    public boolean isForEntityType(String anEntityType) {
      return getEntityType().equals(anEntityType);
    }

    @NotNull
    @Override
    public Set<VariableEntity> getVariableEntities() {

      if(variableEntities == null) {
        loadData();
        variableEntities = getVariableEntitiesInternal();
      }

      return variableEntities;
    }

    private ImmutableSet<VariableEntity> getVariableEntitiesInternal() {
      Collection<String> entityIdentifiers = new HashSet<>();
      ImmutableSet.Builder<VariableEntity> entitiesBuilder = ImmutableSet.builder();
      SPSSVariable entityVariable = spssFile.getVariable(0);
      int numberOfObservations = entityVariable.getNumberOfObservations();
      ValueType valueType = SpssVariableTypeMapper.map(entityVariable);

      for(int i = 1; i <= numberOfObservations; i++) {
        Value identifierValue = new SpssVariableValueFactory(i, entityVariable, valueType, true).create();

        if(identifierValue.isNull()) {
          throw new SpssDatasourceParsingException("Empty entity identifier found.", "SpssEmptyIdentifier",
              entityVariable.getName(), i).dataInfo(entityVariable.getName(), i);
        }

        String identifier = identifierValue.getValue().toString();

        if(entityIdentifiers.contains(identifier)) {
          String variableName = entityVariable.getName();
          throw new SpssDatasourceParsingException("Duplicated entity identifier '" + identifier + "' found.",
              "SpssDuplicateEntity", identifier, i, variableName).dataInfo(variableName, i);
        }

        entityToVariableIndex.put(identifier, i);
        entitiesBuilder.add(new VariableEntityBean(entityType, identifier));
        entityIdentifiers.add(identifier);
      }

      return entitiesBuilder.build();
    }

    private void loadData() {
      if(spssFile.isDataLoaded) {
        return;
      }

      try {
        if(!spssFile.isMetadataLoaded) {
          loadMetadata();
        }

        spssFile.loadData();
      } catch(Exception e) {
        String fileName = spssFile.file.getName();
        throw new DatasourceParsingException("Failed load data in file " + fileName, e, "SpssFailedToLoadData", fileName);
      }
    }

  }

}