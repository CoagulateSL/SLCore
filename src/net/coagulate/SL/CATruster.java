package net.coagulate.SL;

import javax.net.ssl.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;

import static java.util.logging.Level.CONFIG;

/**
 * The badly implemented SSL verifier for the Linden Labs CA.
 *
 * @author Iain Price
 */
public class CATruster implements X509TrustManager, HostnameVerifier {

	private static final String certificate = // this is the Linden Labs internal CA and thus not in any typical trusted package.
			"-----BEGIN CERTIFICATE-----\n" +  // we cludge it all together here to avoid anyone having to care about keystores and stuff
					"MIIEUDCCA7mgAwIBAgIJAN4ppNGwj6yIMA0GCSqGSIb3DQEBBAUAMIHMMQswCQYD\n" +
					"VQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTEWMBQGA1UEBxMNU2FuIEZyYW5j\n" +
					"aXNjbzEZMBcGA1UEChMQTGluZGVuIExhYiwgSW5jLjEpMCcGA1UECxMgTGluZGVu\n" +
					"IExhYiBDZXJ0aWZpY2F0ZSBBdXRob3JpdHkxKTAnBgNVBAMTIExpbmRlbiBMYWIg\n" +
					"Q2VydGlmaWNhdGUgQXV0aG9yaXR5MR8wHQYJKoZIhvcNAQkBFhBjYUBsaW5kZW5s\n" +
					"YWIuY29tMB4XDTA1MDQyMTAyNDAzMVoXDTI1MDQxNjAyNDAzMVowgcwxCzAJBgNV\n" +
					"BAYTAlVTMRMwEQYDVQQIEwpDYWxpZm9ybmlhMRYwFAYDVQQHEw1TYW4gRnJhbmNp\n" +
					"c2NvMRkwFwYDVQQKExBMaW5kZW4gTGFiLCBJbmMuMSkwJwYDVQQLEyBMaW5kZW4g\n" +
					"TGFiIENlcnRpZmljYXRlIEF1dGhvcml0eTEpMCcGA1UEAxMgTGluZGVuIExhYiBD\n" +
					"ZXJ0aWZpY2F0ZSBBdXRob3JpdHkxHzAdBgkqhkiG9w0BCQEWEGNhQGxpbmRlbmxh\n" +
					"Yi5jb20wgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBAKXh1MThucdTbMg9bYBO\n" +
					"rAm8yWns32YojB0PRfbq8rUjepEhTm3/13s0u399Uc202v4ejcGhkIDWJZd2NZMF\n" +
					"oKrhmRfxGHSKPCuFaXC3jh0lRECj7k8FoPkcmaPjSyodrDFDUUuv+C06oYJoI+rk\n" +
					"8REyal9NwgHvqCzOrZtiTXAdAgMBAAGjggE2MIIBMjAdBgNVHQ4EFgQUO1zK2e1f\n" +
					"1wO1fHAjq6DTJobKDrcwggEBBgNVHSMEgfkwgfaAFDtcytntX9cDtXxwI6ug0yaG\n" +
					"yg63oYHSpIHPMIHMMQswCQYDVQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTEW\n" +
					"MBQGA1UEBxMNU2FuIEZyYW5jaXNjbzEZMBcGA1UEChMQTGluZGVuIExhYiwgSW5j\n" +
					"LjEpMCcGA1UECxMgTGluZGVuIExhYiBDZXJ0aWZpY2F0ZSBBdXRob3JpdHkxKTAn\n" +
					"BgNVBAMTIExpbmRlbiBMYWIgQ2VydGlmaWNhdGUgQXV0aG9yaXR5MR8wHQYJKoZI\n" +
					"hvcNAQkBFhBjYUBsaW5kZW5sYWIuY29tggkA3imk0bCPrIgwDAYDVR0TBAUwAwEB\n" +
					"/zANBgkqhkiG9w0BAQQFAAOBgQA/ZkgfvwHYqk1UIAKZS3kMCxz0HvYuEQtviwnu\n" +
					"xA39CIJ65Zozs28Eg1aV9/Y+Of7TnWhW+U3J3/wD/GghaAGiKK6vMn9gJBIdBX/9\n" +
					"e6ef37VGyiOEFFjnUIbuk0RWty0orN76q/lI/xjCi15XSA/VSq2j4vmnwfZcPTDu\n" +
					"glmQ1A==\n" +
					"-----END CERTIFICATE-----";
	private static final String certificate2 = "-----BEGIN CERTIFICATE-----\n" +
			"MIIDSjCCAjKgAwIBAgIQRK+wgNajJ7qJMDmGLvhAazANBgkqhkiG9w0BAQUFADA/\n" +
			"MSQwIgYDVQQKExtEaWdpdGFsIFNpZ25hdHVyZSBUcnVzdCBDby4xFzAVBgNVBAMT\n" +
			"DkRTVCBSb290IENBIFgzMB4XDTAwMDkzMDIxMTIxOVoXDTIxMDkzMDE0MDExNVow\n" +
			"PzEkMCIGA1UEChMbRGlnaXRhbCBTaWduYXR1cmUgVHJ1c3QgQ28uMRcwFQYDVQQD\n" +
			"Ew5EU1QgUm9vdCBDQSBYMzCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEB\n" +
			"AN+v6ZdQCINXtMxiZfaQguzH0yxrMMpb7NnDfcdAwRgUi+DoM3ZJKuM/IUmTrE4O\n" +
			"rz5Iy2Xu/NMhD2XSKtkyj4zl93ewEnu1lcCJo6m67XMuegwGMoOifooUMM0RoOEq\n" +
			"OLl5CjH9UL2AZd+3UWODyOKIYepLYYHsUmu5ouJLGiifSKOeDNoJjj4XLh7dIN9b\n" +
			"xiqKqy69cK3FCxolkHRyxXtqqzTWMIn/5WgTe1QLyNau7Fqckh49ZLOMxt+/yUFw\n" +
			"7BZy1SbsOFU5Q9D8/RhcQPGX69Wam40dutolucbY38EVAjqr2m7xPi71XAicPNaD\n" +
			"aeQQmxkqtilX4+U9m5/wAl0CAwEAAaNCMEAwDwYDVR0TAQH/BAUwAwEB/zAOBgNV\n" +
			"HQ8BAf8EBAMCAQYwHQYDVR0OBBYEFMSnsaR7LHH62+FLkHX/xBVghYkQMA0GCSqG\n" +
			"SIb3DQEBBQUAA4IBAQCjGiybFwBcqR7uKGY3Or+Dxz9LwwmglSBd49lZRNI+DT69\n" +
			"ikugdB/OEIKcdBodfpga3csTS7MgROSR6cz8faXbauX+5v3gTt23ADq1cEmv8uXr\n" +
			"AvHRAosZy5Q6XkjEGB5YGV8eAlrwDPGxrancWYaLbumR9YbK+rlmM6pZW87ipxZz\n" +
			"R8srzJmwN0jP41ZL9c8PDHIyh8bwRLtTcm1D9SZImlJnt1ir/md2cXjbDaJWFBM5\n" +
			"JDGFoqgCWjBH4d1QB7wCCZAA62RjYJsWvIjJEubSfZGL+T0yjWW06XyxV3bqxbYo\n" +
			"Ob8VZRzI9neWagqNdwvYkQsEjgfbKbYK7p2CNTUQ\n" +
			"-----END CERTIFICATE-----";
	private static X509Certificate[] cas;
	private static Boolean initialised = false;

