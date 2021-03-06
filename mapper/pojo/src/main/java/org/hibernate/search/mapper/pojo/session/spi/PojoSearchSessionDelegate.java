/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.session.spi;

import java.util.Collection;

import org.hibernate.search.mapper.pojo.work.spi.PojoWorkPlan;
import org.hibernate.search.mapper.pojo.search.spi.PojoSearchScopeDelegate;
import org.hibernate.search.mapper.pojo.work.spi.PojoSessionWorkExecutor;

public interface PojoSearchSessionDelegate {

	<E, O> PojoSearchScopeDelegate<E, O> createPojoSearchScope(Collection<? extends Class<? extends E>> targetedTypes);

	PojoWorkPlan createWorkPlan();

	PojoSessionWorkExecutor createSessionWorkExecutor();

}
