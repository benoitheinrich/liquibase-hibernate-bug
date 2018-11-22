/*
 * COPYRIGHT (c) 2018 VOCADO, LLC.  ALL RIGHTS RESERVED.  THIS SOFTWARE CONTAINS
 * TRADE SECRETS AND/OR CONFIDENTIAL INFORMATION PROPRIETARY TO VOCADO, LLC AND/OR
 * ITS LICENSORS. ACCESS TO AND USE OF THIS INFORMATION IS STRICTLY LIMITED AND
 * CONTROLLED BY VOCADO, LLC.  THIS SOFTWARE MAY NOT BE COPIED, MODIFIED, DISTRIBUTED,
 * DISPLAYED, DISCLOSED OR USED IN ANY WAY NOT EXPRESSLY AUTHORIZED BY VOCADO, LLC IN WRITING.
 */

package liquibase.ext.fix.diff;

import io.vavr.Tuple2;
import io.vavr.collection.Stream;
import liquibase.change.Change;
import liquibase.database.Database;
import liquibase.diff.ObjectDifferences;
import liquibase.diff.output.DiffOutputControl;
import liquibase.diff.output.changelog.ChangeGeneratorChain;
import liquibase.diff.output.changelog.core.ChangedForeignKeyChangeGenerator;
import liquibase.structure.DatabaseObject;
import liquibase.structure.core.ForeignKey;
import liquibase.structure.core.ForeignKeyConstraintType;

import java.util.function.Predicate;

/**
 * Hibernate doesn't know about all the variations that occur with foreign keys but just whether the FK exists or not.
 * To prevent changing customized foreign keys, we suppress all foreign key changes from hibernate.
 */
public class MyChangedForeignKeyChangeGenerator
        extends ChangedForeignKeyChangeGenerator
        implements IgnorableChange {

    private static final String DELETE_RULE = "deleteRule";
    private static final String UPDATE_RULE = "updateRule";
    private static final String VALIDATE = "validate";

    @Override
    public int getPriority(Class<? extends DatabaseObject> objectType, Database database) {
        if (ForeignKey.class.isAssignableFrom(objectType)) {
            return THE_TOP_OF_THE_PRIORITY;
        }
        return PRIORITY_NONE;
    }

    @Override
    public Change[] fixChanged(DatabaseObject changedObject, ObjectDifferences differences, DiffOutputControl control, Database referenceDatabase, Database comparisonDatabase, ChangeGeneratorChain chain) {
        if (removeIgnorableChanges(differences, referenceDatabase, comparisonDatabase)) return null;

        return super.fixChanged(changedObject, differences, control, referenceDatabase, comparisonDatabase, chain);
    }

    @Override
    public Stream<Tuple2<String, Predicate<Object>>> getIgnorableChanges() {
        return Stream.of(rule(VALIDATE, Boolean.TRUE::equals))
                .append(rule(UPDATE_RULE, this::isIgnorable))
                .append(rule(DELETE_RULE, this::isIgnorable));
    }

    private boolean isIgnorable(final Object comparedValue) {
        return comparedValue instanceof ForeignKeyConstraintType
                && ((ForeignKeyConstraintType) comparedValue).name().equals("importedKeyRestrict");
    }
}
