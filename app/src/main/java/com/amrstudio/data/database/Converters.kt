package com.amrstudio.data.database

import androidx.room.TypeConverter
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.DateTimeFormatter
import kotlinx.serialization.json.Json

class Converters {
    private val json = Json { ignoreUnknownKeys = true }
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(TimeZone.currentSystemDefault())

    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime?): String? = value?.format(formatter)

    @TypeConverter
    fun toLocalDateTime(value: String?): LocalDateTime? = value?.let { LocalDateTime.parse(it, formatter) }

    @TypeConverter
    fun fromStringList(value: List<String>?): String = json.encodeToString(value ?: emptyList())

    @TypeConverter
    fun toStringList(value: String): List<String> = json.decodeFromString(value)

    @TypeConverter
    fun fromModelType(value: com.amrstudio.data.model.Model.ModelType?): String? = value?.name

    @TypeConverter
    fun toModelType(value: String?): com.amrstudio.data.model.Model.ModelType? = value?.let { com.amrstudio.data.model.Model.ModelType.valueOf(it) }

    @TypeConverter
    fun fromMessageRole(value: com.amrstudio.data.model.Message.MessageRole?): String? = value?.name

    @TypeConverter
    fun toMessageRole(value: String?): com.amrstudio.data.model.Message.MessageRole? = value?.let { com.amrstudio.data.model.Message.MessageRole.valueOf(it) }

    @TypeConverter
    fun fromProjectType(value: com.amrstudio.data.model.Project.ProjectType?): String? = value?.name

    @TypeConverter
    fun toProjectType(value: String?): com.amrstudio.data.model.Project.ProjectType? = value?.let { com.amrstudio.data.model.Project.ProjectType.valueOf(it) }

    @TypeConverter
    fun fromConnectorType(value: com.amrstudio.data.model.Connector.ConnectorType?): String? = value?.name

    @TypeConverter
    fun toConnectorType(value: String?): com.amrstudio.data.model.Connector.ConnectorType? = value?.let { com.amrstudio.data.model.Connector.ConnectorType.valueOf(it) }

    @TypeConverter
    fun fromTaskStatus(value: com.amrstudio.data.model.AgentTask.TaskStatus?): String? = value?.name

    @TypeConverter
    fun toTaskStatus(value: String?): com.amrstudio.data.model.AgentTask.TaskStatus? = value?.let { com.amrstudio.data.model.AgentTask.TaskStatus.valueOf(it) }

    @TypeConverter
    fun fromDownloadStatus(value: com.amrstudio.data.model.Download.DownloadStatus?): String? = value?.name

    @TypeConverter
    fun toDownloadStatus(value: String?): com.amrstudio.data.model.Download.DownloadStatus? = value?.let { com.amrstudio.data.model.Download.DownloadStatus.valueOf(it) }

    @TypeConverter
    fun fromNotificationType(value: com.amrstudio.data.model.Notification.NotificationType?): String? = value?.name

    @TypeConverter
    fun toNotificationType(value: String?): com.amrstudio.data.model.Notification.NotificationType? = value?.let { com.amrstudio.data.model.Notification.NotificationType.valueOf(it) }
}