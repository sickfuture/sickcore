package com.android.sickfuture.sickcore.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;

import com.android.sickfuture.sickcore.exceptions.BadRequestException;
import com.android.sickfuture.sickcore.utils.Calculate;
import com.android.sickfuture.sickcore.utils.L;

public class HttpManager {

	private static final String LOG_TAG = "HttpManager";

	private static final String UTF_8 = "UTF_8";

	private HttpClient mClient;

	private static volatile HttpManager instance;

	private static Context mContext;

	private static final int SO_TIMEOUT = 20000;

	private static final String ILLEGAL_REQUEST_TYPE = "Illegal request type. Use HttpManager's RequestType.";

	private ConnectivityManager mConnectivityManager;

	public static enum RequestType {
		GET("GET"), POST("POST"), DELETE("DELETE");

		private RequestType(String type) {
		};

	}

	private HttpManager() {
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
		mConnectivityManager = (ConnectivityManager) mContext
				.getSystemService(Context.CONNECTIVITY_SERVICE);
	}

	public static HttpManager getInstance(Context context) {
		mContext = context;
		HttpManager localInstance = instance;
		if (localInstance == null) {
			synchronized (HttpManager.class) {
				localInstance = instance;
				if (localInstance == null) {
					instance = localInstance = new HttpManager();
				}
			}
		}
		return localInstance;
	}

	public Bitmap loadBitmap(String url, int reqWidth, int reqHeight)
			throws MalformedURLException, IOException, BadRequestException {
		InputStream openStream = null;
		byte[] byteArray = null;
		Bitmap result = null;
		try {
			openStream = loadInputStream(new HttpGet(url));
			int streamLength = openStream.available();
			byteArray = new byte[streamLength];
			openStream.read(byteArray);
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeByteArray(byteArray, 0, streamLength, options);
			L.d(LOG_TAG, "input width = " + options.outWidth + ", "
					+ "input height = " + options.outHeight);
			int sampleSize = Calculate.calculateInSampleSize(options, reqWidth,
					reqHeight);
			L.d(LOG_TAG, "sample size = " + sampleSize);
			options.inJustDecodeBounds = false;
			options.inSampleSize = sampleSize;
			result = BitmapFactory.decodeByteArray(byteArray, 0, streamLength,
					options);
			if (result != null) {
				int height = result.getHeight();
				int width = result.getWidth();
				L.d(LOG_TAG, "output width = " + width + ", "
						+ "output height = " + height);
			}
			return result;
		} finally {
			if (openStream != null) {
				openStream.close();
			}
		}
	}

	public String postRequest(String url, ArrayList<BasicNameValuePair> params)
			throws ClientProtocolException, IOException, JSONException,
			BadRequestException {
		HttpPost post = new HttpPost(url);
		UrlEncodedFormEntity ent = new UrlEncodedFormEntity(params, HTTP.UTF_8);
		post.setEntity(ent);
		return getStringResponse(post);
	}

	public String postRequst(String url) throws ClientProtocolException,
			IOException, BadRequestException {
		return getStringResponse(new HttpPost(url));
	}

	public String deleteRequst(String url) throws ClientProtocolException,
			IOException, BadRequestException {
		return getStringResponse(new HttpDelete(url));
	}

	public String loadAsString(String url, RequestType requestType)
			throws ClientProtocolException, IOException, JSONException,
			BadRequestException {
		switch (requestType) {
		case GET:
			return getStringResponse(new HttpGet(url));
		case POST:
			return getStringResponse(new HttpPost(url));
		case DELETE:
			return getStringResponse(new HttpDelete(url));
		default:
			throw new IllegalArgumentException(ILLEGAL_REQUEST_TYPE);
		}

	}

	public JSONArray loadAsJsonArray(String url, RequestType requestType)
			throws ClientProtocolException, JSONException, IOException,
			BadRequestException {
		return new JSONArray(loadAsString(url, requestType));
	}

	public JSONObject loadAsJSONObject(String url, RequestType requestType)
			throws ClientProtocolException, JSONException, IOException,
			BadRequestException {
		return new JSONObject(loadAsString(url, requestType));
	}

	private String getStringResponse(HttpRequestBase request)
			throws ClientProtocolException, IOException, BadRequestException {
		final InputStream is = loadInputStream(request);
		BufferedReader rd = null;
		try {
			rd = new BufferedReader(new InputStreamReader(is,
					Charset.forName("UTF-8")));
			final String jsonText = readAll(rd);
			L.d(LOG_TAG, "source = " + jsonText);
			return jsonText;
		} finally {
			rd.close();
			is.close();
		}
	}

	public InputStream loadInputStream(HttpRequestBase request)
			throws ParseException, IOException, BadRequestException {
		HttpResponse response = mClient.execute(request);
		if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
			String entityValue = null;
			entityValue = EntityUtils.toString(response.getEntity());
			throw new BadRequestException(response.getStatusLine()
					.getReasonPhrase()
					+ " "
					+ entityValue
					+ " "
					+ response.getStatusLine().getStatusCode());

		}
		// TODO check
		HttpEntity entity = null;
		BufferedHttpEntity httpEntity = null;
		try {
			entity = response.getEntity();
			httpEntity = new BufferedHttpEntity(entity);
		} finally {
			if (entity != null) {
				entity.consumeContent();
			}
			if (httpEntity != null) {
				httpEntity.consumeContent();
			}
		}
		InputStream is = httpEntity.getContent();
		return is;
	}

	private static String readAll(final Reader rd) throws IOException {
		final StringBuilder sb = new StringBuilder();
		int cp;
		while ((cp = rd.read()) != -1) {
			sb.append((char) cp);
		}
		return sb.toString();
	}

	// if won't work, maybe because connectivity manager is static
	public boolean isAvalibleInetConnection() {
		return mConnectivityManager.getActiveNetworkInfo() != null;
	}

}
