/*
 Copyright 2013 Red Hat, Inc. and/or its affiliates.

 This file is part of lightblue.

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.redhat.lightblue.metadata;

import java.util.List;
import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import com.redhat.lightblue.metadata.types.UIDType;
import com.redhat.lightblue.metadata.types.IntegerType;
import com.redhat.lightblue.metadata.types.StringType;

import com.redhat.lightblue.metadata.constraints.RequiredConstraint;

import com.redhat.lightblue.util.JsonDoc;
import com.redhat.lightblue.util.Path;

public class UIDTest {

    private static final JsonNodeFactory nodeFactory=JsonNodeFactory.withExactBigDecimals(false);

    private EntityMetadata getMD1() {
        EntityMetadata entityMetadata = new EntityMetadata("test");

        entityMetadata.getFields().addNew(new SimpleField("simpleInteger", IntegerType.TYPE));
        entityMetadata.getFields().addNew(new SimpleField("simpleUID", UIDType.TYPE));
        ObjectField objectField1 = new ObjectField("obj1");
        entityMetadata.getFields().addNew(objectField1);
        objectField1.getFields().addNew(new SimpleField("nestedSimpleInteger", IntegerType.TYPE));
        objectField1.getFields().addNew(new SimpleField("nestedUID", UIDType.TYPE));
        ObjectField objectField2 = new ObjectField("nested");
        objectField1.getFields().addNew(objectField2);
        objectField2.getFields().addNew(new SimpleField("doubleNestedUID", UIDType.TYPE));
        ArrayField arrayField1 = new ArrayField("simpleArr", new SimpleArrayElement(StringType.TYPE));
        objectField2.getFields().addNew(arrayField1);

        ObjectArrayElement objectArrayElement = new ObjectArrayElement();
        objectArrayElement.getFields().addNew(new SimpleField("nestedArrObjString1", StringType.TYPE));
        objectArrayElement.getFields().addNew(new SimpleField("nestedArrObjUID", UIDType.TYPE));
        ArrayField arrayField2 = new ArrayField("objArr", objectArrayElement);
        objectField2.getFields().addNew(arrayField2);

        return entityMetadata;
    }

    private EntityMetadata getMDWithReq() {
        EntityMetadata entityMetadata = new EntityMetadata("test");
        List<FieldConstraint> list=new ArrayList<>();
        list.add(new RequiredConstraint());

        entityMetadata.getFields().addNew(new SimpleField("simpleInteger", IntegerType.TYPE));
        Field f=new SimpleField("simpleUID",UIDType.TYPE);
        f.setConstraints(list);
        entityMetadata.getFields().addNew(f);
        ObjectField objectField1 = new ObjectField("obj1");
        entityMetadata.getFields().addNew(objectField1);
        objectField1.getFields().addNew(new SimpleField("nestedSimpleInteger", IntegerType.TYPE));
        f=new SimpleField("nestedUID",UIDType.TYPE);
        f.setConstraints(list);
        objectField1.getFields().addNew(f);
        ObjectField objectField2 = new ObjectField("nested");
        objectField1.getFields().addNew(objectField2);
        f=new SimpleField("doubleNestedUID",UIDType.TYPE);
        f.setConstraints(list);
        objectField2.getFields().addNew(f);
        ArrayField arrayField1 = new ArrayField("simpleArr", new SimpleArrayElement(StringType.TYPE));
        objectField2.getFields().addNew(arrayField1);

        ObjectArrayElement objectArrayElement = new ObjectArrayElement();
        objectArrayElement.getFields().addNew(new SimpleField("nestedArrObjString1", StringType.TYPE));
        f=new SimpleField("nestedArrObjUID",UIDType.TYPE);
        f.setConstraints(list);
        objectArrayElement.getFields().addNew(f);
        ArrayField arrayField2 = new ArrayField("objArr", objectArrayElement);
        objectField2.getFields().addNew(arrayField2);

        return entityMetadata;
    }


    @Test
    public void nonReq() throws Exception {
        EntityMetadata md=getMD1();
        ObjectNode node=nodeFactory.objectNode();
        node.put("simpleInteger",10);
        JsonDoc doc=new JsonDoc(node);
        UIDFields.initializeUIDFields(nodeFactory,md,doc);
        Assert.assertEquals(10,doc.get(new Path("simpleInteger")).asInt());
        Assert.assertNull(doc.get(new Path("simpleUID")));
        Assert.assertNull(doc.get(new Path("obj1")));
        Assert.assertNull(doc.get(new Path("obj1.nested.objArr")));
    }

    @Test
    public void req() throws Exception {
        EntityMetadata md=getMDWithReq();
        ObjectNode node=nodeFactory.objectNode();
        node.put("simpleInteger",10);
        JsonDoc doc=new JsonDoc(node);
        UIDFields.initializeUIDFields(nodeFactory,md,doc);
        Assert.assertEquals(10,doc.get(new Path("simpleInteger")).asInt());
        Assert.assertNotNull(doc.get(new Path("simpleUID")).asText());
        Assert.assertNotNull(doc.get(new Path("obj1.nestedUID")).asText());
        Assert.assertNotNull(doc.get(new Path("obj1.nested.doubleNestedUID")).asText());
        Assert.assertNull(doc.get(new Path("obj1.nested.objArr")));
    }

    @Test
    public void reqArr() throws Exception {
        EntityMetadata md=getMDWithReq();
        ObjectNode node=nodeFactory.objectNode();
        node.put("simpleInteger",10);
        
        ArrayNode arr=nodeFactory.arrayNode();
        arr.add(nodeFactory.objectNode());
        arr.add(nodeFactory.objectNode());
        ObjectNode obj1=nodeFactory.objectNode();
        node.put("obj1",obj1);
        ObjectNode nested=nodeFactory.objectNode();
        obj1.put("nested",nested);
        nested.put("objArr",arr);

        JsonDoc doc=new JsonDoc(node);
        UIDFields.initializeUIDFields(nodeFactory,md,doc);
        Assert.assertNotNull(doc.get(new Path("obj1.nested.objArr.0.nestedArrObjUID")));
        Assert.assertNotNull(doc.get(new Path("obj1.nested.objArr.1.nestedArrObjUID")));
        Assert.assertNull(doc.get(new Path("obj1.nested.objArr.2.nestedArrObjUID")));
    }
}
