/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.impl;

import org.hibernate.search.engine.backend.spi.BackendStartContext;
import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;

class BackendStartContextImpl implements BackendStartContext {
	private final ContextualFailureCollector failureCollector;
	private final ConfigurationPropertySource configurationPropertySource;

	BackendStartContextImpl(ContextualFailureCollector failureCollector,
			ConfigurationPropertySource configurationPropertySource) {
		this.failureCollector = failureCollector;
		this.configurationPropertySource = configurationPropertySource;
	}

	@Override
	public ContextualFailureCollector getFailureCollector() {
		return failureCollector;
	}

	@Override
	public ConfigurationPropertySource getConfigurationPropertySource() {
		return configurationPropertySource;
	}
}
