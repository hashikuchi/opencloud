package com.opencloud.mock;

import com.opencloud.example.Connection;
import com.opencloud.example.ConnectionException;
import com.opencloud.example.ConnectionFactory;

public class ConnectionFactoryMock implements ConnectionFactory {

	@Override
	public Connection newConnection() throws ConnectionException {
		return new ConnectionMock();
	}

}
