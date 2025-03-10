/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceHandler;
import org.jkiss.dbeaver.model.impl.AbstractContextDescriptor;

/**
 * DataSourceHandlerDescriptor
 */
public class DataSourceHandlerDescriptor extends AbstractContextDescriptor {
    public static final String EXTENSION_ID = "org.jkiss.dbeaver.dataSourceHandler"; //$NON-NLS-1$

    private static final Log log = Log.getLog(DataSourceHandlerDescriptor.class);

    private final String id;
    private final ObjectType impl;
    private DBPDataSourceHandler instance;


    public DataSourceHandlerDescriptor(IConfigurationElement config) {
        super(config);

        this.id = config.getAttribute("id");
        this.impl = new ObjectType(config, "class");
    }

    public String getId() {
        return id;
    }

    public synchronized DBPDataSourceHandler getInstance() throws DBException {
        if (instance == null) {
            instance = impl.createInstance(DBPDataSourceHandler.class);
        }
        return instance;
    }

    @Override
    public String toString() {
        return DataSourceHandlerDescriptor.class.getSimpleName() + ":" + id;
    }

}
