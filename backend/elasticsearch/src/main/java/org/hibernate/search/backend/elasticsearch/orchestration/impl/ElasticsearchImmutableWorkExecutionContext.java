/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.orchestration.impl;

import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchClient;
import org.hibernate.search.backend.elasticsearch.gson.spi.GsonProvider;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWorkExecutionContext;
import org.hibernate.search.util.AssertionFailure;

/**
 * The execution context used in {@link ElasticsearchWorkProcessor}
 * when the context must be shared by multiple threads.
 * <p>
 * This context is immutable and thread-safe, but doesn't support
 * {@link #setIndexDirty(String)}.
 *
 * @author Yoann Rodiere
 */
class ElasticsearchImmutableWorkExecutionContext implements ElasticsearchWorkExecutionContext {

	private final ElasticsearchClient client;
	private final GsonProvider gsonProvider;

	public ElasticsearchImmutableWorkExecutionContext(ElasticsearchClient client, GsonProvider gsonProvider) {
		super();
		this.client = client;
		this.gsonProvider = gsonProvider;
	}

	@Override
	public ElasticsearchClient getClient() {
		return client;
	}

	@Override
	public GsonProvider getGsonProvider() {
		return gsonProvider;
	}

	@Override
	public void setIndexDirty(URLEncodedString indexName) {
		throw new AssertionFailure( "Unexpected dirty index with a default context."
				+ " Works that may alter index content should be executed"
				+ " through the " + ElasticsearchWorkProcessor.class.getName()
				+ ", using an appropriate context." );
	}

}