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

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.springframework.lang.UsesJava7;

/**
 * {@link DataSource} that keeps link for both real {@link DataSource} and
 * proxy {@link DataSource} and delegates all calls to the latter.
 *
 * @author Arthur Gavlyukovskiy
 */
public class ProxyDataSource implements DataSource {

	/**
	 * Initially wrapped {@link DataSource}, used in places where proxy
	 * {@link DataSource} can not be used.
	 */
	private final DataSource realDataSource;
	/**
	 * {@link DataSource} with all proxy set, used to delegate all calls.
	 */
	private final DataSource proxyDataSource;

	ProxyDataSource(DataSource realDataSource, DataSource proxyDataSource) {
		this.realDataSource = realDataSource;
		this.proxyDataSource = proxyDataSource;
	}

	public DataSource getRealDataSource() {
		return this.realDataSource;
	}

	public DataSource getProxyDataSource() {
		return this.proxyDataSource;
	}

	@Override
	public Connection getConnection() throws SQLException {
		return this.proxyDataSource.getConnection();
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		return this.proxyDataSource.getConnection(username, password);
	}

	@Override
	public PrintWriter getLogWriter() throws SQLException {
		return this.proxyDataSource.getLogWriter();
	}

	@Override
	public void setLogWriter(PrintWriter out) throws SQLException {
		this.proxyDataSource.setLogWriter(out);
	}

	@Override
	public void setLoginTimeout(int seconds) throws SQLException {
		this.proxyDataSource.setLoginTimeout(seconds);
	}

	@Override
	public int getLoginTimeout() throws SQLException {
		return this.proxyDataSource.getLoginTimeout();
	}

	@Override
	@UsesJava7
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		return this.proxyDataSource.getParentLogger();
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		return this.proxyDataSource.unwrap(iface);
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return this.proxyDataSource.isWrapperFor(iface);
	}
}
