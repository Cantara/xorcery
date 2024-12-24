/*
 * Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.xorcery.translation.deepl.test;

import dev.xorcery.configuration.spi.ResourceBundleTranslationProvider;
import dev.xorcery.translation.deepl.providers.DeepLResourceBundleTranslationProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Disabled("only enabled during development, requires authkey")
public class DeepLResourceBundleTranslationProviderTest
{

    protected static ResourceBundleTranslationProvider translationProvider;

    @BeforeAll
    public static void setup() throws ExecutionException, InterruptedException, TimeoutException {
        translationProvider = new DeepLResourceBundleTranslationProvider();
    }

    @Test
    public void testTranslate()
    {
        String result = translationProvider.translate("This is a test", Locale.GERMAN).orTimeout(10, TimeUnit.SECONDS).join();

        Assertions.assertEquals("Dies ist ein Test", result);
    }

    @Test
    public void testTranslateFailed()
    {
        String result = translationProvider.translate("This is a test", new Locale.Builder().setLanguage("am").build()).orTimeout(10, TimeUnit.SECONDS).join();
        Assertions.assertNull(result);
    }

}
