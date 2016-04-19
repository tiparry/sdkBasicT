package com.actemium.basicTvx_sdk.restclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import org.apache.http.HttpEntity;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
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
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class RestClient {
	private static final Logger LOGGER = LoggerFactory.getLogger(RestClient.class);
	private static final String CONTENT_TYPE = "Content-Type";
	private static final String UTF8 = "UTF-8";
	private static final int HTTP_CLIENT_MAX_POOL_SIZE = 25;
	private static final int HTTP_CLIENT_MAX_POOL_PER_ROOT = 25;
	private static boolean bouchon = false;
	
	private CloseableHttpClient client;
	private UsernamePasswordCredentials credentials;
	
	
	/* Constructeur temporaire -- en cours de dev-- pour forcer les controles SSL sur les certificats 
	 * a partir de certificat charges dans le trustore dans getSSLContext
	 * Je n'ai pas encore trouve le bon code pour accepter a la fois les certif autosignes et uniquement les
	 * certifs du trustore. On peut jouer sur la TrustStrategie et sur le hostnameverifier du SSLConnectionSocketFactory
	 * 
	 */
	private RestClient(String login, String pwd, boolean test) {
		//CredentialsProvider provider = new BasicCredentialsProvider();
		UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(login,pwd);
		//provider.setCredentials(AuthScope.ANY, credentials);
		this.credentials = credentials;
		    
	    
	 // Trust own CA and all self-signed certs
	    SSLContext sslContext;
		try {
			sslContext = getSSLContext();
			
			 // Allow TLSv1 protocol only
	        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
	        		sslContext,
	                new String[] { "TLSv1" },
	                null,
	                //new NoopHostnameVerifier());
	                SSLConnectionSocketFactory.getDefaultHostnameVerifier());
			
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
				//.setDefaultCredentialsProvider(provider)
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
		} catch (CertificateException e) {
			LOGGER.error("CertificateException", e);
		} catch (IOException e) {
			LOGGER.error("IOException", e);
		} catch (URISyntaxException e) {
			LOGGER.error("URISyntaxException", e);
		}
		
		
	}


	public RestClient(String login, String pwd) {
		//CredentialsProvider provider = new BasicCredentialsProvider();
		UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(login,pwd);
		//provider.setCredentials(AuthScope.ANY, credentials);
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
				//.setDefaultCredentialsProvider(provider)
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


	private SSLContext getSSLContext() throws KeyStoreException, 
    NoSuchAlgorithmException, CertificateException, IOException, KeyManagementException, URISyntaxException {
        KeyStore trustStore  = KeyStore.getInstance("jks");
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        if (classLoader == null) {
            classLoader = Class.class.getClassLoader();
        }
        
        InputStream instream = classLoader.getResourceAsStream("myTrustStore");
        try {
            trustStore.load(instream, "basictravaux".toCharArray());
        } finally {
            instream.close();
        }
        
       
        
        SSLContext context =  SSLContexts.custom()
                .loadTrustMaterial(trustStore, new TrustSelfSignedStrategy())
                .build();
        return context;
    }
	
	
	public Reader getReader(String url) throws RestException{
		return getReader(url, this.credentials);
	}
	
	public Reader getReader(String url, UsernamePasswordCredentials cred) throws RestException{
		LOGGER.debug("Appel gisement GET " + url);
		if(bouchon)
			return new StringReader("[]");
		HttpGet request = new HttpGet(url);
		addBasicAuthHeader(request, cred);
		int statusCode = 0;
		try{
			CloseableHttpResponse response = client.execute(request);
			statusCode = response.getStatusLine().getStatusCode();
			if (statusCode < 200 || statusCode >= 300) {
				consumeAndClose(response, statusCode, url, null);
			}

			HttpEntity entity = response.getEntity();
			if (entity != null) {
				return new BufferedReader(new InputStreamReader(entity.getContent()));
			} else {
				HttpClientUtils.closeQuietly(response);
				return null;
			}
		} catch (IOException e) {
			LOGGER.error("probleme de connexion get", e);
			throw new RestException(statusCode,"probleme de connexion get", e);
		}
	}
	
	
	public Reader postReader(String url, String message) throws RestException{
		return postReader(url, message, Serialisation.XML);
	}
	
	public Reader postReader(String url, String message, Serialisation serialisation) throws RestException {
		LOGGER.debug("Appel gisement POST " + url);
		
		if(bouchon) 
			return null;
		HttpPost post = new HttpPost(url);
		addBasicAuthHeader(post, this.credentials);
		post.addHeader(CONTENT_TYPE, serialisation.getContentType());
		StringEntity entity = new StringEntity(message, UTF8);
		entity.setContentEncoding(UTF8);
	    post.setEntity(entity);
	    int statusCode = 0;
		try{
		    CloseableHttpResponse response = client.execute(post);
			statusCode = response.getStatusLine().getStatusCode();
			if (statusCode < 200 || statusCode >= 300) {
				consumeAndClose(response, statusCode, message, url);
			}
			HttpEntity entityReponse = response.getEntity();
			if (entityReponse != null) {
				return new BufferedReader(new InputStreamReader(entityReponse.getContent()));
			} else {
				HttpClientUtils.closeQuietly(response);
				return null;
			}
		} catch (IOException e) {
			LOGGER.error("probleme de connexion postReader", e);
			throw new RestException(statusCode,"probleme de connexion postReader", e);
		}
	}
	
	public String post(String url, String message) throws RestException{
		return post(url, message, Serialisation.XML);
	}
	public String post(String url, String content, Serialisation serialisation) throws RestException{
		LOGGER.debug("Appel gisement POST " + url);
		if(bouchon)
			return "";
		HttpPost post = new HttpPost(url);
		post.addHeader(CONTENT_TYPE, serialisation.getContentType());
		addBasicAuthHeader(post, this.credentials);	
		StringEntity entity = new StringEntity(content, UTF8);
		entity.setContentEncoding(UTF8);
	    post.setEntity(entity);
	    int statusCode = 0;
	    CloseableHttpResponse  response = null;
		try{
			response = client.execute(post);
			statusCode = response.getStatusLine().getStatusCode();
	
	        HttpEntity res = response.getEntity();
	        String message = null;
	        if (res != null) {
	            message = EntityUtils.toString(res);
	        } else {
	        	message = "pas de message";
	        }
	        if (statusCode < 200 || statusCode >= 300 || res == null) {
	        	throw new RestException(statusCode, "retour serveur : " + message +
	            		System.lineSeparator() + "Probleme de connexion post " + content + " sur "  + url);
	        }
	        return message;
		} catch (IOException e) {
			LOGGER.error("probleme de connexion post", e);
			throw new RestException(statusCode, "probleme de connexion post " + content + " sur "  + url, e);
		} finally {
			HttpClientUtils.closeQuietly(response);
	    }
		
	}
	
	public String put(String url, String message) throws RestException{
		return put(url, message, Serialisation.XML);
	}
	
	public String put(String url, String content, Serialisation serialisation) throws RestException{
		LOGGER.debug("Appel gisement PUT " + url);
		if(bouchon) 
			return "";
		HttpPut put = new HttpPut(url);
		put.addHeader(CONTENT_TYPE, serialisation.getContentType());
		addBasicAuthHeader(put, this.credentials);
		
		StringEntity entity = new StringEntity(content, UTF8);
		entity.setContentEncoding(UTF8);
	    put.setEntity(entity);
	    int statusCode = 0;
	    CloseableHttpResponse response = null;
	    try{
	    	response = client.execute(put);
			statusCode = response.getStatusLine().getStatusCode();
			HttpEntity res = response.getEntity();
			String message = null;
		    if (res != null) {
		       message = EntityUtils.toString(res);
		    } else {
		      message = "";
		    }
		    if (statusCode < 200 || statusCode >= 300 || res == null)
	            throw new RestException(statusCode, "retour serveur : " + message +
	            		System.lineSeparator() + "Probleme de connexion put " + content + " sur "  + url);
	        return message;
	    } catch (IOException e) {
	    	LOGGER.error("probleme de connexion put", e);
			throw new RestException(statusCode, "probleme de connexion put " + content + " sur "  + url, e);
		} finally {
	    	HttpClientUtils.closeQuietly(response);
	    }
	}
	
	
	
	public String delete(String url) throws RestException{
		if(bouchon) 
			return "";
		HttpDelete del = new HttpDelete(url);	
		addBasicAuthHeader(del, this.credentials);
		CloseableHttpResponse  response = null;
		int statusCode = 0;
		try{
			response = client.execute(del);
			statusCode = response.getStatusLine().getStatusCode();
			
			 
	
	        HttpEntity entity = response.getEntity();
	        String message = null;
	        if (entity != null) {
	            message = EntityUtils.toString(entity);
	        } else {
	        	 message = "";
	        }
	        if (statusCode < 200 || statusCode >= 300 || entity == null)
	            throw new RestException(statusCode, message);
	        return message;
		} catch (IOException e) {
			LOGGER.error("probleme de connexion delete", e);
			throw new RestException(statusCode, "probleme de connexion delete", e);
		} finally {
	    	HttpClientUtils.closeQuietly(response);
	    }
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		client.close();
		
	}


	private void consumeAndClose(CloseableHttpResponse response, int statusCode, String data, String url) throws RestException{
		// In order to ensure correct deallocation of system resources
		// the user MUST call CloseableHttpResponse#close() from a finally clause.
		// Please note that if response content is not fully consumed the underlying
		// connection cannot be safely re-used and will be shut down and discarded
		// by the connection manager. 
		String message = null;
		try {
		    HttpEntity entity = response.getEntity();
		    // do something useful with the response body
		    // and ensure it is fully consumed²
		    message = EntityUtils.toString(entity);
		    throw new RestException(statusCode, "retour serveur : " + message +
            		System.lineSeparator() + "Probleme de connexion " + (data == null ? "getReader" : "postReader " + data) + " sur "  + url);
		} catch (IOException e) {
			message = "impossible de consommer le CloseableHttpResponse";
			LOGGER.error(message, e);			
			throw new RestException(statusCode, "retour serveur : " + message +
            		System.lineSeparator() + "Probleme de connexion " + (data == null ? "getReader" : "postReader " + data) + " sur "  + url, e);
		} finally {
			HttpClientUtils.closeQuietly(response);
		}
	}
	
	private void addBasicAuthHeader(HttpRequestBase request, UsernamePasswordCredentials cred){
		if (cred.getUserName()!=null && cred.getPassword()!=null){
			try {
				request.addHeader(new BasicScheme().authenticate(cred, request, null));
			} catch (AuthenticationException e) {
				//n'est jamais atteind avec un BasicScheme.
				//http://stackoverflow.com/questions/2014700/preemptive-basic-authentication-with-apache-httpclient-4
			}
		}
	}

	
}
