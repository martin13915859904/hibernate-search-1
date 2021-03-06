/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;


/**
 * A superinterface for contexts allowing to set the "boost" of a predicate.
 *
 * @param <S> The "self" type (the actual type of the terminal context)
 */
public interface SearchPredicateBoostContext<S> {

	/**
	 * Boost the weight of the predicate in score computation.
	 *
	 * @param boost The boost factor. Higher than 1 increases the weight in score computation,
	 * between 0 and 1 lowers the weight. Lower than 0 is for experts only.
	 * @return {@code this}, for method chaining.
	 */
	S boostedTo(float boost);

}
