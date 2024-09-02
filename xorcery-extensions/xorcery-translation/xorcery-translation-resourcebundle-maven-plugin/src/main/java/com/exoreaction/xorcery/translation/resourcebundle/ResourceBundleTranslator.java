package com.exoreaction.xorcery.translation.resourcebundle;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.configuration.builder.ConfigurationBuilder;
import com.exoreaction.xorcery.configuration.builder.StandardConfigurationBuilder;
import com.exoreaction.xorcery.translation.api.Translation;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ResourceBundleTranslator {

    private final Translation translation;

    public ResourceBundleTranslator(Translation translation) {
        this.translation = translation;
    }

    public CompletableFuture<ObjectNode> translate(String sourceBaseName, Locale sourceLocale, File result, List<Locale> resultLocales) {
        try {
            // Load source
            Configuration sourceBundle = new ConfigurationBuilder(sourceBaseName).addDefaults().build();

            // Find all paths that needs translation
            List<String> translationPaths = new ArrayList<>();
            findPaths(sourceBundle.json(), Set.of("default"), "", translationPaths);

            System.out.println("Translation paths:" + translationPaths);

            Configuration.Builder builder = new Configuration.Builder();
            // Load existing result, if any
            if (result.exists()) {
                StandardConfigurationBuilder standardConfigurationBuilder = new StandardConfigurationBuilder();
                try (FileInputStream inputStream = new FileInputStream(result)) {
                    standardConfigurationBuilder.addYaml(inputStream).accept(builder);
                }
            }
            Configuration resultBundle = builder.build();

            // Remove paths not found in source
            Set<String> resultPaths = new HashSet<>();
            Set<String> allowedLeafKeys = resultLocales.stream().map(Locale::getLanguage).collect(Collectors.toSet());
            allowedLeafKeys.addAll(resultLocales.stream().map(Locale::getCountry).collect(Collectors.toSet()));
            findPaths(resultBundle.json(), allowedLeafKeys, "", resultPaths);
            System.out.println("Existing result paths:" + result);
            Set<String> unusedPaths = new HashSet<>(resultPaths);
            unusedPaths.removeAll(translationPaths);
            System.out.println("Unused paths:" + unusedPaths);
            if (!unusedPaths.isEmpty()) {
                removeUnusedKeys(resultBundle.json(), "", unusedPaths);
            }

            // Calculate translation needs per target locale
            Map<Locale, List<TranslationRequest>> translationRequired = new LinkedHashMap<>();
            for (String translationPath : translationPaths) {
                for (Locale resultLocale : resultLocales) {
                    String resultPath = translationPath + "." + resultLocale.toLanguageTag().replace('_', '.');
                    if (!sourceBundle.has(resultPath) && !resultBundle.has(resultPath)) {
                        translationRequired.computeIfAbsent(resultLocale, v -> new ArrayList<>())
                                .add(new TranslationRequest(resultPath, sourceBundle.getString(translationPath + ".default").orElseThrow()));
                    }
                }
            }

            // Perform translations
            Map<String, String> newTranslations = new LinkedHashMap<>();
            for (Map.Entry<Locale, List<TranslationRequest>> localeTranslations : translationRequired.entrySet()) {
                List<String> sourceTexts = localeTranslations
                        .getValue()
                        .stream()
                        .map(TranslationRequest::sourceText)
                        .toList();

                List<String> translations = translation.translate(sourceTexts, sourceLocale, localeTranslations.getKey())
                        .orTimeout(30, TimeUnit.SECONDS).join();

                for (int i = 0; i < localeTranslations.getValue().size(); i++) {
                    TranslationRequest translationRequest = localeTranslations.getValue().get(i);
                    newTranslations.put(translationRequest.translationPath, translations.get(i));
                }
            }

            // Merge translations into result
            Configuration.Builder resultBuilder = resultBundle.asBuilder();
            for (Map.Entry<String, String> translationPathText : newTranslations.entrySet()) {
                resultBuilder.add(translationPathText.getKey(), translationPathText.getValue());
            }

            return CompletableFuture.completedFuture(resultBuilder.build().json());
        } catch (Throwable e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private void findPaths(ObjectNode json, Set<String> allowedLeafKeys, String path, Collection<String> translationKeys) {
        for (Map.Entry<String, JsonNode> property : json.properties()) {
            if (property.getValue() instanceof ObjectNode child) {
                findPaths(child, allowedLeafKeys, path + (path.isEmpty() ? "" : ".") + property.getKey(), translationKeys);
            } else if (allowedLeafKeys.contains(property.getKey()) && !path.isEmpty()) {
                translationKeys.add(path);
            }
        }
    }

    private void removeUnusedKeys(ObjectNode json, String path, Set<String> unusedKeys) {
        for (Map.Entry<String, JsonNode> property : new HashSet<>(json.properties())) {
            if (property.getValue() instanceof ObjectNode child) {
                String childPath = path + (path.isEmpty() ? "" : ".") + property.getKey();
                if (unusedKeys.contains(childPath)) {
                    json.remove(property.getKey());
                } else {
                    removeUnusedKeys(child, childPath, unusedKeys);
                }
            }
        }
    }

    record TranslationRequest(String translationPath, String sourceText) {
    }
}
