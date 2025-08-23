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

import ai.philterd.phileas.model.cache.InMemoryCache;
import ai.philterd.phileas.model.configuration.PhileasConfiguration;
import ai.philterd.phileas.model.enums.MimeType;
import ai.philterd.phileas.model.policy.Identifiers;
import ai.philterd.phileas.model.policy.Policy;
import ai.philterd.phileas.model.policy.filters.EmailAddress;
import ai.philterd.phileas.model.policy.filters.strategies.AbstractFilterStrategy;
import ai.philterd.phileas.model.policy.filters.strategies.rules.EmailAddressFilterStrategy;
import ai.philterd.phileas.services.PhileasFilterService;
import io.airlift.log.Logger;
import io.airlift.slice.Slice;
import io.trino.spi.function.Description;
import io.trino.spi.function.ScalarFunction;
import io.trino.spi.function.SqlNullable;
import io.trino.spi.function.SqlType;

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
            return s == null ? null : slice(filterService.filter(policy, "<cxt>", "<doc>", s.toStringUtf8(), MimeType.TEXT_PLAIN).getFilteredText());
        } catch (Exception e) {
            log.error(e);
            return null;
        }
    }

    static Slice slice(String s) {
        return utf8Slice(nullToEmpty(s));
    }

    static void use(PhileasConfig config) {
        if (config != null) log.info("using phileas config: policy.file=" + config.getPolicyFile());     // todo use file to configure identifiers

        // configure identifiers to mask email addresses (temporary)
        Identifiers identifiers = new Identifiers();
        EmailAddressFilterStrategy fs = new EmailAddressFilterStrategy();
        fs.setStrategy(AbstractFilterStrategy.MASK);
        fs.setMaskCharacter("*");
        fs.setMaskLength(AbstractFilterStrategy.SAME);
        EmailAddress x = new EmailAddress();
        x.setEmailAddressFilterStrategies(List.of(fs));
        identifiers.setEmailAddress(x);

        // create filter service
        try {
            policy = new Policy();
            policy.setName("default");
            policy.setIdentifiers(identifiers);
            Properties properties = new Properties();
            PhileasConfiguration configuration = new PhileasConfiguration(properties, "phileas");
            filterService = new PhileasFilterService(configuration, new InMemoryCache());
        } catch (Exception e) {
            log.error(e);
        }
    }

    private static PhileasFilterService filterService;
    private static Policy policy;

    private static final Logger log = Logger.get(PhileasFunctions.class);

}
