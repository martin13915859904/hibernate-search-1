/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.declaration;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.mapper.pojo.bridge.mapping.AnnotationMarkerBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.MarkerBuilder;

/**
 * Reference a marker mapping builder in a {@link MarkerMapping}.
 * <p>
 * Reference can be obtained using either a name or a type.
 * <p>
 * Each time the mapped annotation is encountered, an instance of the marker mapping builder will be created.
 * The builder will be passed the annotation through its {@link AnnotationMarkerBuilder#initialize(Annotation)} method,
 * and then the marker will be retrieved by calling {@link MarkerBuilder#build(org.hibernate.search.mapper.pojo.bridge.mapping.MarkerBuildContext)}.
 * <p>
 * Marker mapped this way can be parameterized:
 * the marker mapping will be able to take any attribute of the mapped annotation into account
 * in its {@link AnnotationMarkerBuilder#initialize(Annotation)} method.
 *
 * @author Yoann Rodiere
 */
@Documented
@Target({}) // Only used as a component in other annotations
@Retention(RetentionPolicy.RUNTIME)
public @interface MarkerRef {

	String builderName() default "";

	Class<? extends AnnotationMarkerBuilder<?>> builderType() default UndefinedBuilderImplementationType.class;

	/**
	 * Class used as a marker for the default value of the {@link #builderType()} attribute.
	 */
	abstract class UndefinedBuilderImplementationType implements AnnotationMarkerBuilder<Annotation> {
		private UndefinedBuilderImplementationType() {
		}
	}
}

