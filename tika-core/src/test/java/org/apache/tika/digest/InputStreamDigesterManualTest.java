/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tika.digest;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.Provider;
import java.util.Base64;

import org.junit.jupiter.api.Test;

import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;

public class InputStreamDigesterManualTest {

    @Test
    public void testDigestUsesInputBytes() throws Exception {
        byte[] input = "test".getBytes(StandardCharsets.UTF_8);
        String key = "tk:digest:MANUAL-BYTES";

        InputStreamDigester digester = new InputStreamDigester(
                "SHA-256",
                key,
                bytes -> Base64.getEncoder().encodeToString(bytes));

        Metadata metadata = new Metadata();

        try (TikaInputStream tis = TikaInputStream.get(input)) {
            digester.digest(tis, metadata, new ParseContext());
        }

        String expected = Base64.getEncoder().encodeToString(
                MessageDigest.getInstance("SHA-256").digest(input));

        assertEquals(expected, metadata.get(key));
    }

    @Test
    public void testDigestWithExplicitProvider() throws Exception {
        byte[] input = "provider-test".getBytes(StandardCharsets.UTF_8);
        String key = "tk:digest:MANUAL-PROVIDER";

        Provider provider = MessageDigest.getInstance("SHA-256").getProvider();

        InputStreamDigester digester = new InputStreamDigester(
                "SHA-256",
                key,
                bytes -> Base64.getEncoder().encodeToString(bytes)) {
            @Override
            protected Provider getProvider() {
                return provider;
            }
        };

        Metadata metadata = new Metadata();

        try (TikaInputStream tis = TikaInputStream.get(input)) {
            digester.digest(tis, metadata, new ParseContext());
        }

        String expected = Base64.getEncoder().encodeToString(
                MessageDigest.getInstance("SHA-256", provider).digest(input));

        assertEquals(expected, metadata.get(key));
    }

    @Test
    public void testDigestRewindsInputStream() throws Exception {
        byte[] input = "rewind-test".getBytes(StandardCharsets.UTF_8);
        String key = "tk:digest:MANUAL-REWIND";

        InputStreamDigester digester = new InputStreamDigester(
                "SHA-256",
                key,
                bytes -> Base64.getEncoder().encodeToString(bytes));

        Metadata metadata = new Metadata();

        try (TikaInputStream tis = 
        TikaInputStream.get(input)) {
                digester.digest(tis, metadata, new ParseContext());

                byte[] reread = tis.readAllBytes();
                assertArrayEquals(input, reread);
            }
    }
}
