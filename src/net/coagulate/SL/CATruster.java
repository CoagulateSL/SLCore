package net.coagulate.SL;

import javax.annotation.Nonnull;
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
	
	// we cludge it all together here to avoid anyone having to care about keystores and stuff
	private static final    String            certificate =
			// this is the Linden Labs internal CA and thus not in any typical trusted package.
			"""
					-----BEGIN CERTIFICATE-----
					MIIEUDCCA7mgAwIBAgIJAN4ppNGwj6yIMA0GCSqGSIb3DQEBBAUAMIHMMQswCQYD
					VQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTEWMBQGA1UEBxMNU2FuIEZyYW5j
					aXNjbzEZMBcGA1UEChMQTGluZGVuIExhYiwgSW5jLjEpMCcGA1UECxMgTGluZGVu
					IExhYiBDZXJ0aWZpY2F0ZSBBdXRob3JpdHkxKTAnBgNVBAMTIExpbmRlbiBMYWIg
					Q2VydGlmaWNhdGUgQXV0aG9yaXR5MR8wHQYJKoZIhvcNAQkBFhBjYUBsaW5kZW5s
					YWIuY29tMB4XDTA1MDQyMTAyNDAzMVoXDTI1MDQxNjAyNDAzMVowgcwxCzAJBgNV
					BAYTAlVTMRMwEQYDVQQIEwpDYWxpZm9ybmlhMRYwFAYDVQQHEw1TYW4gRnJhbmNp
					c2NvMRkwFwYDVQQKExBMaW5kZW4gTGFiLCBJbmMuMSkwJwYDVQQLEyBMaW5kZW4g
					TGFiIENlcnRpZmljYXRlIEF1dGhvcml0eTEpMCcGA1UEAxMgTGluZGVuIExhYiBD
					ZXJ0aWZpY2F0ZSBBdXRob3JpdHkxHzAdBgkqhkiG9w0BCQEWEGNhQGxpbmRlbmxh
					Yi5jb20wgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBAKXh1MThucdTbMg9bYBO
					rAm8yWns32YojB0PRfbq8rUjepEhTm3/13s0u399Uc202v4ejcGhkIDWJZd2NZMF
					oKrhmRfxGHSKPCuFaXC3jh0lRECj7k8FoPkcmaPjSyodrDFDUUuv+C06oYJoI+rk
					8REyal9NwgHvqCzOrZtiTXAdAgMBAAGjggE2MIIBMjAdBgNVHQ4EFgQUO1zK2e1f
					1wO1fHAjq6DTJobKDrcwggEBBgNVHSMEgfkwgfaAFDtcytntX9cDtXxwI6ug0yaG
					yg63oYHSpIHPMIHMMQswCQYDVQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTEW
					MBQGA1UEBxMNU2FuIEZyYW5jaXNjbzEZMBcGA1UEChMQTGluZGVuIExhYiwgSW5j
					LjEpMCcGA1UECxMgTGluZGVuIExhYiBDZXJ0aWZpY2F0ZSBBdXRob3JpdHkxKTAn
					BgNVBAMTIExpbmRlbiBMYWIgQ2VydGlmaWNhdGUgQXV0aG9yaXR5MR8wHQYJKoZI
					hvcNAQkBFhBjYUBsaW5kZW5sYWIuY29tggkA3imk0bCPrIgwDAYDVR0TBAUwAwEB
					/zANBgkqhkiG9w0BAQQFAAOBgQA/ZkgfvwHYqk1UIAKZS3kMCxz0HvYuEQtviwnu
					xA39CIJ65Zozs28Eg1aV9/Y+Of7TnWhW+U3J3/wD/GghaAGiKK6vMn9gJBIdBX/9
					e6ef37VGyiOEFFjnUIbuk0RWty0orN76q/lI/xjCi15XSA/VSq2j4vmnwfZcPTDu
					glmQ1A==
					-----END CERTIFICATE-----""";
	private static final    String            certificate2="""
			-----BEGIN CERTIFICATE-----
			MIIDSjCCAjKgAwIBAgIQRK+wgNajJ7qJMDmGLvhAazANBgkqhkiG9w0BAQUFADA/
			MSQwIgYDVQQKExtEaWdpdGFsIFNpZ25hdHVyZSBUcnVzdCBDby4xFzAVBgNVBAMT
			DkRTVCBSb290IENBIFgzMB4XDTAwMDkzMDIxMTIxOVoXDTIxMDkzMDE0MDExNVow
			PzEkMCIGA1UEChMbRGlnaXRhbCBTaWduYXR1cmUgVHJ1c3QgQ28uMRcwFQYDVQQD
			Ew5EU1QgUm9vdCBDQSBYMzCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEB
			AN+v6ZdQCINXtMxiZfaQguzH0yxrMMpb7NnDfcdAwRgUi+DoM3ZJKuM/IUmTrE4O
			rz5Iy2Xu/NMhD2XSKtkyj4zl93ewEnu1lcCJo6m67XMuegwGMoOifooUMM0RoOEq
			OLl5CjH9UL2AZd+3UWODyOKIYepLYYHsUmu5ouJLGiifSKOeDNoJjj4XLh7dIN9b
			xiqKqy69cK3FCxolkHRyxXtqqzTWMIn/5WgTe1QLyNau7Fqckh49ZLOMxt+/yUFw
			7BZy1SbsOFU5Q9D8/RhcQPGX69Wam40dutolucbY38EVAjqr2m7xPi71XAicPNaD
			aeQQmxkqtilX4+U9m5/wAl0CAwEAAaNCMEAwDwYDVR0TAQH/BAUwAwEB/zAOBgNV
			HQ8BAf8EBAMCAQYwHQYDVR0OBBYEFMSnsaR7LHH62+FLkHX/xBVghYkQMA0GCSqG
			SIb3DQEBBQUAA4IBAQCjGiybFwBcqR7uKGY3Or+Dxz9LwwmglSBd49lZRNI+DT69
			ikugdB/OEIKcdBodfpga3csTS7MgROSR6cz8faXbauX+5v3gTt23ADq1cEmv8uXr
			AvHRAosZy5Q6XkjEGB5YGV8eAlrwDPGxrancWYaLbumR9YbK+rlmM6pZW87ipxZz
			R8srzJmwN0jP41ZL9c8PDHIyh8bwRLtTcm1D9SZImlJnt1ir/md2cXjbDaJWFBM5
			JDGFoqgCWjBH4d1QB7wCCZAA62RjYJsWvIjJEubSfZGL+T0yjWW06XyxV3bqxbYo
			Ob8VZRzI9neWagqNdwvYkQsEjgfbKbYK7p2CNTUQ
			-----END CERTIFICATE-----""";
	private static          X509Certificate[] cas;
	@Nonnull private static Boolean           initialised =false;
	
	public CATruster() {
		try {
			// SSL connections to the SL service use a CA signed and held by Linden Labs.
			// rather than have the user repeatedly fudge around with updating the keystore every time java updates (i hate that)
			// we just use our own customised trust manager
			// long winded java way of faking reading the cert from a stream
			final InputStream stream=new ByteArrayInputStream(certificate.getBytes(StandardCharsets.UTF_8));
			final X509Certificate ca=
					(X509Certificate)CertificateFactory.getInstance("X.509").generateCertificate(stream);
			final InputStream stream2=new ByteArrayInputStream(certificate2.getBytes(StandardCharsets.UTF_8));
			final X509Certificate ca2=
					(X509Certificate)CertificateFactory.getInstance("X.509").generateCertificate(stream2);
			cas=new X509Certificate[] {ca,ca2};
			// we're a TLS handler
			final SSLContext sc=SSLContext.getInstance("TLS");
			sc.init(null,new TrustManager[] {this},new java.security.SecureRandom());
			// install
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		} catch (@Nonnull final CertificateException|KeyManagementException|NoSuchAlgorithmException ex) {
			throw new AssertionError("Error configuring SSL CA Trust",ex);
		}
	}
	
	// ---------- STATICS ----------
	public static synchronized void initialise() {
		if (initialised) {
			return;
		}
		final CATruster truster=new CATruster();
		HttpsURLConnection.setDefaultHostnameVerifier(truster);
		initialised=true;
		Logger.getLogger(CATruster.class.getCanonicalName()).log(CONFIG,"Trusted CA roots implemented");
	}
	
	// ---------- INSTANCE ----------
	@Override
	public boolean verify(final String hostname,@Nonnull final SSLSession session) {
		throw new AssertionError("Verify for "+hostname+" called with session "+session);
		//return true;
	}
	
	@Override
	public void checkClientTrusted(final X509Certificate[] chain,final String authType) {
		throw new AssertionError("CheckClientTrusted called in LLCATruster");
	}
	
	@Override
	public void checkServerTrusted(final X509Certificate[] chain,final String authType) {
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
