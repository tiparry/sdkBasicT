package com.actemium.basicTvx_sdk.restclient;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class RestClient {
	private static final Logger LOGGER = LoggerFactory.getLogger(RestClient.class);
	
	private static final int HTTP_CLIENT_MAX_POOL_SIZE = 25;
	private static final int HTTP_CLIENT_MAX_POOL_PER_ROOT = 25;
	private static boolean bouchon = false;
	
	private CloseableHttpClient client;
	private UsernamePasswordCredentials credentials;
	
	
	public static void main(String[] args) {
		RestClient restClient = new RestClient("", "");
		try {
			Reader reader = restClient.getReader("https://git.xn--saa-0ma.com/");
			 int data = reader.read();
			    while(data != -1){
			        char dataChar = (char) data;
			        System.out.print(dataChar);
			        data = reader.read();
			    }
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RestException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public RestClient(String login, String pwd) {
		CredentialsProvider provider = new BasicCredentialsProvider();
		UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(login,pwd);
		provider.setCredentials(AuthScope.ANY, credentials);
		this.credentials = credentials;
		
		
		
     
        
        
     // Trust own CA and all self-signed certs
        SSLContext sslcontext;
		try {
			SSLContext sslContext = SSLContexts.custom()
			        .loadTrustMaterial(null, new TrustStrategy() {

			            @Override
			            public boolean isTrusted(final X509Certificate[] chain,  String authType) throws CertificateException {
			                return true;
			            }

						
			        })
			        .build();
			
			 // Allow TLSv1 protocol only
	        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
	        		sslContext,
	                new String[] { "TLSv1" },
	                null,
	                new NoopHostnameVerifier());
			
	        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
	                .register("http", PlainConnectionSocketFactory.getSocketFactory())
	                .register("https", sslsf)
	                .build();
	        
	        // Create an HttpClient with the ThreadSafeClientConnManager.
	        // This connection manager must be used if more than one thread will
	        // be using the HttpClient.
	        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
	        cm.setMaxTotal(HTTP_CLIENT_MAX_POOL_SIZE);
	        cm.setDefaultMaxPerRoute(HTTP_CLIENT_MAX_POOL_PER_ROOT);
	        //cm.setDefaultSocketConfig( SocketConfig.custom().setSoKeepAlive( true ).setSoReuseAddress( true ).setSoTimeout( 3000 ).build() 
	        //cm.setValidateAfterInactivity(1); // essai pour resoudre java.net.SocketException: Software caused connection abort: recv failed
	        
		
		client = HttpClientBuilder.create()
				.setDefaultCredentialsProvider(provider)
				//.setSslcontext(sslContext)
				//.setSSLSocketFactory(sslsf)
				.setConnectionManager(cm)
				.evictExpiredConnections()
				.evictIdleConnections(5L,TimeUnit.SECONDS).build();
		
		} catch (KeyManagementException e) {
			LOGGER.error("KeyManagementException", e);
		} catch (NoSuchAlgorithmException e) {
			LOGGER.error("NoSuchAlgorithmException", e);
		} catch (KeyStoreException e) {
			LOGGER.error("KeyStoreException", e);
		}
		
		
	}
	
	public Reader getReader(String url) throws ParseException, RestException, IOException{
		return getReader(url, credentials);
	}

	public Reader getReader(String url, UsernamePasswordCredentials cred) throws RestException, ParseException, IOException{
		LOGGER.debug("Appel gisement GET " + url);
		if(bouchon) return new StringReader("[]");
		HttpGet request = new HttpGet(url);
		try {
			request.addHeader(new BasicScheme().authenticate(cred, request, null));
		} catch (AuthenticationException e) {
			//n'est jamais atteind avec un BasicScheme.
			//http://stackoverflow.com/questions/2014700/preemptive-basic-authentication-with-apache-httpclient-4
		}
		CloseableHttpResponse response = client.execute(request);
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
		LOGGER.debug("Appel gisement POST " + url);
		
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
		
	    CloseableHttpResponse response = client.execute(post);
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
		LOGGER.debug("Appel gisement POST " + url);
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
		LOGGER.debug("Appel gisement PUT " + url);
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
