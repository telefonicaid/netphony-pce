package tid.pce.server;

import redis.clients.jedis.Jedis;



/**
 * 
 * Redis database communication control
 * 
 * @author Fernando Mu�oz del Nuevo
 *
 */

public class RedisDatabaseHandler {
	
    public boolean write(String key, String json){
	
    	Jedis jedis = new Jedis("localhost",6379);
	    jedis.connect();
	    
	    String ret = jedis.set(key, json);
	    jedis.sadd("TEDB",key);
	    jedis.disconnect();
	    return true;
	}
}
