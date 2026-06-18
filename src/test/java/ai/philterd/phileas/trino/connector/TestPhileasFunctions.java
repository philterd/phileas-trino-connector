/*
 *     Copyright 2024-2025 Philterd, LLC @ https://www.philterd.ai
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.philterd.phileas.trino.connector;

import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static ai.philterd.phileas.trino.connector.PhileasFunctions.redact;
import static ai.philterd.phileas.trino.connector.PhileasFunctions.slice;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class TestPhileasFunctions {

    @Test
    public void testDefaultRedaction() {
        PhileasFunctions.use(new PhileasConfig().setPolicyFile(null));
        assertEquals(Objects.requireNonNull(redact(slice("my word is bond"))).toStringUtf8(), "my word is bond");
        assertEquals(Objects.requireNonNull(redact(slice("my email is rik@resurfacd.io"))).toStringUtf8(), "my email is ****************");
    }

    @Test
    public void testPolicyFileRedaction() throws IOException {
        // A policy file that redacts SSNs and emails. SSN redaction is something the built-in
        // email-only default cannot do, so seeing the SSN removed proves the file was honored.
        final String policyJson = "{ \"identifiers\": { "
                + "\"ssn\": { \"ssnFilterStrategies\": [ { \"strategy\": \"REDACT\", \"redactionFormat\": \"[SSN]\" } ] }, "
                + "\"emailAddress\": { \"emailAddressFilterStrategies\": [ { \"strategy\": \"REDACT\", \"redactionFormat\": \"[EMAIL]\" } ] } } }";
        final Path policyFile = Files.createTempFile("policy", ".json");
        Files.writeString(policyFile, policyJson);

        PhileasFunctions.use(new PhileasConfig().setPolicyFile(policyFile.toString()));

        final String out = Objects.requireNonNull(redact(slice("email rik@resurfacd.io ssn 123-45-6789"))).toStringUtf8();
        assertFalse(out.contains("123-45-6789"), "SSN should be redacted by the policy file: " + out);
        assertFalse(out.contains("rik@resurfacd.io"), "email should be redacted by the policy file: " + out);
        assertTrue(out.contains("[SSN]"), "SSN redaction format should be applied: " + out);
    }

}
