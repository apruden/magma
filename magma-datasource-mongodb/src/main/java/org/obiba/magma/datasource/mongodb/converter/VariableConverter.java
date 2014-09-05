/*
 * Copyright (c) 2013 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.magma.datasource.mongodb.converter;

import java.util.Collection;

import org.bson.BSONObject;
import org.obiba.magma.Attribute;
import org.obiba.magma.Category;
import org.obiba.magma.ValueType;
import org.obiba.magma.Variable;
import org.obiba.magma.datasource.mongodb.MongoDBVariable;

import com.google.common.collect.ImmutableList;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;

public class VariableConverter extends AttributeAwareConverter {

  private VariableConverter() {}

  public static MongoDBVariable unmarshall(BSONObject object) {
    ValueType valueType = ValueType.Factory.forName(getFieldAsString(object, "valueType"));
    Variable.Builder builder = Variable.Builder.newVariable(getFieldAsString(object, "name"), valueType,
        getFieldAsString(object, "entityType")) //
        .repeatable(getFieldAsBoolean(object, "repeatable")) //
        .mimeType(getFieldAsString(object, "mimeType")) //
        .referencedEntityType(getFieldAsString(object, "referencedEntityType")) //
        .occurrenceGroup(getFieldAsString(object, "occurrenceGroup")) //
        .unit(getFieldAsString(object, "unit")).index(getFieldAsInteger(object, "index"));

    if(object.containsField("categories")) {
      builder.addCategories(unmarshallCategories((Iterable<?>) object.get("categories")));
    }

    if(object.containsField("attributes")) {
      builder.addAttributes(unmarshallAttributes((Iterable<?>) object.get("attributes")));
    }

    return new MongoDBVariable(builder.build(), object.get("_id").toString());
  }

  private static Iterable<Category> unmarshallCategories(Iterable<?> cats) {
    ImmutableList.Builder<Category> list = ImmutableList.builder();
    for(Object o : cats) {
      BSONObject cat = (BSONObject) o;
      Category.Builder catBuilder = Category.Builder.newCategory(cat.get("name").toString())
          .missing(Boolean.parseBoolean(cat.get("missing").toString()));
      if(cat.containsField("attributes")) {
        catBuilder.addAttributes(unmarshallAttributes((Iterable<?>) cat.get("attributes")));
      }
      list.add(catBuilder.build());
    }
    return list.build();
  }

  public static DBObject marshall(Variable variable) {
    BasicDBObjectBuilder builder = BasicDBObjectBuilder.start() //
        .add("name", variable.getName()) //
        .add("valueType", variable.getValueType().getName()) //
        .add("entityType", variable.getEntityType()) //
        .add("mimeType", variable.getMimeType()) //
        .add("repeatable", variable.isRepeatable()) //
        .add("occurrenceGroup", variable.getOccurrenceGroup()) //
        .add("referencedEntityType", variable.getReferencedEntityType()) //
        .add("unit", variable.getUnit()).add("index", variable.getIndex());

    if(variable.hasCategories()) {
      Collection<Object> list = new BasicDBList();
      for(Category category : variable.getCategories()) {
        list.add(marshall(category));
      }
      builder.add("categories", list);
    }

    if(variable.hasAttributes()) {
      Collection<Object> list = new BasicDBList();
      for(Attribute attribute : variable.getAttributes()) {
        list.add(marshall(attribute));
      }
      builder.add("attributes", list);
    }

    return builder.get();
  }

  private static DBObject marshall(Category category) {
    BasicDBObjectBuilder builder = BasicDBObjectBuilder.start() //
        .add("name", category.getName()).add("missing", category.isMissing());
    if(category.hasAttributes()) {
      Collection<Object> list = new BasicDBList();
      for(Attribute attribute : category.getAttributes()) {
        list.add(marshall(attribute));
      }
      builder.add("attributes", list);
    }
    return builder.get();
  }


}
