/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.mapper.pojo.dirtiness.ReindexOnUpdate;

/**
 * Given a property, defines how a dependency of the indexing process to this property
 * should affect automatic reindexing.
 * <p>
 * This annotation is generally not needed, as the default behavior is to consider all properties
 * that are actually used in the indexing process as dependencies that trigger reindexing when they are updated.
 * However, there are some reasons that could justify ignoring a dependency, in which case this annotation is necessary.
 * <p>
 * In particular, some properties might be updated very frequently,
 * or trigger reindexing to other entities that are very expensive to load in memory.
 * <p>
 * See for example this model:
 *
 * <pre><code>
 *     &#064;Indexed
 *     public class EntityA {
 *
 *         &#064;IndexedEmbedded
 *         &#064;IndexingDependency(reindexOnUpdate = ReindexOnUpdate.NO)
 *         // + some annotations defining "EntityB.a" as the inverse side of this association
 *         private EntityB b;
 *
 *     }
 *
 *     public class EntityB {
 *         private List&lt;EntityA&gt; a; // This is a very big list
 *
 *         &#064;GenericField
 *         private String field;
 *     }
 * </code></pre>
 *
 * Entity A is indexed and embeds entity B.
 * But the link back from entity B to entity A is a list that might contain a lot of elements,
 * so we don't want to load this list every time {@code EntityB#field} is updated:
 * the {@code @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.NO)} annotation prevents that.
 * This means in particular that updates to {@code EntityB#field}
 * need to be taken into account through some external process.
 * One solution would be to reindex every night in a batch process, for example,
 * which would mean allowing the index to be partly out-of-date for at most 24 hours.
 */
@Documented
@Target({ ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface IndexingDependency {

	/**
	 * @return How indexed entities using the annotated property should be reindexed when the value,
	 * or any nested value, is updated.
	 * This setting is only effective for values that are actually used when indexing
	 * (values used in field definitions, in bridges, ...).
	 */
	ReindexOnUpdate reindexOnUpdate() default ReindexOnUpdate.DEFAULT;

	ObjectPath[] derivedFrom() default {};

	/**
	 * @return An array of references to container value extractor implementation classes,
	 * allowing to precisely define which value this annotation is referring to.
	 * For instance, on a property of type {@code Map<EntityA, EntityB>},
	 * {@code @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.NO, extractors = @ContainerExtractor(type = MapKeyExtractor.class))}
	 * would mean "do not reindex the entity when map keys (of type EntityA) change",
	 * while {@code @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.NO, extractors = @ContainerExtractor(type = MapValueExtractor.class))}
	 * would mean "do not reindex the entity when map values (of type EntityB) change".
	 * By default, Hibernate Search will try to apply a set of extractors for common types
	 * ({@link Iterable}, {@link java.util.Collection}, {@link java.util.Optional}, ...)
	 * and use the first one that works.
	 * To prevent Hibernate Search from applying any extractor, set this attribute to an empty array (<code>{}</code>).
	 */
	ContainerExtractorRef[] extractors() default @ContainerExtractorRef;

}
