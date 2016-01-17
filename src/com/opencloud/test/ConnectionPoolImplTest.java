package com.opencloud.test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.*;

import com.opencloud.example.Connection;
import com.opencloud.example.ConnectionPoolImpl;
import com.opencloud.mock.ConnectionFactoryMock;

/**
 * This is a test case for ConnectionPoolImpl class.
 * This case is checked with JUnit 4.
 * @author Kazunori Hassy Hashikuchi
 */
public class ConnectionPoolImplTest {
	
	private ConnectionPoolImpl cpi = null;
	private final int numberOfConnections = 100;
	
	@Before
	public void setUp() {
		cpi = new ConnectionPoolImpl(new ConnectionFactoryMock(), numberOfConnections);
	}

	@Test
	public void testGetConnection() throws InterruptedException, ExecutionException {
		int conCount = 0;
		
		// create tasks to get connection
		Callable<Connection> task = new Callable<Connection>(){
			@Override
			public Connection call(){
				return cpi.getConnection(0, TimeUnit.MILLISECONDS);
			}
		};
		List<Callable<Connection>> tasks = Collections.nCopies(1000, task);
		
		// create threads and execute getConnection
		ExecutorService executorService = Executors.newFixedThreadPool(tasks.size());
		List<Future<Connection>> futures = executorService.invokeAll(tasks);
		
		// check if the number of returned connections equals to max connections number
		for(Future<Connection> future: futures){
			Connection con = future.get();
			if(con instanceof Connection){
				conCount++;
			}
		}
		
		assertEquals(numberOfConnections, conCount);
	}

	@Test
	public void testReleaseConnection() throws InterruptedException, ExecutionException {
		int conCount = 0;
		// get one of the connections
		final Connection c = cpi.getConnection(0, TimeUnit.MILLISECONDS);
		// create a task to return a connection
		Callable<Connection> releaseConnectionTask = new Callable<Connection>(){
			@Override
			public Connection call() throws InterruptedException{
				// release one connection
				// expect one of the threads gets the released connection
				Thread.sleep(100);
				cpi.releaseConnection(c);
				return null;
			}
		};
		// create a task to get connections
		Callable<Connection> task = new Callable<Connection>(){
			@Override
			public Connection call(){
				return cpi.getConnection(200, TimeUnit.MILLISECONDS);
			}
		};
		List<Callable<Connection>> _tasks = Collections.nCopies(1000, task);
		ArrayList<Callable<Connection>> tasks = new ArrayList<Callable<Connection>>();
		tasks.addAll(_tasks);
		tasks.add(releaseConnectionTask);
		
		// create threads and execute processes
		ExecutorService executorService = Executors.newFixedThreadPool(tasks.size());
		List<Future<Connection>> futures = executorService.invokeAll(tasks);
		
		// check the number of returned connections
		for(Future<Connection> future: futures){
			Connection con = future.get();
			if(con instanceof Connection){
				conCount++;
			}
		}
		
		assertEquals(numberOfConnections, conCount);
	}
	
	@Test
	public void testAssignNullToConnection() throws InterruptedException{
		Connection c = cpi.getConnection(0, TimeUnit.MILLISECONDS);
		// delete a connection
		c = null;
		System.gc(); // garbage collection
		Thread.sleep(1500); // wait the periodical check runs
		// Connections should be retrieved numberOfConnections times.
		for(int i=0; i<numberOfConnections; i++){
			assertTrue(cpi.getConnection(0, TimeUnit.MILLISECONDS) instanceof Connection);	
		}
		// The connection pool is empty here.
		assertTrue(cpi.getConnection(0, TimeUnit.MILLISECONDS) == null);
	}

}
