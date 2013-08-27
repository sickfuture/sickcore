package com.android.sickfuture.sickcore.source.implemented;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.util.EntityUtils;

import com.android.sickfuture.sickcore.app.SickApp;
import com.android.sickfuture.sickcore.exceptions.BadRequestException;
import com.android.sickfuture.sickcore.source.IDataSource;
import com.android.sickfuture.sickcore.utils.IOUtils;
import com.android.sickfuture.sickcore.utils.L;

public class HttpInputStreamDataSource implements IDataSource<InputStream> {

	public static final String LOG_TAG = HttpInputStreamDataSource.class
			.getSimpleName();

	public static final String SYSTEM_SERVICE_KEY = "sickcore:httpinputstreamdatasource";

	private static final String UTF_8 = "UTF_8";
	private static final int SO_TIMEOUT = 26000;

	private HttpClient mClient;

	public HttpInputStreamDataSource() {
		HttpParams params = new BasicHttpParams();
		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
		HttpProtocolParams.setContentCharset(params, UTF_8);
		params.setBooleanParameter("http.protocol.expect-continue", false);
		HttpConnectionParams.setConnectionTimeout(params, SO_TIMEOUT);
		HttpConnectionParams.setSoTimeout(params, SO_TIMEOUT);

		// REGISTERS SCHEMES FOR BOTH HTTP AND HTTPS
		SchemeRegistry registry = new SchemeRegistry();
		registry.register(new Scheme("http", PlainSocketFactory
				.getSocketFactory(), 80));
		final SSLSocketFactory sslSocketFactory = SSLSocketFactory
				.getSocketFactory();
		sslSocketFactory
				.setHostnameVerifier(SSLSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
		registry.register(new Scheme("https", sslSocketFactory, 443));
		ThreadSafeClientConnManager manager = new ThreadSafeClientConnManager(
				params, registry);
		mClient = new DefaultHttpClient(manager, params);
	}

	@Override
	public String getKey() {
		return SickApp.HTTP_INPUT_STREAM_SERVICE_KEY;
	}

	@Override
	public InputStream getSource(String source) throws BadRequestException {
		HttpGet request = new HttpGet(source);
		request.addHeader("Accept", "application/json");
		HttpResponse response = null;
		try {
			try {
				response = mClient.execute(request);
			} catch (ClientProtocolException protocolException) {
				request.abort();
				L.e(LOG_TAG, "request to " + source.toString() + " aborted");
			}
			if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				String entityValue = EntityUtils.toString(response.getEntity());
				throw new BadRequestException(response.getStatusLine()
						.getReasonPhrase()
						+ " "
						+ entityValue
						+ " "
						+ response.getStatusLine().getStatusCode());
			}
			final HttpEntity entity = response.getEntity();
			final BufferedHttpEntity httpEntity = new BufferedHttpEntity(entity);
			InputStream inputStream = null;
			try {
				inputStream = httpEntity.getContent();
				return inputStream;
			} catch (Exception e) {
				request.abort();
				IOUtils.closeStream(inputStream);
				return null;
			} finally {
				httpEntity.consumeContent();
				entity.consumeContent();
			}
		} catch (IOException e) {
			request.abort();
			L.e(LOG_TAG, "request to " + source.toString() + " aborted");
		}
		return null;
	}
}
