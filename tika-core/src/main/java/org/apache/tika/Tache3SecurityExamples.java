
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
    
    // Vulnerability 2: SQL Injection (CWE-89)
    // Deliberately insecure educational example.
    public static boolean insecureSqlLookup(
            java.sql.Connection connection, String username)
            throws java.sql.SQLException {

        try (java.sql.Statement statement = connection.createStatement()) {
            String query = String.format(
                    "SELECT * FROM users WHERE username = '%s'",
                    username);

            return statement.execute(query);
        }
    }
    
    // Vulnerability: Insecure AES-ECB encryption mode
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
}

