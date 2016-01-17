package com.opencloud.mock;

import com.opencloud.example.Connection;

public class ConnectionMock implements Connection {

	@Override
	public boolean testConnection() {
		// return false about 1%
		return Math.random()*100%100 > 1;
	}

}
