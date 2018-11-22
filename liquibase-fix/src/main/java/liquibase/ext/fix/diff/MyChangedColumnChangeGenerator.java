/*
 * COPYRIGHT (c) 2018 VOCADO, LLC.  ALL RIGHTS RESERVED.  THIS SOFTWARE CONTAINS
 * TRADE SECRETS AND/OR CONFIDENTIAL INFORMATION PROPRIETARY TO VOCADO, LLC AND/OR
 * ITS LICENSORS. ACCESS TO AND USE OF THIS INFORMATION IS STRICTLY LIMITED AND
 * CONTROLLED BY VOCADO, LLC.  THIS SOFTWARE MAY NOT BE COPIED, MODIFIED, DISTRIBUTED,
 * DISPLAYED, DISCLOSED OR USED IN ANY WAY NOT EXPRESSLY AUTHORIZED BY VOCADO, LLC IN WRITING.
 */

package liquibase.ext.fix.diff;

import liquibase.change.AddColumnConfig;
import liquibase.change.Change;
import liquibase.change.core.AddColumnChange;
import liquibase.change.core.DropColumnChange;
import liquibase.change.core.DropDefaultValueChange;
import liquibase.change.core.ModifyDataTypeChange;
import liquibase.change.core.OutputChange;
import liquibase.change.core.RawSQLChange;
import liquibase.change.core.RenameColumnChange;
import liquibase.database.Database;
import liquibase.database.core.MSSQLDatabase;
import liquibase.database.core.OracleDatabase;
import liquibase.datatype.DataTypeFactory;
import liquibase.diff.Difference;
import liquibase.diff.ObjectDifferences;
import liquibase.diff.output.DiffOutputControl;
import liquibase.diff.output.changelog.core.ChangedColumnChangeGenerator;
import liquibase.diff.output.changelog.core.MissingColumnChangeGenerator;
import liquibase.structure.DatabaseObject;
import liquibase.structure.core.Column;
import liquibase.structure.core.DataType;
import liquibase.structure.core.Table;

import java.util.Arrays;
import java.util.List;

