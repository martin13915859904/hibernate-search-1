/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.model.impl;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import org.hibernate.search.engine.mapper.model.spi.TypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoIndexableTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.util.spi.JavaClassOrdering;
import org.hibernate.search.util.SearchException;

class JavaBeanTypeModel<T> implements PojoIndexableTypeModel<T> {

	private final JavaBeanIntrospector introspector;
	private final Class<T> clazz;
	private final BeanInfo beanInfo;
	private final BeanInfo declaredBeanInfo;

	JavaBeanTypeModel(JavaBeanIntrospector introspector, Class<T> clazz) throws IntrospectionException {
		this.introspector = introspector;
		this.clazz = clazz;
		this.beanInfo = Introspector.getBeanInfo( clazz );
		this.declaredBeanInfo = Introspector.getBeanInfo( clazz, clazz.getSuperclass() );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + clazz.getName() + "]";
	}

	@Override
	public Class<T> getJavaClass() {
		return clazz;
	}

	@Override
	public boolean isSubTypeOf(TypeModel other) {
		return other instanceof JavaBeanTypeModel
				&& ( (JavaBeanTypeModel<?>) other ).clazz.isAssignableFrom( clazz );
	}

	@Override
	public <U> Optional<PojoTypeModel<U>> getSuperType(Class<U> superClassCandidate) {
		return superClassCandidate.isAssignableFrom( clazz )
				? Optional.of( introspector.getTypeModel( superClassCandidate ) )
				: Optional.empty();
	}

	@Override
	@SuppressWarnings( "unchecked" )
	public Stream<? extends PojoTypeModel<? super T>> getAscendingSuperTypes() {
		return JavaClassOrdering.get().getAscendingSuperTypes( clazz )
				.map( clazz -> introspector.getTypeModel( (Class<? super T>) clazz ) );
	}

	@Override
	@SuppressWarnings( "unchecked" )
	public Stream<? extends PojoTypeModel<? super T>> getDescendingSuperTypes() {
		return JavaClassOrdering.get().getDescendingSuperTypes( clazz )
				.map( clazz -> introspector.getTypeModel( (Class<? super T>) clazz ) );
	}

	@Override
	public <A extends Annotation> Optional<A> getAnnotationByType(Class<A> annotationType) {
		return introspector.getAnnotationByType( clazz, annotationType );
	}

	@Override
	public <A extends Annotation> Stream<A> getAnnotationsByType(Class<A> annotationType) {
		return introspector.getAnnotationsByType( clazz, annotationType );
	}

	@Override
	public Stream<? extends Annotation> getAnnotationsByMetaAnnotationType(Class<? extends Annotation> metaAnnotationType) {
		return introspector.getAnnotationsByMetaAnnotationType( clazz, metaAnnotationType );
	}

	@Override
	public PojoPropertyModel<?> getProperty(String propertyName) {
		try {
			String normalizedName = Introspector.decapitalize( propertyName );
			PropertyDescriptor propertyDescriptor = getPropertyDescriptor( normalizedName );
			return createProperty( propertyDescriptor );
		}
		catch (RuntimeException e) {
			throw new SearchException( "Exception while retrieving property model for '"
					+ propertyName + "' on '" + this + "'", e );
		}
	}

	@Override
	public Stream<PojoPropertyModel<?>> getDeclaredProperties() {
		return Arrays.stream( declaredBeanInfo.getPropertyDescriptors() )
				.filter( descriptor -> descriptor.getReadMethod() != null )
				.map( this::createProperty );
	}

	private PropertyDescriptor getPropertyDescriptor(String normalizedName) {
		PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
		for ( PropertyDescriptor descriptor : propertyDescriptors ) {
			if ( normalizedName.equals( descriptor.getName() ) ) {
				return descriptor;
			}
		}
		throw new SearchException( "JavaBean property '" + normalizedName + "' not found in '" + this + "'" );
	}

	private PojoPropertyModel<?> createProperty(PropertyDescriptor propertyDescriptor) {
		if ( propertyDescriptor.getReadMethod() == null ) {
			throw new SearchException( "Property '" + propertyDescriptor.getName() + "' on '"
					+ this + "' can't be read" );
		}
		return new JavaBeanPropertyModel<>(
				introspector, this, propertyDescriptor.getPropertyType(), propertyDescriptor
		);
	}

	@Override
	public T cast(Object instance) {
		return getJavaClass().cast( instance );
	}
}