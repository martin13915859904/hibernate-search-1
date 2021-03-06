/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.index.spi.IndexWorkExecutor;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendBehavior;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubIndexWork;

public class StubIndexWorkExecutor implements IndexWorkExecutor {

	private final String indexName;
	private final StubBackendBehavior behavior;

	public StubIndexWorkExecutor(String indexName, StubBackendBehavior behavior) {
		this.indexName = indexName;
		this.behavior = behavior;
	}

	@Override
	public CompletableFuture<?> optimize() {
		StubIndexWork work = StubIndexWork.builder( StubIndexWork.Type.OPTIMIZE ).build();
		return behavior.executeBulkWork( indexName, work );
	}

	@Override
	public CompletableFuture<?> purge(String tenantId) {
		StubIndexWork work = StubIndexWork.builder( StubIndexWork.Type.PURGE ).tenantIdentifier( tenantId ).build();
		return behavior.executeBulkWork( indexName, work );
	}

	@Override
	public CompletableFuture<?> flush() {
		StubIndexWork work = StubIndexWork.builder( StubIndexWork.Type.FLUSH ).build();
		return behavior.executeBulkWork( indexName, work );
	}
}
