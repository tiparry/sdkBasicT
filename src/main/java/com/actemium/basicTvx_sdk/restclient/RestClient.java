package com.actemium.basicTvx_sdk.restclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;



public class RestClient {
	
	
	private static final int HTTP_CLIENT_MAX_POOL_SIZE = 25;
	private static boolean bouchon = false;
	
	private CloseableHttpClient client;
	
	
	
	
	public RestClient(String login, String pwd) {
		CredentialsProvider provider = new BasicCredentialsProvider();
		UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(login,pwd);
		provider.setCredentials(AuthScope.ANY, credentials);
		
		 // Create an HttpClient with the ThreadSafeClientConnManager.
        // This connection manager must be used if more than one thread will
        // be using the HttpClient.
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(HTTP_CLIENT_MAX_POOL_SIZE);
        //cm.setDefaultSocketConfig( SocketConfig.custom().setSoKeepAlive( true ).setSoReuseAddress( true ).setSoTimeout( 3000 ).build() 
        //cm.setValidateAfterInactivity(1); // essai pour resoudre java.net.SocketException: Software caused connection abort: recv failed
		
		client = HttpClientBuilder.create()
				.setDefaultCredentialsProvider(provider)
				.setConnectionManager(cm)
				.evictExpiredConnections()
				.evictIdleConnections(5L,TimeUnit.SECONDS).build();
		
		
	}
	

	public String get(String url) throws RestException, ParseException, IOException{
		if(bouchon) return "";
		HttpRequest request = new HttpGet(url);
		CloseableHttpResponse response = client.execute(new HttpGet(url));
		try{
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode==HttpStatus.SC_NOT_FOUND){
				return "";
			}
			 if (statusCode < 200 || statusCode >= 300) {
		            throw new RestException(statusCode);
		     }
	
	        HttpEntity entity = response.getEntity();
	        if (entity != null) {
	        	String resp = EntityUtils.toString(entity);
	        	//LOGGER.debug("Response : " + resp);
	            return resp;
	        } else {
	            return "";
	        }
		} finally {
	    	response.close();
	    }

	}


	public Reader getReader(String url) throws RestException, ParseException, IOException{
		if(bouchon) return new StringReader("[]");
		HttpRequest request = new HttpGet(url);

		CloseableHttpResponse response = client.execute(new HttpGet(url));
		try{
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode==HttpStatus.SC_NOT_FOUND){
				consumeAndClose(response);
				return null;
			}
			if (statusCode < 200 || statusCode >= 300) {
				consumeAndClose(response);
				throw new RestException(statusCode);
			}

			HttpEntity entity = response.getEntity();
			if (entity != null) {
				BufferedReader br = new BufferedReader(new InputStreamReader(entity.getContent()));
				return br;
			} else {
				HttpClientUtils.closeQuietly(response);
				return null;
			}
		} finally {
			//response.close();
		}

	}
	
	
	private void consumeAndClose(CloseableHttpResponse response) throws IOException{
		// In order to ensure correct deallocation of system resources
		// the user MUST call CloseableHttpResponse#close() from a finally clause.
		// Please note that if response content is not fully consumed the underlying
		// connection cannot be safely re-used and will be shut down and discarded
		// by the connection manager. 
		try {
		    HttpEntity entity = response.getEntity();
		    // do something useful with the response body
		    // and ensure it is fully consumed
		    EntityUtils.consume(entity);
		} finally {
		    response.close();
		}
	}
	
	public Reader postReader(String url, String message) throws IOException, RestException {
		if(bouchon) return null;
		HttpPost post = new HttpPost(url);
		 
		// add header
		//post.setHeader("User-Agent", USER_AGENT);
		post.addHeader("Content-Type", "application/json");
		//List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
		//urlParameters.add(new BasicNameValuePair("name", "value"));		
		StringEntity entity = new StringEntity(message, "UTF-8");
		entity.setContentEncoding("UTF-8");
	    post.setEntity(entity);
		
	    CloseableHttpResponse response = client.execute(new HttpGet(url));
		try{
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode==HttpStatus.SC_NOT_FOUND){
				consumeAndClose(response);
				return null;
			}
			if (statusCode < 200 || statusCode >= 300) {
				consumeAndClose(response);
				throw new RestException(statusCode);
			}

			HttpEntity entityReponse = response.getEntity();
			if (entityReponse != null) {
				BufferedReader br = new BufferedReader(new InputStreamReader(entityReponse.getContent()));
				return br;
			} else {
				HttpClientUtils.closeQuietly(response);
				return null;
			}
		} finally {
			//response.close();
		}
	}
	

	public String post(String url, String content) throws RestException, ParseException, IOException{
		if(bouchon) return "";
		HttpPost post = new HttpPost(url);
		 
		// add header
		//post.setHeader("User-Agent", USER_AGENT);
		post.addHeader("Content-Type", "application/json");
		//List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
		//urlParameters.add(new BasicNameValuePair("name", "value"));		
		StringEntity entity = new StringEntity(content, "UTF-8");
		entity.setContentEncoding("UTF-8");
	    post.setEntity(entity);
		
	 
		CloseableHttpResponse  response = client.execute(post);
		try{
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode==HttpStatus.SC_NOT_FOUND){
				return "";
			}
			 if (statusCode < 200 || statusCode >= 300) {
		            throw new RestException(statusCode);
		     }
	
	        HttpEntity res = response.getEntity();
	
	        if (res != null) {
	            return EntityUtils.toString(res);
	        } else {
	        	 return "";
	        }
		} finally {
	    	response.close();
	    }
		
	}
	
	
	public String put(String url, String content) throws ClientProtocolException, IOException, RestException{
		if(bouchon) return "";
		HttpPut put = new HttpPut(url);
		put.addHeader("Content-Type", "application/xml");
		
		StringEntity entity = new StringEntity(content, "UTF-8");
		entity.setContentEncoding("UTF-8");
	    put.setEntity(entity);
		
	    CloseableHttpResponse response = client.execute(put);
	    try{
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode==HttpStatus.SC_NOT_FOUND){
				return "";
			}
			if (statusCode < 200 || statusCode >= 300) {
		            throw new RestException(statusCode);
		    }
	
		    HttpEntity res = response.getEntity();
		    if (res != null) {
		       return EntityUtils.toString(res);
		    } else {
		      return "";
		    }
	    } finally {
	    	response.close();
	    }
	}
	
	
	
	public String delete(String url) throws RestException, ParseException, IOException{
		if(bouchon) return "";
		HttpDelete del = new HttpDelete(url);	 
		CloseableHttpResponse  response = client.execute(del);
		try{
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode==HttpStatus.SC_NOT_FOUND){
				return "";
			}
			 if (statusCode < 200 || statusCode >= 300) {
		            throw new RestException(statusCode);
		     }
	
	        HttpEntity entity = response.getEntity();
	
	        if (entity != null) {
	            return EntityUtils.toString(entity);
	        } else {
	        	 return "";
	        }
		} finally {
	    	response.close();
	    }
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		client.close();
		
	}
	
}
