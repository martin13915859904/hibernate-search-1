/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;

/**
 * The context used when a range predicate is fully defined.
 * <p>
 * Allows to set options or to {@link #toPredicate() retrieve the predicate}.
 */
public interface RangePredicateTerminalContext
		extends SearchPredicateTerminalContext, SearchPredicateBoostContext<RangePredicateTerminalContext> {

}
