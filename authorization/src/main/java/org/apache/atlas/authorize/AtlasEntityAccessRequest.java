/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.authorize;

import org.apache.atlas.model.instance.AtlasClassification;
import org.apache.atlas.model.instance.AtlasEntityHeader;
import org.apache.atlas.type.AtlasClassificationType;
import org.apache.atlas.type.AtlasEntityType;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.commons.lang.StringUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class AtlasEntityAccessRequest extends AtlasAccessRequest {
    private final AtlasEntityHeader   entity;
    private final String              entityId;
    private final AtlasClassification classification;
    private final String              attributeName;
    private final AtlasTypeRegistry   typeRegistry;
    private final Set<String>         entityClassifications;


    public AtlasEntityAccessRequest(AtlasTypeRegistry typeRegistry, AtlasPrivilege action) {
        this(typeRegistry, action, null, null, null, null, null);
    }

    public AtlasEntityAccessRequest(AtlasTypeRegistry typeRegistry, AtlasPrivilege action, AtlasEntityHeader entity) {
        this(typeRegistry, action, entity, null, null, null, null);
    }

    public AtlasEntityAccessRequest(AtlasTypeRegistry typeRegistry, AtlasPrivilege action, AtlasEntityHeader entity, AtlasClassification classification) {
        this(typeRegistry, action, entity, classification, null, null, null);
    }

    public AtlasEntityAccessRequest(AtlasTypeRegistry typeRegistry, AtlasPrivilege action, AtlasEntityHeader entity, String attributeName) {
        this(typeRegistry, action, entity, null, attributeName, null, null);
    }

    public AtlasEntityAccessRequest(AtlasTypeRegistry typeRegistry, AtlasPrivilege action, AtlasEntityHeader entity, String userName, Set<String> userGroups) {
        this(typeRegistry, action, entity, null, null, userName, userGroups);
    }

    public AtlasEntityAccessRequest(AtlasTypeRegistry typeRegistry, AtlasPrivilege action, AtlasEntityHeader entity, AtlasClassification classification, String userName, Set<String> userGroups) {
        this(typeRegistry, action, entity, classification, null, userName, userGroups);
    }

    public AtlasEntityAccessRequest(AtlasTypeRegistry typeRegistry, AtlasPrivilege action, AtlasEntityHeader entity, String attributeName, String userName, Set<String> userGroups) {
        this(typeRegistry, action, entity, null, attributeName, userName, userGroups);
    }

    public AtlasEntityAccessRequest(AtlasTypeRegistry typeRegistry, AtlasPrivilege action, AtlasEntityHeader entity, AtlasClassification classification, String attributeName, String userName, Set<String> userGroups) {
        super(action, userName, userGroups);

        this.entity         = entity;
        this.entityId       = entity != null ? (String) entity.getAttribute("qualifiedName") : null;
        this.classification = classification;
        this.attributeName  = attributeName;
        this.typeRegistry   = typeRegistry;

        if (entity == null || entity.getClassifications() == null) {
            this.entityClassifications = Collections.emptySet();
        } else {
            this.entityClassifications = new HashSet<>();

            for (AtlasClassification classify : entity.getClassifications()) {
                this.entityClassifications.add(classify.getTypeName());
            }
        }
    }

    public AtlasEntityHeader getEntity() {
        return entity;
    }

    public String getEntityId() {
        return entityId;
    }

    public AtlasClassification getClassification() {
        return classification;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public String getEntityType() {
        return entity == null ? StringUtils.EMPTY : entity.getTypeName();
    }

    public Set<String> getEntityClassifications() {
        return entityClassifications;
    }

    public Set<String> getEntityTypeAndAllSuperTypes() {
        final Set<String> ret;

        if (entity == null) {
            ret = Collections.emptySet();
        } else if (typeRegistry == null) {
            ret = Collections.singleton(entity.getTypeName());
        } else {
            AtlasEntityType entityType = typeRegistry.getEntityTypeByName(entity.getTypeName());

            ret = entityType != null ? entityType.getTypeAndAllSuperTypes() : Collections.singleton(entity.getTypeName());
        }

        return ret;
    }

    public Set<String> getClassificationTypeAndAllSuperTypes(String classificationName) {
        if (typeRegistry != null && classificationName != null) {
            AtlasClassificationType classificationType = typeRegistry.getClassificationTypeByName(classificationName);

            return classificationType == null ? Collections.emptySet() : classificationType.getTypeAndAllSuperTypes();
        }

        return Collections.emptySet();
    }

    @Override
    public String toString() {
        return "AtlasEntityAccessRequest[entity=" + entity + ", classification=" + classification + ", attributeName" + attributeName +
                                         ", action=" + getAction() + ", accessTime=" + getAccessTime() + ", user=" + getUser() +
                                         ", userGroups=" + getUserGroups() + ", clientIPAddress=" + getClientIPAddress() + "]";
    }
}


