package com.hamdel.ai.data.remote

/**
 * Decrypted API credentials used to talk to the AI providers.
 * Populated at runtime by [SecureKeyManager] – never hardcoded, never committed.
 */
data class AiKeys(
    val gapgptKeys: List<String> = emptyList(),
    val liaraKeys: List<String> = emptyList(),
    val liaraBaseUrl: String? = null
) {
    val hasGapgpt: Boolean get() = gapgptKeys.isNotEmpty()
    val hasLiara: Boolean get() = liaraKeys.isNotEmpty()
    val isEmpty: Boolean get() = !hasGapgpt && !hasLiara

    companion object {
        private val KNOWN_LIARA_BASE_URL_REGEX =
            Regex("https://ai\\.liara\\.ir/api/[a-zA-Z0-9]+/v1")

        /**
         * Parses the decrypted keys.txt content. Supports:
         *  - KEY=VALUE lines (GAPGPT_API_KEY / GAPGPT_KEY / LIARA_API_KEY / LIARA_KEY / LIARA_BASE_URL ...)
         *  - PREFIX:VALUE lines (e.g. "gapgpt:sk-..." / "liara:eyJ...") - this is the actual format
         *    produced by the current encrypt_keys.py / keys.txt files served from the remote URLs.
         *  - Bare lines: values starting with "sk-" are treated as gapgpt keys (OpenAI style, matches
         *    the gapgpt docs), any other non-blank token is treated as a Liara key candidate.
         *  - A full Liara base URL (https://ai.liara.ir/api/<project>/v1) if present anywhere in the file.
         */
        fun parse(raw: String): AiKeys {
            val gapgpt = linkedSetOf<String>()
            val liara = linkedSetOf<String>()
            var liaraBaseUrl: String? = null

            KNOWN_LIARA_BASE_URL_REGEX.find(raw)?.let { liaraBaseUrl = it.value }

            fun handleBareToken(line: String) {
                val token = line.trim('"', ',')
                if (KNOWN_LIARA_BASE_URL_REGEX.matches(token)) {
                    liaraBaseUrl = token
                } else if (token.startsWith("sk-")) {
                    gapgpt += token
                } else if (token.length > 10) {
                    liara += token
                }
            }

            raw.lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") && !it.startsWith("//") }
                .forEach { line ->
                    val eqIndex = line.indexOf('=')
                    val colonIndex = line.indexOf(':')
                    // Prefer '=' (KEY=VALUE) when present before any ':'.
                    val useEquals = eqIndex > 0 && (colonIndex <= 0 || eqIndex < colonIndex)
                    val prefixCandidate = if (!useEquals && colonIndex > 0) {
                        line.substring(0, colonIndex).trim().uppercase()
                    } else null

                    when {
                        useEquals -> {
                            val key = line.substring(0, eqIndex).trim().uppercase()
                            val value = line.substring(eqIndex + 1).trim().trim('"')
                            if (value.isBlank()) return@forEach
                            when {
                                key.contains("GAPGPT") -> gapgpt += value
                                key.contains("LIARA") && key.contains("URL") -> liaraBaseUrl = value
                                key.contains("LIARA") -> liara += value
                                key.contains("OPENAI") -> gapgpt += value
                                else -> handleBareToken(line)
                            }
                        }
                        // e.g. "gapgpt:sk-xxx" or "liara:eyJ..." - the real-world keys.txt format.
                        prefixCandidate != null &&
                            (prefixCandidate.contains("GAPGPT") ||
                                prefixCandidate.contains("LIARA") ||
                                prefixCandidate.contains("OPENAI")) -> {
                            val value = line.substring(colonIndex + 1).trim().trim('"', ',')
                            if (value.isBlank()) return@forEach
                            when {
                                prefixCandidate.contains("GAPGPT") -> gapgpt += value
                                prefixCandidate.contains("OPENAI") -> gapgpt += value
                                prefixCandidate.contains("LIARA") && (
                                    prefixCandidate.contains("URL") ||
                                        KNOWN_LIARA_BASE_URL_REGEX.matches(value)
                                    ) -> liaraBaseUrl = value
                                prefixCandidate.contains("LIARA") -> liara += value
                            }
                        }
                        else -> handleBareToken(line)
                    }
                }

            return AiKeys(
                gapgptKeys = gapgpt.toList(),
                liaraKeys = liara.toList(),
                liaraBaseUrl = liaraBaseUrl
            )
        }
    }
}
