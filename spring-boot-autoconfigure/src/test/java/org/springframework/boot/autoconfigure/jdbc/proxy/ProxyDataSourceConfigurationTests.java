/*
 * Copyright 2012-2017 the original author or authors.
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

import java.io.PrintWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Random;
import java.util.logging.Logger;

import javax.sql.DataSource;

import com.p6spy.engine.spy.P6DataSource;
import com.vladmihalcea.flexypool.FlexyPoolDataSource;
import com.vladmihalcea.flexypool.config.PropertyLoader;
import com.vladmihalcea.flexypool.metric.MetricsFactory;
import com.vladmihalcea.flexypool.metric.MetricsFactoryService;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfigurationTests;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_MOCKS;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ProxyDataSourceConfiguration}.
 *
 * @author Arthur Gavlyukovskiy
 */
public class ProxyDataSourceConfigurationTests {

	private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@Before
	public void init() {
		EnvironmentTestUtils.addEnvironment(this.context,
			"spring.datasource.initialize:false",
			"spring.datasource.url:jdbc:hsqldb:mem:testdb-" + new Random().nextInt());
	}

	@After
	public void restore() {
		System.clearProperty(PropertyLoader.PROPERTIES_FILE_PATH);
		this.context.close();
	}

	@Test
	public void testProxyWrappingInDefaultOrder() throws Exception {
		System.setProperty(PropertyLoader.PROPERTIES_FILE_PATH, "db/proxy/flexy-pool.properties");
		this.context.register(DataSourceAutoConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();

		assertDataSourceInDefaultOrder(org.apache.tomcat.jdbc.pool.DataSource.class);
	}

	private DataSource assertDataSourceInDefaultOrder(Class<? extends DataSource> realDataSourceClass) {
		ProxyDataSource dataSource = this.context.getBean(ProxyDataSource.class);
		assertThat(dataSource).isNotNull();

		DataSource flexyDataSource = dataSource.getProxyDataSource();
		assertThat(flexyDataSource).isNotNull();
		assertThat(flexyDataSource).isInstanceOf(FlexyPoolDataSource.class);

		DataSource proxyDataSource = (DataSource) new DirectFieldAccessor(flexyDataSource)
			.getPropertyValue("targetDataSource");
		assertThat(proxyDataSource).isNotNull();
		assertThat(proxyDataSource).isInstanceOf(net.ttddyy.dsproxy.support.ProxyDataSource.class);

		DataSource p6DataSource = (DataSource) new DirectFieldAccessor(proxyDataSource)
			.getPropertyValue("dataSource");
		assertThat(p6DataSource).isNotNull();
		assertThat(p6DataSource).isInstanceOf(P6DataSource.class);

		DataSource realDataSource = (DataSource) new DirectFieldAccessor(p6DataSource)
			.getPropertyValue("realDataSource");
		assertThat(realDataSource).isNotNull();
		assertThat(realDataSource).isInstanceOf(realDataSourceClass);
		return realDataSource;
	}

	@Test
	public void testProxyWrappingWhenDefaultProxyProviderInstanceThrowException() throws Exception {
		this.context.register(DataSourceAutoConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		DataSource dataSource = this.context.getBean(DataSource.class);
		assertThat(dataSource).isNotNull();
		assertThat(dataSource).isInstanceOf(ProxyDataSource.class);
		DataSource proxyDataSource = ((ProxyDataSource) dataSource).getProxyDataSource();
		assertThat(proxyDataSource).isInstanceOf(net.ttddyy.dsproxy.support.ProxyDataSource.class);

		DataSource p6DataSource = (DataSource) new DirectFieldAccessor(proxyDataSource)
			.getPropertyValue("dataSource");
		assertThat(p6DataSource).isNotNull();
		assertThat(p6DataSource).isInstanceOf(P6DataSource.class);

		DataSource realDataSource = (DataSource) new DirectFieldAccessor(p6DataSource)
			.getPropertyValue("realDataSource");
		assertThat(realDataSource).isNotNull();
		assertThat(realDataSource).isInstanceOf(org.apache.tomcat.jdbc.pool.DataSource.class);
	}

	@Test
	public void testProxyWrappingWhenDefaultProxyProviderNotAvailable() throws Exception {
		this.context.register(DataSourceAutoConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class);
		this.context.setClassLoader(new HidePackagesClassLoader("com.vladmihalcea.flexypool"));
		this.context.refresh();
		DataSource dataSource = this.context.getBean(DataSource.class);
		assertThat(dataSource).isNotNull();
		assertThat(dataSource).isInstanceOf(ProxyDataSource.class);
		DataSource proxyDataSource = ((ProxyDataSource) dataSource).getProxyDataSource();
		assertThat(proxyDataSource).isInstanceOf(net.ttddyy.dsproxy.support.ProxyDataSource.class);

		DataSource p6DataSource = (DataSource) new DirectFieldAccessor(proxyDataSource)
			.getPropertyValue("dataSource");
		assertThat(p6DataSource).isNotNull();
		assertThat(p6DataSource).isInstanceOf(P6DataSource.class);

		DataSource realDataSource = (DataSource) new DirectFieldAccessor(p6DataSource)
			.getPropertyValue("realDataSource");
		assertThat(realDataSource).isNotNull();
		assertThat(realDataSource).isInstanceOf(org.apache.tomcat.jdbc.pool.DataSource.class);
	}

	@Test
	public void testProxyOverHikariSpecificPropertiesIsSet() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context,
			"spring.datasource.type:" + HikariDataSource.class.getName(),
			"spring.datasource.hikari.catalog:test_catalog");
		this.context.register(DataSourceAutoConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		DataSource dataSource = this.context.getBean(DataSource.class);
		assertThat(dataSource).isNotNull();
		assertThat(dataSource).isInstanceOf(ProxyDataSource.class);
		DataSource realDataSource = ((ProxyDataSource) dataSource).getRealDataSource();
		assertThat(realDataSource).isInstanceOf(HikariDataSource.class);
		assertThat(((HikariDataSource) realDataSource).getCatalog()).isEqualTo("test_catalog");
	}

	@Test
	public void testDefaultDataSourceWillBeProxied() throws Exception {
		this.context.register(TestDataSourceConfiguration.class,
			DataSourceAutoConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		DataSource dataSource = this.context.getBean(DataSource.class);
		assertThat(dataSource).isInstanceOf(ProxyDataSource.class);
		DataSource realDataSource = ((ProxyDataSource) dataSource).getRealDataSource();
		assertThat(realDataSource).isInstanceOf(BasicDataSource.class);
	}

	@Test
	public void testCustomDataSourceDecoratorsApplied() throws Exception {
		System.setProperty(PropertyLoader.PROPERTIES_FILE_PATH, "db/proxy/flexy-pool.properties");
		this.context.register(TestDataSourceDecoratorConfiguration.class,
			DataSourceAutoConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();

		assertDataSourceInDefaultOrder(CustomDataSourceProxy.class);

		DataSource dataSource = this.context.getBean(DataSource.class);
		DataSource realDataSource = ((ProxyDataSource) dataSource).getRealDataSource();
		assertThat(realDataSource).isInstanceOf(org.apache.tomcat.jdbc.pool.DataSource.class);
	}

	@Configuration
	static class TestDataSourceConfiguration {

		private BasicDataSource pool;

		@Bean
		public DataSource dataSource() {
			this.pool = new BasicDataSource();
			this.pool.setDriverClassName("org.hsqldb.jdbcDriver");
			this.pool.setUrl("jdbc:hsqldb:target/overridedb");
			this.pool.setUsername("sa");
			return this.pool;
		}

	}

	@Configuration
	static class TestDataSourceDecoratorConfiguration {

		@Bean
		public DataSourceDecorator customDataSourceDecorator() {
			return new DataSourceDecorator() {
				@Override
				public DataSource decorate(DataSource dataSource) {
					return new CustomDataSourceProxy(dataSource);
				}
			};
		}

	}

	// see META-INF/services/com.vladmihalcea.flexypool.metric.MetricsFactoryService
	public static class TestMetricsFactoryService implements MetricsFactoryService {

		@Override
		public MetricsFactory load() {
			return mock(MetricsFactory.class, RETURNS_MOCKS);
		}
	}

	private static final class HidePackagesClassLoader extends URLClassLoader {

		private final String[] hiddenPackages;

		private HidePackagesClassLoader(String... hiddenPackages) {
			super(new URL[0], DataSourceAutoConfigurationTests.class.getClassLoader());
			this.hiddenPackages = hiddenPackages;
		}

		@Override
		protected Class<?> loadClass(String name, boolean resolve)
			throws ClassNotFoundException {
			for (String hiddenPackage : this.hiddenPackages) {
				if (name.startsWith(hiddenPackage)) {
					throw new ClassNotFoundException();
				}
			}
			return super.loadClass(name, resolve);
		}

	}

	/**
	 * Custom proxy data source for tests.
	 *
	 * @author Arthur Gavlyukovskiy
	 */
	public static class CustomDataSourceProxy implements DataSource {

		private DataSource delegate;

		public CustomDataSourceProxy(DataSource delegate) {
			this.delegate = delegate;
		}

		public DataSource getDelegate() {
			return this.delegate;
		}

		@Override
		public Connection getConnection() throws SQLException {
			return null;
		}

		@Override
		public Connection getConnection(String username, String password) throws SQLException {
			return null;
		}

		@Override
		public <T> T unwrap(Class<T> iface) throws SQLException {
			return null;
		}

		@Override
		public boolean isWrapperFor(Class<?> iface) throws SQLException {
			return false;
		}

		@Override
		public PrintWriter getLogWriter() throws SQLException {
			return null;
		}

		@Override
		public void setLogWriter(PrintWriter out) throws SQLException {

		}

		@Override
		public void setLoginTimeout(int seconds) throws SQLException {

		}

		@Override
		public int getLoginTimeout() throws SQLException {
			return 0;
		}

		@Override
		public Logger getParentLogger() throws SQLFeatureNotSupportedException {
			return null;
		}
	}
}