public class MyChangedColumnChangeGenerator
        extends ChangedColumnChangeGenerator
        implements HibernateOverrideChange {

    @Override
    public int getPriority(final Class<? extends DatabaseObject> objectType, final Database database) {
        if (Column.class.isAssignableFrom(objectType)) {
            return THE_TOP_OF_THE_PRIORITY;
        }
        return PRIORITY_NONE;
    }

    @Override
    protected void handleTypeDifferences(final Column column, final ObjectDifferences differences, final DiffOutputControl control, final List<Change> changes, final Database referenceDatabase, final Database comparisonDatabase) {
        if (isHibernatePlugin(referenceDatabase, comparisonDatabase)) {
            final Difference typeDifference = differences.getDifference("type");
            if (isTypeReallyDifferent(typeDifference, comparisonDatabase)) {
                handleTypeDifferences2(column, differences, control, changes, referenceDatabase, comparisonDatabase);
            }
        } else {
            super.handleTypeDifferences(column, differences, control, changes, referenceDatabase, comparisonDatabase);
        }
    }

    /**
     * Compares the reference and compared value based on the actual {@link DataType} representation in the comparison database.
     * This prevents false positives to be detected as a change.
     */
    private boolean isTypeReallyDifferent(final Difference typeDifference, final Database comparisonDatabase) {
        if (typeDifference != null) {
            final String reference = DataTypeFactory.getInstance().from((DataType) typeDifference.getReferenceValue(), comparisonDatabase).toString();
            final String compared = DataTypeFactory.getInstance().from((DataType) typeDifference.getComparedValue(), comparisonDatabase).toString();

            return !compared.startsWith(reference);
        }
        return false;
    }

    /**
     * This method is identical to the {@link ChangedColumnChangeGenerator#handleTypeDifferences} except that it changes how it
     * generates the new type associated to the built {@link Change}.
     * <p>
     * The official code generates the new type doing the following call:
     * <pre>
     * DataType referenceType = (DataType) typeDifference.getReferenceValue();
     * change.setNewDataType(DataTypeFactory.getInstance().from(referenceType, comparisonDatabase).toString());
     * </pre>
     * My class simply use the {@code change.setNewDataType(column.getType().toString());} which is similar to what
     * the {@link MissingColumnChangeGenerator} does when it generates new columns.
     * <p>
     * This prevents unnecessary changes to be generated at the DB schema level.
     */
    private void handleTypeDifferences2(Column column, ObjectDifferences differences, DiffOutputControl control, List<Change> changes, Database referenceDatabase, Database comparisonDatabase) {
        Difference typeDifference = differences.getDifference("type");
        if (typeDifference != null) {
            String catalogName = null;
            String schemaName = null;
            if (control.getIncludeCatalog()) {
                catalogName = column.getRelation().getSchema().getCatalog().getName();
            }
            if (control.getIncludeSchema()) {
                schemaName = column.getRelation().getSchema().getName();
            }


            String tableName = column.getRelation().getName();

            if ((comparisonDatabase instanceof OracleDatabase) && ("clob".equalsIgnoreCase(((DataType) typeDifference
                    .getReferenceValue()).getTypeName()) || "clob".equalsIgnoreCase(((DataType) typeDifference
                    .getComparedValue()).getTypeName()))) {
                String tempColName = "TEMP_CLOB_CONVERT";
                OutputChange outputChange = new OutputChange();
                outputChange.setMessage("Cannot convert directly from " + ((DataType) typeDifference.getComparedValue()).getTypeName() + " to " + ((DataType) typeDifference.getReferenceValue()).getTypeName() + ". Instead a new column will be created and the data transferred. This may cause unexpected side effects including constraint issues and/or table locks.");
                changes.add(outputChange);

                AddColumnChange addColumn = new AddColumnChange();
                addColumn.setCatalogName(catalogName);
                addColumn.setSchemaName(schemaName);
                addColumn.setTableName(tableName);
                AddColumnConfig addColumnConfig = new AddColumnConfig(column);
                addColumnConfig.setName(tempColName);
                addColumnConfig.setType(typeDifference.getReferenceValue().toString());
                addColumnConfig.setAfterColumn(column.getName());
                addColumn.setColumns(Arrays.asList(addColumnConfig));
                changes.add(addColumn);

                changes.add(new RawSQLChange("UPDATE " + referenceDatabase.escapeObjectName(tableName, Table.class) + " SET " + tempColName + "=" + referenceDatabase.escapeObjectName(column.getName(), Column.class)));

                DropColumnChange dropColumnChange = new DropColumnChange();
                dropColumnChange.setCatalogName(catalogName);
                dropColumnChange.setSchemaName(schemaName);
                dropColumnChange.setTableName(tableName);
                dropColumnChange.setColumnName(column.getName());
                changes.add(dropColumnChange);

                RenameColumnChange renameColumnChange = new RenameColumnChange();
                renameColumnChange.setCatalogName(catalogName);
                renameColumnChange.setSchemaName(schemaName);
                renameColumnChange.setTableName(tableName);
                renameColumnChange.setOldColumnName(tempColName);
                renameColumnChange.setNewColumnName(column.getName());
                changes.add(renameColumnChange);

            } else {
                if ((comparisonDatabase instanceof MSSQLDatabase) && (column.getDefaultValue() != null)) { //have to drop the default value, will be added back with the "data type changed" logic.
                    DropDefaultValueChange dropDefaultValueChange = new DropDefaultValueChange();
                    dropDefaultValueChange.setCatalogName(catalogName);
                    dropDefaultValueChange.setSchemaName(schemaName);
                    dropDefaultValueChange.setTableName(tableName);
                    dropDefaultValueChange.setColumnName(column.getName());
                    changes.add(dropDefaultValueChange);
                }

                ModifyDataTypeChange change = new ModifyDataTypeChange();
                change.setCatalogName(catalogName);
                change.setSchemaName(schemaName);
                change.setTableName(tableName);
                change.setColumnName(column.getName());
                change.setNewDataType(column.getType().toString());

                changes.add(change);
            }
        }
    }

}
