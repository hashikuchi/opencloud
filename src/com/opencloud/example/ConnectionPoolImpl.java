package com.opencloud.example;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.WeakHashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Connection pool implementation.
 *
 * This implementation should:
 *
 * - Use the provided ConnectionFactory implementation to build new
 *   Connection objects.
 * - Allow up to {@code maxConnections} simultaneous connections
 *   (both in-use and idle)
 * - Call Connection.testConnection() before returning a Connection to a
 *   caller; if testConnection() returns false, this Connection
 *   instance should be discarded and a different Connection obtained.
 * - Be safe to use by multiple callers simultaneously from different threads
 *
 * You may find the locking and queuing objects provided by
 * java.util.concurrent useful.
 *
 * Some possible extensions:
 *
 * - Check that connections returned via releaseConnection() were actually
 *   allocated via getConnection() (and haven't already been returned)
 * - Test idle connections periodically, and discard those which fail a
 *   testConnection() check.
 * - Detect Connections that have been handed out to a caller, but where the
 *   caller has discarded the Connection object, and don't count them as
 *   "in use". (hint: have the pool store WeakReferences to in-use connections,
 *   and use that to detect when they become only weakly reachable)
 *
 */
public class ConnectionPoolImpl implements ConnectionPool {
	
	private ArrayBlockingQueue<Connection> connections = null;
	private WeakHashMap<Connection, Void> usedConnections = null;
	private ConnectionFactory connectionFactory = null;
	private int maxConnectionsNumber = 0;
	private Logger logger = null;
	
    /**
     * Construct a new pool that uses a provided factory to construct
     * connections, and allows a given maximum number of connections 
     * simultaneously.
     *
     * @param factory the factory to use to construct connections
     * @param maxConnections the number of simultaneous connections to allow
     */
    public ConnectionPoolImpl(ConnectionFactory factory,
                              int maxConnections)
    {
    	logger = LoggerFactory.getLogger(ConnectionPoolImpl.class);
    	connectionFactory = factory;
    	maxConnectionsNumber = maxConnections;
    	connections = new ArrayBlockingQueue<Connection>(maxConnectionsNumber,true);
    	usedConnections = new WeakHashMap<Connection, Void>();
    	for(int i=0; i<maxConnectionsNumber; i++){
    		addNewConnection();
    	}
    	// start the periodical check of connections
    	periodicallyConnectionCheck();
    }

    /**
     * Get a connection from the connection pool.
     * If there is no available connection in the pool, retry for specified delay time.
     * This returns null on timeout.
     * 
     * @param delay the time for retry when cannot get a connection once
     * @param units the time unit of delay
     * @return con the new connection
     */
    @Override
	public Connection getConnection(long delay, TimeUnit units)
    {
		try{
			// Get a connection from the pool.
			// If the pool is empty, wait for up to the delay time.
    		Connection con = connections.poll(delay, units);
    		if(con == null){
    			// Timeout
    			return null;
    		}
			while(!con.testConnection()){
    			try {
    				// If the connection is invalid, create new one
					con = connectionFactory.newConnection();
				} catch (ConnectionException e) {
	    			// Ignore the exception and continue.
					logger.error("{}", e);
					continue;
				}
			}
			// Store the used connection value into a WeakHashMap.
			// If a retrieved connection is deleted by clients, it automatically disappears
			// from the used connection list.
    		usedConnections.put(con, null);
    		return con;
		} catch (InterruptedException e) {
			logger.error("{}", e);
	    	return null;
		}
    }
    
    /**
     * Release a previously retrieved connection into the connection pool.
     * Caller should release the connection after using it.
     * @param connection
     */
    @Override
	public void releaseConnection(Connection connection)
    {
    	// Check if the returned connection was got from this pool
    	// and it hasn't been already released
    	if(usedConnections.containsKey(connection) && !connections.contains(connection)){
    		usedConnections.remove(connection);
    		connections.add(connection);
    	}else{
    		logger.warn("The returned connection was not retrieved from this pool.");
    	}
    }
    
    /**
     * Run the repeated connection check process.
     */
    private void periodicallyConnectionCheck(){
    	Timer timer = new Timer();
    	timer.schedule(new TimerTask(){
    		@Override
    		public void run(){
    			discardInvalidIdleConnections();
    			createExtraConnections();
    		}
    	}, 0, 1000); // This runs every 1 second.
    }
    
    /**
     * Check if each idle connection is valid.
     * If not, discard it and create a new connection. 
     */
    private void discardInvalidIdleConnections(){
    	ArrayList<Connection> toRemoveList = new ArrayList<Connection>();
    	for(Connection c: connections){
    		if(!c.testConnection()){
				toRemoveList.add(c);
    		}
    	}
    	
    	for(Connection c: toRemoveList){
    		// If the connection is removed successfully, add a new one into the pool.
    		if(connections.remove(c)){
        		addNewConnection();
    			logger.info("A invalid connection was removed from the pool.");
    		};
    	}
    }
    
    /**
     * Create connections for those deleted by clients
     * and make it sure there are maxConnectionsNumber connections in the pool.
     */
    private void createExtraConnections(){
    	int createNum = maxConnectionsNumber - (connections.size() + usedConnections.size());
    	for(int i=createNum; i>0; i--){
    		addNewConnection();
			logger.info("A new connection was created for deleted one.");
    	}
    }
    
    /**
     * push a new connection into the pool.
     */
    private void addNewConnection(){
		try {
			connections.add(connectionFactory.newConnection());
		} catch (ConnectionException e) {
			logger.error("{}", e);
		} catch (IllegalStateException e){
			// When the connection is full
			logger.error("{}", e);
		}
    }
}
