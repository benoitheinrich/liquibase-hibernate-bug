/*
 * COPYRIGHT (c) 2018 VOCADO, LLC.  ALL RIGHTS RESERVED.  THIS SOFTWARE CONTAINS
 * TRADE SECRETS AND/OR CONFIDENTIAL INFORMATION PROPRIETARY TO VOCADO, LLC AND/OR
 * ITS LICENSORS. ACCESS TO AND USE OF THIS INFORMATION IS STRICTLY LIMITED AND
 * CONTROLLED BY VOCADO, LLC.  THIS SOFTWARE MAY NOT BE COPIED, MODIFIED, DISTRIBUTED,
 * DISPLAYED, DISCLOSED OR USED IN ANY WAY NOT EXPRESSLY AUTHORIZED BY VOCADO, LLC IN WRITING.
 */

package liquibase.ext.fix.diff;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.Stream;
import liquibase.database.Database;
import liquibase.diff.Difference;
import liquibase.diff.ObjectDifferences;

import java.util.function.Predicate;

interface IgnorableChange extends HibernateOverrideChange {
    Stream<Tuple2<String, Predicate<Object>>> getIgnorableChanges();

    default Tuple2<String, Predicate<Object>> rule(String name, Predicate<Object> isIgnorable) {
        return Tuple.of(name, isIgnorable);
    }

    default void removeIgnorableDifference(final ObjectDifferences differences, final String name, Predicate<Object> isIgnorable) {
        final Difference diff = differences.getDifference(name);
        if (diff.getReferenceValue() == null && isIgnorable.test(diff.getComparedValue())) {
            differences.removeDifference(name);
        }
    }

    default boolean removeIgnorableChanges(final ObjectDifferences differences, final Database referenceDatabase, final Database comparisonDatabase) {
        if (isHibernatePlugin(referenceDatabase, comparisonDatabase)) {
            getIgnorableChanges()
                    .forEach(t -> removeIgnorableDifference(differences, t._1, t._2));
            return !differences.hasDifferences();
        }
        return false;
    }

}
