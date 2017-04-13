/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.jdbc.proxy;

import javax.sql.DataSource;

import com.p6spy.engine.spy.P6DataSource;
import com.vladmihalcea.flexypool.FlexyPoolDataSource;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Proxy DataSource configurations imported by {@link DataSourceAutoConfiguration}.
 *
 * @author Arthur Gavlyukovskiy
 */
@Configuration
@ConditionalOnProperty(prefix = "spring.datasource.proxy", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ProxyDataSourceConfiguration {

	@Bean
	@ConditionalOnClass(P6DataSource.class)
	public DataSourceDecorator p6SpyDataSourceDecorator() {
		return new DataSourceDecorator() {
			@Override
			public DataSource decorate(DataSource dataSource) {
				return new P6DataSource(dataSource);
			}
		};
	}

	@Bean
	@ConditionalOnClass(net.ttddyy.dsproxy.support.ProxyDataSource.class)
	public DataSourceDecorator proxyDataSourceDecorator() {
		return new DataSourceDecorator() {
			@Override
			public DataSource decorate(DataSource dataSource) {
				return new net.ttddyy.dsproxy.support.ProxyDataSource(dataSource);
			}
		};
	}

	@Bean
	@ConditionalOnClass(FlexyPoolDataSource.class)
	public DataSourceDecorator flexyPoolDataSourceDecorator() {
		return new DataSourceDecorator() {
			@Override
			public DataSource decorate(DataSource dataSource) {
				try {
					return new FlexyPoolDataSource<DataSource>(dataSource);
				}
				catch (Exception e) {
					return dataSource;
				}
			}
		};
	}

	@Bean
	@ConditionalOnBean(DataSourceDecorator.class)
	public DataSourceProxyingBeanPostProcessor dataSourceProxyingBeanPostProcessor() {
		return new DataSourceProxyingBeanPostProcessor();
	}
}