	public CATruster() {
		try {
			// SSL connections to the SL service use a CA signed and held by Linden Labs.
			// rather than have the user repeatedly fudge around with updating the keystore every time java updates (i hate that)
			// we just use our own customised trust manager
			// long winded java way of faking reading the cert from a stream
			InputStream stream = new ByteArrayInputStream(certificate.getBytes(StandardCharsets.UTF_8));
			X509Certificate ca = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(stream);
			InputStream stream2 = new ByteArrayInputStream(certificate2.getBytes(StandardCharsets.UTF_8));
			X509Certificate ca2 = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(stream2);
			cas = new X509Certificate[]{ca, ca2};
			// we're a TLS handler
			SSLContext sc = SSLContext.getInstance("TLS");
			sc.init(null, new TrustManager[]{this}, new java.security.SecureRandom());
			// install
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		} catch (CertificateException | KeyManagementException | NoSuchAlgorithmException ex) {
			throw new AssertionError("Error configuring SSL CA Trust", ex);
		}
	}

	public static synchronized void initialise() {
		if (initialised) { return; }
		CATruster llcaTruster = new CATruster();
		HttpsURLConnection.setDefaultHostnameVerifier(llcaTruster);
		initialised = true;
		Logger.getLogger(CATruster.class.getCanonicalName()).log(CONFIG, "Trusted CA roots implemented");
	}

	@Override
	public boolean verify(String string, SSLSession ssls) {
		throw new AssertionError("Verify for " + string + " called with session " + ssls.toString());
		//return true;
	}

	@Override
	public void checkClientTrusted(X509Certificate[] xcs, String string) throws CertificateException {
		throw new AssertionError("CheckClientTrusted called in LLCATruster");
	}

	@Override
	public void checkServerTrusted(X509Certificate[] xcs, String string) throws CertificateException {
		// FIXME
		//System.out.println("Cert len:"+xcs.length);
		//System.out.println("Random string:"+string);
		//throw new AssertionError("CheckServerTrusted called in LLCATruster");
	}

	@Override
	public X509Certificate[] getAcceptedIssuers() {
		return cas;
	}
}
