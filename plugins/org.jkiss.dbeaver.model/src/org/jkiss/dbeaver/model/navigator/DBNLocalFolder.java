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
package org.jkiss.dbeaver.model.navigator;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * DBNLocalFolder
 */
public class DBNLocalFolder extends DBNNode implements DBNContainer
{
    private DBPDataSourceFolder folder;

    public DBNLocalFolder(DBNProjectDatabases parentNode, DBPDataSourceFolder folder)
    {
        super(parentNode);
        this.folder = folder;
    }

    @Override
    protected void dispose(boolean reflect)
    {
        super.dispose(reflect);
    }

    public DBPDataSourceFolder getFolder() {
        return folder;
    }

    public void setFolder(DBPDataSourceFolder folder) {
        this.folder = folder;
    }

    public DBPDataSourceRegistry getDataSourceRegistry() {
        return ((DBNProjectDatabases)parentNode).getDataSourceRegistry();
    }

    @NotNull
    @Property(viewable = true, order = 1)
    public String getName()
    {
        return getNodeName();
    }

    @Override
    public Object getValueObject()
    {
        return folder;
    }

    @Override
    public String getChildrenType()
    {
        return ModelMessages.model_navigator_Connection;
    }

    @Override
    public String getNodeType()
    {
        return "folder";
    }

    @Override
    public String getNodeName()
    {
        return folder.getName();
    }

    @Override
    public String getNodeDescription()
    {
        return folder.getDescription();
    }

    @Override
    public DBPImage getNodeIcon()
    {
        DBPImage dsIcon = null;
        for (DBNDataSource ds : getDataSources()) {
            final DBPImage icon = DBValueFormatting.getObjectImage(ds.getDataSourceContainer());
            if (dsIcon == null) {
                dsIcon = icon;
            } else if (!CommonUtils.equalObjects(dsIcon, icon)) {
                dsIcon = null;
                break;
            }
        }
        return DBIcon.TREE_FOLDER_DATABASE;
/*
        if (dsIcon == null) {
            return DBIcon.TREE_DATABASE_CATEGORY;
        } else {
            // All datasources have the same icon.
            // Make it a folder
            return new DBIconComposite(
                dsIcon,
                false,
                null,
                null,
                null,
                DBIcon.OVER_FOLDER);
        }
*/
    }

    @Override
    public String getNodeItemPath() {
        return makeLocalFolderItemPath(folder);
    }

    @Override
    public DBNProjectDatabases getParentNode() {
        return (DBNProjectDatabases)super.getParentNode();
    }

    @Override
    public boolean allowsChildren()
    {
        return true;
    }

    @Override
    public boolean hasChildren(boolean navigableOnly) {
        if (!ArrayUtils.isEmpty(folder.getChildren())) {
            return true;
        }
        for (DBNDataSource dataSource : getParentNode().getDataSources()) {
            if (folder == dataSource.getDataSourceContainer().getFolder()) {
                return true;
            }
        }
        return false;
    }

    public DBNNode getLogicalParent() {
        if (folder.getParent() == null) {
            return getParentNode();
        } else {
            return getParentNode().getFolderNode(folder.getParent());
        }
    }

    @Override
    public DBNNode[] getChildren(DBRProgressMonitor monitor)
    {
        if (ArrayUtils.isEmpty(folder.getChildren())) {
            return ArrayUtils.toArray(DBNDataSource.class, getDataSources());
        }
        final List<DBNNode> nodes = new ArrayList<>();
        for (DBPDataSourceFolder childFolder : folder.getChildren()) {
            nodes.add(getParentNode().getFolderNode(childFolder));
        }
        nodes.addAll(getDataSources());
        sortNodes(nodes);
        return ArrayUtils.toArray(DBNNode.class, nodes);
    }

