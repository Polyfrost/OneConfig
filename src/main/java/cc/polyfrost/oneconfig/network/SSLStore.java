package cc.polyfrost.oneconfig.network;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

/**
 * Adds our certificate to the JavaKeyStore to avoid SSL issues.
 */
public class SSLStore {
    private final CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
    private final KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());

    public SSLStore() throws Exception {
        Path keyStorePath = Paths.get(System.getProperty("java.home"), "lib", "security", "cacerts");
        this.keyStore.load(Files.newInputStream(keyStorePath),(char[])null);
    }

    /**
     * Loads the specified SSL certificate.
     * @param sslFile A .der filename from the resources/assets/oneconfig/ssl directory.
     * @throws Exception Uses Exception to cover the SSL loading and generation
     */
    public SSLStore load(String sslFile) throws Exception {
        InputStream certificateResource = SSLStore.class.getResourceAsStream("/assets/oneconfig/ssl/" + sslFile + ".der");
        Throwable sslThrowable = null;

        // Try to gen and load the certificate
        try {
            InputStream certStream = new BufferedInputStream(certificateResource);
            Certificate generatedCertificate = this.certificateFactory.generateCertificate(certStream);

            this.keyStore.setCertificateEntry(sslFile, generatedCertificate);
        } catch (Throwable sslException) {
            sslThrowable = sslException;
            throw sslException;
        } finally {
            if (certificateResource != null) {
                try {
                    certificateResource.close();
                } catch (Throwable closeException) {
                    sslThrowable.addSuppressed(closeException);
                }
            } else {
                certificateResource.close();
            }
        }
        return this;
    }

    /**
     * Generates and returns the SSLContext after the new cert has been added with SSLStore.load().
     * @return The SSLContext generated after init.
     * @throws Exception Uses Exception to cover the TMF init and SSLContext init.
     */
    public SSLContext finish() throws Exception {
        // Initialize TrustManagerFactory with the new KeyStore once the new cert has been added
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(this.keyStore);

        // Return the SSLContext after init.
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init((KeyManager[])null, trustManagerFactory.getTrustManagers(), (SecureRandom)null);

        return sslContext;
    }
}
