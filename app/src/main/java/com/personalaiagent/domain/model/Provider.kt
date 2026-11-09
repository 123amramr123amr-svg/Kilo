package com.personalaiagent.domain.model

data class Provider(
    val id: String,
    val name: String,
    val kind: ProviderKind,
    val baseUrl: String,
    val isDefault: Boolean,
    val hasKey: Boolean,
    val status: ProviderStatus,
    val lastCheckedAt: Long?,
    val createdAt: Long,
)

/** Values edited in the provider form before validation/persistence. */
data class ProviderDraft(
    val name: String,
    val kind: ProviderKind,
    val baseUrl: String,
)

data class ConnectionTestResult(
    val modelsFound: Int,
    val providerName: String,
)