    public List<DBNDataSource> getDataSources()
    {
        List<DBNDataSource> children = new ArrayList<>();
        DBNProjectDatabases parent = getParentNode();
        for (DBNDataSource dataSource : parent.getDataSources()) {
            if (folder == dataSource.getDataSourceContainer().getFolder()) {
                children.add(dataSource);
            }
        }
        sortNodes(children);
        return children;
    }

    @Override
    public Class<? extends DBSObject> getChildrenClass()
    {
        return DBPDataSourceContainer.class;
    }

    @Override
    public boolean supportsDrop(DBNNode otherNode)
    {
        return otherNode == null || otherNode instanceof DBNDataSource ||
            (otherNode instanceof DBNLocalFolder && ((DBNLocalFolder) otherNode).getFolder().canMoveTo(getFolder()));
    }

    @Override
    public void dropNodes(DBRProgressMonitor monitor, Collection<DBNNode> nodes) throws DBException {
        for (DBNNode node : nodes) {
            if (node.getOwnerProject() == this.getOwnerProject()) {
                if (node instanceof DBNDataSource) {
                    ((DBNDataSource) node).moveToFolder(getOwnerProject(), folder);
                } else if (node instanceof DBNLocalFolder) {
                    DBPDataSourceFolder nodeFolder = ((DBNLocalFolder) node).getFolder();
                    getDataSourceRegistry().moveFolder(
                        nodeFolder.getFolderPath(),
                        generateNewFolderPath(this.getFolder(), nodeFolder.getName())
                    );
                }
            } else {
                if (node instanceof DBNDataSource) {
                    getParentNode().moveNodesToFolder(Collections.singletonList(node), this.getFolder());
                } else if (node instanceof DBNLocalFolder) {
                    log.error("Can't move nodes between projects");
                }
            }
        }
        DBNModel.updateConfigAndRefreshDatabases(this);
    }

    @Override
    public boolean supportsRename()
    {
        return true;
    }

    @Override
    public void rename(DBRProgressMonitor monitor, String newName) throws DBException
    {
        getDataSourceRegistry().moveFolder(folder.getFolderPath(), generateNewFolderPath(folder.getParent(), newName));
        DBNModel.updateConfigAndRefreshDatabases(this);
    }

    public boolean hasConnected() {
        for (DBPDataSourceFolder childFolder : folder.getChildren()) {
            if (getParentNode().getFolderNode(childFolder).hasConnected()) {
                return true;
            }
        }
        for (DBNDataSource ds : getDataSources()) {
            if (ds.getDataSource() != null) {
                return true;
            }
        }
        return false;
    }

    public List<DBNDataSource> getNestedDataSources() {
        List<DBNDataSource> result = new ArrayList<>();
        fillNestedDataSources(result);
        return result;
    }

    private void fillNestedDataSources(List<DBNDataSource> dataSources) {
        for (DBNNode childFolder : getChildren(new VoidProgressMonitor())) {
            if (childFolder instanceof DBNLocalFolder) {
                ((DBNLocalFolder) childFolder).fillNestedDataSources(dataSources);
            }
        }
        dataSources.addAll(getDataSources());
    }

    public String generateNewFolderPath(DBPDataSourceFolder newParent, String newName) {
        var folderPath = Path.of(getFolder().getFolderPath());
        if (newParent != null) {
            return normalizePath(Path.of(newParent.getFolderPath()).resolve(newName).toString());
        }
        return normalizePath(folderPath.getFileName().resolveSibling(newName).toString());
    }

    @NotNull
    public static String makeLocalFolderItemPath(DBPDataSourceFolder folder) {
        return makeLocalFolderItemPath(folder.getDataSourceRegistry().getProject().getId(), folder.getFolderPath());
    }

    @NotNull
    public static String makeLocalFolderItemPath(@NotNull String projectId, @NotNull String folderPath) {
        return NodePathType.folder.getPrefix() + projectId + "/" + folderPath;
    }

    @Override
    public String toString() {
        return folder.getFolderPath();
    }

    private String normalizePath(@NotNull String folderPath) {
        return folderPath.replace("\\", "/");
    }

}
