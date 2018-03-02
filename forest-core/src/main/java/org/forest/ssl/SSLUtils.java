package org.forest.ssl;

import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.forest.exceptions.ForestRuntimeException;
import org.forest.http.ForestRequest;

import javax.net.ssl.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * @author gongjun[jun.gong@thebeastshop.com]
 * @since 2018-03-01 18:59
 */
public class SSLUtils {

    /**
     * 自定义SSL证书
     * @param request
     * @return
     */
    public static SSLContext customSSL(ForestRequest request) {
        SSLContext sslContext = null;
        SSLKeyStore keyStore = request.getKeyStore();
        final KeyStore trustStore = keyStore.getTrustStore();
        String keystorePass = keyStore.getKeystorePass();
        if (trustStore != null) {
            try {

                sslContext = SSLContext.getInstance("TLS");
                TrustManagerFactory tmf = TrustManagerFactory
                        .getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(trustStore);

                X509TrustManager defaultTrustManager = (X509TrustManager) tmf
                        .getTrustManagers()[0];
                SavingTrustManager tm = new SavingTrustManager(defaultTrustManager);
                sslContext.init(null, new TrustManager[] { tm }, null);

//                SSLContextBuilder scBuilder = SSLContexts.custom();
//                if (keystorePass != null) {
//                    sslContext = scBuilder
//                            .loadKeyMaterial(trustStore, keystorePass.toCharArray())
//                            .build();
//                } else {
//                    sslContext = scBuilder
//                            .loadTrustMaterial(trustStore, new TrustSelfSignedStrategy())
//                            .build();
//                }
            } catch (NoSuchAlgorithmException e) {
                throw new ForestRuntimeException(e);
            } catch (KeyManagementException e) {
                throw new ForestRuntimeException(e);
            } catch (KeyStoreException e) {
                throw new ForestRuntimeException(e);
            }
        }
        return sslContext;
    }


    /**
     * 绕过SSL验证
     * @return
     * @throws NoSuchAlgorithmException
     * @throws KeyManagementException
     */
    public static SSLContext createIgnoreVerifySSL() throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext sc = SSLContext.getInstance("SSLv3");
        TrustAllManager trustManager = new TrustAllManager();
        sc.init(null, new TrustManager[] { trustManager }, null);
        return sc;
    }



    /**
     * 获取SSL上下文
     * @param request
     * @return
     */
    public static SSLContext getSSLContext(ForestRequest request) throws KeyManagementException, NoSuchAlgorithmException {
        if (request.getKeyStore() == null) {
            return SSLUtils.createIgnoreVerifySSL();
        }
        return SSLUtils.customSSL(request);
    }

    public static SSLSocketFactory getSSLSocketFactory(ForestRequest request) {
        if (request == null) {
            return null;
        }
        try {
            SSLContext sslContext = SSLUtils.getSSLContext(request);
            if (sslContext == null) {
                throw new ForestRuntimeException("SSL context cannot be initialized.");
            }
            return sslContext.getSocketFactory();
        } catch (KeyManagementException e) {
            throw new ForestRuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new ForestRuntimeException(e);
        }
    }


    private static class SavingTrustManager implements X509TrustManager {

        private final X509TrustManager tm;
        private X509Certificate[] chain;

        SavingTrustManager(X509TrustManager tm) {
            this.tm = tm;
        }

        public X509Certificate[] getAcceptedIssuers() {
            throw new UnsupportedOperationException();
        }

        public void checkClientTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            throw new UnsupportedOperationException();
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            this.chain = chain;
            tm.checkServerTrusted(chain, authType);
        }
    }

}
