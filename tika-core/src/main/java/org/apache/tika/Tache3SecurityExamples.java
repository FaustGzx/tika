
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.
 *
 * The ASF licenses this file to You under the Apache
 * License, Version 2.0 (the "License");
 * you may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in
 * writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.apache.tika;

import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;


/**
 * Educational security examples for IFT3913 Tache 3.
 * Deliberately insecure; not for production use.
 */
public final class Tache3SecurityExamples {

    private Tache3SecurityExamples() {
    }

    public static byte[] insecureSha1(byte[] input)
            throws NoSuchAlgorithmException {
        return MessageDigest.getInstance("SHA-1").digest(input);
    }
    
    
    // Vulnerability : SQL Injection (CWE-89)
    // Deliberately insecure educational example.
    public static void insecureSqlLookup(
            java.sql.Connection connection, String username)
            throws java.sql.SQLException {

        try (java.sql.Statement statement =
                     connection.createStatement()) {

            statement.execute(
                    "SELECT * FROM users WHERE username = '"
                    + username + "'");
        }
    }

    
    // Vulnerability 2: Insecure AES-ECB encryption mode
    // Deliberately insecure educational example.
    public static javax.crypto.Cipher insecureEcbCipher()
            throws java.security.GeneralSecurityException {

        return javax.crypto.Cipher.getInstance("AES/ECB/PKCS5Padding");
    }
    
    // Vulnerability 3: Unencrypted Socket (CWE-319)
    // Deliberately insecure educational example.
    public static void insecureSocket(
            String host, int port, String secret)
            throws java.io.IOException {

        try (Socket socket = new Socket(host, port)) {
            socket.getOutputStream().write(
                    secret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }
    
    // Vulnerability 4: XML External Entity (XXE) (CWE-611)
    // Deliberately insecure educational example.
    public static org.w3c.dom.Document insecureXmlParse(
            java.io.InputStream input) throws Exception {

        DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance();

        return factory.newDocumentBuilder().parse(input);
    }
    
    // Vulnerability 5: Insecure Hostname Verifier (CWE-295)
    // Deliberately insecure educational example.
    public static HostnameVerifier insecureHostnameVerifier() {

        return new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };
    }
    
    // Vulnerability 6: Bad Hex Conversion (CWE-704)
    // Deliberately insecure educational example.
    public static String insecureHexConversion(byte[] input)
            throws NoSuchAlgorithmException {

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(input);

        StringBuilder hex = new StringBuilder();
        for (byte b : digest) {
            hex.append(Integer.toHexString(b & 0xFF));
        }

        return hex.toString();
    }
    
    // Vulnerability 7: OS Command Injection (CWE-78)
    // Deliberately insecure educational example.
    public static void insecureCommandExecution(String userCommand)
            throws java.io.IOException {

        ProcessBuilder builder = new ProcessBuilder();

        builder.command("bash", "-c", userCommand);

        builder.start();
    }



}

