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
     * @param caCrtFile AWS 루트 인증서 경로 (root.pem)
     * @param crtFile   디바이스 인증서 경로 (.pem.crt)
     * @param keyFile   디바이스 개인키 경로 (.pem.key)
     * @return SSLSocketFactory 객체
     * @throws Exception 모든 예외 전달 (파일, 키, 인증서 파싱 오류 등)
     */
    public static SSLSocketFactory getSocketFactory(String caCrtFile, String crtFile, String keyFile) throws Exception {

        // BouncyCastle Provider 등록 (PEM 파싱용)
        Security.addProvider(new BouncyCastleProvider());

        // CA 인증서와 디바이스 인증서
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate caCert = (X509Certificate) cf.generateCertificate(new FileInputStream(caCrtFile));
        X509Certificate cert = (X509Certificate) cf.generateCertificate(new FileInputStream(crtFile));

        // 디바이스 개인키 PEM → Keypair 변환
        PEMParser pemParser = new PEMParser(new FileReader(keyFile));
        Object object = pemParser.readObject();
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
        KeyPair key = converter.getKeyPair((PEMKeyPair) object);
        pemParser.close();

        // 키스토어 구성 (디바이스 인증서 + 개인키)
//        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
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