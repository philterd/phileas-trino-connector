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

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.policy.Identifiers;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.policy.filters.EmailAddress;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import ai.philterd.phileas.services.filters.filtering.PlainTextFilterService;
import ai.philterd.phileas.services.strategies.AbstractFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.EmailAddressFilterStrategy;
import com.google.gson.Gson;
import io.airlift.log.Logger;
import io.airlift.slice.Slice;
import io.trino.spi.function.Description;
import io.trino.spi.function.ScalarFunction;
import io.trino.spi.function.SqlNullable;
import io.trino.spi.function.SqlType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import static com.google.common.base.Strings.nullToEmpty;
import static io.airlift.slice.Slices.utf8Slice;

public final class PhileasFunctions {

    @SqlNullable
    @Description("Redact input string using system policy")
    @ScalarFunction("phileas_redact")
    @SqlType("varchar")
    public static Slice redact(@SqlType("varchar") Slice s) {
        try {
            return s == null ? null : slice(filterService.filter(policy, "<cxt>", s.toStringUtf8()).getFilteredText());
        } catch (Exception e) {
            log.error(e);
            return null;
        }
    }

    static Slice slice(String s) {
        return utf8Slice(nullToEmpty(s));
    }

    static void use(PhileasConfig config) {
        final String policyFile = (config == null) ? null : config.getPolicyFile();

        try {
            if (policyFile != null && !policyFile.isBlank()) {
                // Load the redaction policy from the configured JSON file. A Phileas Policy is a
                // Gson POJO, so this deserializes the same way Phileas itself loads a policy.
                final String json = Files.readString(Path.of(policyFile));
                policy = new Gson().fromJson(json, Policy.class);
                log.info("loaded phileas policy from " + policyFile);
            } else {
                // No policy file configured: fall back to a built-in policy that masks email addresses.
                log.info("no phileas.policy.file configured; using built-in email-only policy");
                policy = defaultPolicy();
            }

            final Properties properties = new Properties();
            final PhileasConfiguration configuration = new PhileasConfiguration(properties);
            filterService = new PlainTextFilterService(configuration, new DefaultContextService(), new InMemoryVectorService(), null);
        } catch (Exception e) {
            log.error(e);
        }
    }

    // The built-in policy used when no phileas.policy.file is configured: mask email addresses only.
    private static Policy defaultPolicy() {
        final Identifiers identifiers = new Identifiers();
        final EmailAddressFilterStrategy fs = new EmailAddressFilterStrategy();
        fs.setStrategy(AbstractFilterStrategy.MASK);
        fs.setMaskCharacter("*");
        fs.setMaskLength(AbstractFilterStrategy.SAME);
        final EmailAddress x = new EmailAddress();
        x.setEmailAddressFilterStrategies(List.of(fs));
        identifiers.setEmailAddress(x);
        final Policy p = new Policy();
        p.setIdentifiers(identifiers);
        return p;
    }

    private static PlainTextFilterService filterService;
    private static Policy policy;

    private static final Logger log = Logger.get(PhileasFunctions.class);

}
