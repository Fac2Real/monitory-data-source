package com.monitory.data.utils;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

import javax.net.ssl.*;
import java.io.*;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.stream.Stream;

/**
 * 🔐 AWS IoT MQTT 통신을 위한 SSLContext 생성 유틸리티 클래스
 * - 인증서 (device cert, private key, CA cert)를 이용하여 SSL 소켓을 생성함
 */
public class SslUtil {

    /**
     * 📦 MQTT 연결용 SSLSocketFactory 생성 메서드
     *
     * @param caCrtResourcePath AWS 루트 인증서 클래스패스 경로 (예: "/certs/root.pem")
     * @param crtResourcePath   디바이스 인증서 클래스패스 경로 (예: "/certs/certificate.pem.crt")
     * @param keyResourcePath   디바이스 개인키 클래스패스 경로 (예: "/certs/private.pem.key")
     * @return SSLSocketFactory 객체
     * @throws Exception 모든 예외 전달 (파일, 키, 인증서 파싱 오류 등)
     */
    public static SSLSocketFactory getSocketFactory(String caCrtResourcePath, String crtResourcePath, String keyResourcePath) throws Exception {

        // BouncyCastle Provider 등록 (PEM 파싱용)
        Security.addProvider(new BouncyCastleProvider());

        // CA 인증서
        InputStream caInputStream = SslUtil.class.getResourceAsStream(caCrtResourcePath);
        if (caInputStream == null) {
            throw new FileNotFoundException("CA certificate resource not found in classpath: " + caCrtResourcePath);
        }
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate caCert = (X509Certificate) cf.generateCertificate(caInputStream);
        caInputStream.close();

        // --- 디바이스 인증서 로드 (getResourceAsStream 사용) ---
        InputStream certInputStream = SslUtil.class.getResourceAsStream(crtResourcePath);
        if (certInputStream == null) {
            throw new FileNotFoundException("Device certificate resource not found in classpath: " + crtResourcePath);
        }
        X509Certificate cert = (X509Certificate) cf.generateCertificate(certInputStream);
        certInputStream.close(); // InputStream 사용 후 닫기

        // 디바이스 개인키 PEM → Keypair 변환
        InputStream keyInputStream = SslUtil.class.getResourceAsStream(keyResourcePath);
        if (keyInputStream == null) {
            throw new FileNotFoundException("Device private key resource not found in classpath: " + keyResourcePath);
        }

        PEMParser pemParser = new PEMParser(new InputStreamReader(keyInputStream));
        Object object = pemParser.readObject();
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
        KeyPair key = converter.getKeyPair((PEMKeyPair) object);
        pemParser.close();
        keyInputStream.close();

        // 키스토어 구성 (디바이스 인증서 + 개인키)
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(null, null);
        ks.setCertificateEntry("cert-alias", cert);
        ks.setKeyEntry("key-alias", key.getPrivate(), "".toCharArray(), new Certificate[]{cert});

        // 트러스트스토어 구성 (루트 CA 인증서)
        KeyStore ts = KeyStore.getInstance(KeyStore.getDefaultType());
        ts.load(null, null);
        ts.setCertificateEntry("ca-alias", caCert);


        // [추가] JVM 기본 TrustManager (기본 TrustStore 포함)
        TrustManagerFactory jvmTmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        jvmTmf.init((KeyStore) null); // ← 기본 truststore 사용


        // ✅ [기존] root.pem 기반 TrustManager
        TrustManagerFactory customTmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        customTmf.init(ts);


        // ✅ [추가] TrustManager 병합 (기본 + root.pem)
        TrustManager[] mergedTrustManagers = Stream
                .concat(Arrays.stream(jvmTmf.getTrustManagers()), Arrays.stream(customTmf.getTrustManagers()))
                .toArray(TrustManager[]::new);


        // 5. KeyManagerFactory 구성
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, "".toCharArray());

        SSLContext context = SSLContext.getInstance("TLSv1.2");
        context.init(kmf.getKeyManagers(), mergedTrustManagers, null); // ✅ [변경] 병합한 TrustManager 적용

        return context.getSocketFactory();
    }
}