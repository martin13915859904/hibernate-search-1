/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;

import org.hibernate.search.engine.search.predicate.DslConverter;

/**
 * The context used when defining a range predicate, after at least one field was mentioned.
 */
public interface RangePredicateFieldSetContext extends MultiFieldPredicateFieldSetContext<RangePredicateFieldSetContext> {

	/**
	 * Target the given field in the range predicate,
	 * as an alternative to the already-targeted fields.
	 * <p>
	 * See {@link RangePredicateContext#onField(String)} for more information about targeting fields.
	 *
	 * @param absoluteFieldPath The absolute path (from the document root) of the targeted field.
	 * @return {@code this}, for method chaining.
	 *
	 * @see RangePredicateContext#onField(String)
	 */
	default RangePredicateFieldSetContext orField(String absoluteFieldPath) {
		return orFields( absoluteFieldPath );
	}

	/**
	 * Target the given fields in the range predicate,
	 * as an alternative to the already-targeted fields.
	 * <p>
	 * See {@link RangePredicateContext#onFields(String...)} for more information about targeting fields.
	 *
	 * @param absoluteFieldPaths The absolute paths (from the document root) of the targeted fields.
	 * @return {@code this}, for method chaining.
	 *
	 * @see RangePredicateContext#onFields(String...)
	 */
	RangePredicateFieldSetContext orFields(String ... absoluteFieldPaths);

	/**
	 * Require at least one of the targeted fields to be "higher than" the given value,
	 * and "lower than" another value (to be provided in following calls).
	 * <p>
	 * This syntax is essentially used like this: {@code .from( lowerBound ).to( upperBound )}.
	 *
	 * @param value The lower bound of the range. May be null, in which case the range has no lower bound
	 * and the upper bound (passed to {@link RangePredicateFromContext#to(Object)}) must not be null.
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field.
	 * See {@link DslConverter#ENABLED} for more information.
	 *
	 * Lower bound is included by default in the range.
	 *
	 * @return A context allowing to exclude the lower bound from the range or to set the upper bound of the range.
	 */
	default RangePredicateFromContext from(Object value) {
		return from( value, DslConverter.ENABLED );
	}

	/**
	 * Require at least one of the targeted fields to be "higher than" the given value,
	 * and "lower than" another value (to be provided in following calls).
	 * <p>
	 * This syntax is essentially used like this: {@code .from( lowerBound ).to( upperBound )}.
	 *
	 * @param value The lower bound of the range. May be null, in which case the range has no lower bound
	 * and the upper bound (passed to {@link RangePredicateFromContext#to(Object)}) must not be null.
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field and on the {@code dslConverter} parameter.
	 * See {@link DslConverter} for more information.
	 * @param dslConverter Controls how the {@code value} should be converted before Hibernate Search attempts to interpret it as a field value.
	 * See {@link DslConverter} for more information.
	 *
	 * Lower bound is included by default in the range.
	 *
	 * @return A context allowing to exclude the lower bound from the range or to set the upper bound of the range.
	 */
	RangePredicateFromContext from(Object value, DslConverter dslConverter);

	/**
	 * Require at least one of the targeted fields to be "higher than" the given value,
	 * with no limit as to how high it can be.
	 *
	 * @param value The lower bound of the range. Must not be null.
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field.
	 * See {@link DslConverter#ENABLED} for more information.
	 *
	 * Lower bound is included by default in the range.
	 *
	 * @return A context allowing to exclude the lower bound from the range, to set options or to get the resulting predicate.
	 */
	default RangePredicateLimitTerminalContext above(Object value) {
		return above( value, DslConverter.ENABLED );
	}

	/**
	 * Require at least one of the targeted fields to be "higher than" the given value,
	 * with no limit as to how high it can be.
	 *
	 * @param value The lower bound of the range. Must not be null.
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field and on the {@code dslConverter} parameter.
	 * See {@link DslConverter} for more information.
	 * @param dslConverter Controls how the {@code value} should be converted before Hibernate Search attempts to interpret it as a field value.
	 * See {@link DslConverter} for more information.
	 *
	 * Lower bound is included by default in the range.
	 *
	 * @return A context allowing to exclude the lower bound from the range, to set options or to get the resulting predicate.
	 */
	RangePredicateLimitTerminalContext above(Object value, DslConverter dslConverter);

	/**
	 * Require at least one of the targeted fields to be "lower than" the given value,
	 * with no limit as to how low it can be.
	 *
	 * @param value The upper bound of the range. Must not be null.
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field.
	 * See {@link DslConverter#ENABLED} for more information.
	 *
	 * Upper bound is included by default in the range.
	 *
	 * @return A context allowing to exclude the upper bound from the range, to set options or to get the resulting predicate.
	 */
	default RangePredicateLimitTerminalContext below(Object value) {
		return below( value, DslConverter.ENABLED );
	}

	/**
	 * Require at least one of the targeted fields to be "lower than" the given value,
	 * with no limit as to how low it can be.
	 *
	 * @param value The upper bound of the range. Must not be null.
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field and on the {@code dslConverter} parameter.
	 * See {@link DslConverter} for more information.
	 * @param dslConverter Controls how the {@code value} should be converted before Hibernate Search attempts to interpret it as a field value.
	 * See {@link DslConverter} for more information.
	 *
	 * Upper bound is included by default in the range.
	 *
	 * @return A context allowing to exclude the upper bound from the range, to set options or to get the resulting predicate.
	 */
	RangePredicateLimitTerminalContext below(Object value, DslConverter dslConverter);

}
