package com.amrstudio.ui.projects

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amrstudio.data.model.Project
import com.amrstudio.data.model.ProjectFile
import com.amrstudio.data.model.ProjectVersion
import com.amrstudio.data.repository.ProjectRepository
import com.amrstudio.data.repository.ProjectFileRepository
import com.amrstudio.data.repository.ProjectVersionRepository
import com.amrstudio.utils.IdGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class ProjectsViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val fileRepository: ProjectFileRepository,
    private val versionRepository: ProjectVersionRepository,
    application: Application
) : ViewModel() {

    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    val projects = _projects.asStateFlow()

    private val _currentProject = MutableStateFlow<Project?>(null)
    val currentProject = _currentProject.asStateFlow()

    private val _projectFiles = MutableStateFlow<List<ProjectFile>>(emptyList())
    val projectFiles = _projectFiles.asStateFlow()

    private val _currentFile = MutableStateFlow<ProjectFile?>(null)
    val currentFile = _currentFile.asStateFlow()

    private val _versions = MutableStateFlow<List<ProjectVersion>>(emptyList())
    val versions = _versions.asStateFlow()

    private val _isLoading = MutableStateFlow<Boolean>(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    var currentUserId: String? = null

    fun loadUserProjects(userId: String) {
        currentUserId = userId
        viewModelScope.launch {
            projectRepository.getUserProjects(userId).collect { _projects.value = it }
        }
    }

    fun selectProject(project: Project) {
        _currentProject.value = project
        viewModelScope.launch {
            fileRepository.getProjectFiles(project.id).collect { _projectFiles.value = it }
            versionRepository.getProjectVersions(project.id).collect { _versions.value = it }
        }
    }

    fun createProject(name: String, type: Project.ProjectType): Project {
        val project = Project(
            id = IdGenerator.generate(),
            userId = currentUserId ?: "",
            name = name,
            type = type,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        viewModelScope.launch {
            projectRepository.createProject(project)
            // Create default files
            createDefaultFiles(project)
        }
        return project
    }

    private fun createDefaultFiles(project: Project) {
        val defaultFiles = when (project.type) {
            Project.ProjectType.HTML, Project.ProjectType.LANDING_PAGE -> listOf(
                ProjectFile(IdGenerator.generate(), project.id, "index.html", "index.html", getHtmlTemplate(project.type), "html", LocalDateTime.now(), LocalDateTime.now()),
                ProjectFile(IdGenerator.generate(), project.id, "style.css", "style.css", getCssTemplate(), "css", LocalDateTime.now(), LocalDateTime.now()),
                ProjectFile(IdGenerator.generate(), project.id, "app.js", "app.js", getJsTemplate(), "javascript", LocalDateTime.now(), LocalDateTime.now())
            )
            Project.ProjectType.WEB_APP -> listOf(
                ProjectFile(IdGenerator.generate(), project.id, "index.html", "index.html", getWebAppHtml(), "html", LocalDateTime.now(), LocalDateTime.now()),
                ProjectFile(IdGenerator.generate(), project.id, "style.css", "style.css", getCssTemplate(), "css", LocalDateTime.now(), LocalDateTime.now()),
                ProjectFile(IdGenerator.generate(), project.id, "app.js", "app.js", getWebAppJs(), "javascript", LocalDateTime.now(), LocalDateTime.now()),
                ProjectFile(IdGenerator.generate(), project.id, "manifest.json", "manifest.json", getManifestJson(project.name), "json", LocalDateTime.now(), LocalDateTime.now())
            )
            Project.ProjectType.DASHBOARD -> listOf(
                ProjectFile(IdGenerator.generate(), project.id, "index.html", "index.html", getDashboardHtml(), "html", LocalDateTime.now(), LocalDateTime.now()),
                ProjectFile(IdGenerator.generate(), project.id, "style.css", "style.css", getDashboardCss(), "css", LocalDateTime.now(), LocalDateTime.now()),
                ProjectFile(IdGenerator.generate(), project.id, "app.js", "app.js", getDashboardJs(), "javascript", LocalDateTime.now(), LocalDateTime.now())
            )
            Project.ProjectType.STORE -> listOf(
                ProjectFile(IdGenerator.generate(), project.id, "index.html", "index.html", getStoreHtml(), "html", LocalDateTime.now(), LocalDateTime.now()),
                ProjectFile(IdGenerator.generate(), project.id, "style.css", "style.css", getStoreCss(), "css", LocalDateTime.now(), LocalDateTime.now()),
                ProjectFile(IdGenerator.generate(), project.id, "app.js", "app.js", getStoreJs(), "javascript", LocalDateTime.now(), LocalDateTime.now())
            )
            Project.ProjectType.API -> listOf(
                ProjectFile(IdGenerator.generate(), project.id, "server.js", "server.js", getApiTemplate(), "javascript", LocalDateTime.now(), LocalDateTime.now()),
                ProjectFile(IdGenerator.generate(), project.id, "package.json", "package.json", getPackageJson(project.name), "json", LocalDateTime.now(), LocalDateTime.now())
            )
        }
        viewModelScope.launch {
            fileRepository.saveFiles(defaultFiles)
        }
    }

    fun selectFile(file: ProjectFile) {
        _currentFile.value = file
    }

    fun updateFileContent(file: ProjectFile, content: String) {
        val updated = file.copy(content = content, updatedAt = LocalDateTime.now())
        _projectFiles.value = _projectFiles.value.map { if (it.id == file.id) updated else it }
        _currentFile.value = updated
        viewModelScope.launch {
            fileRepository.updateFile(updated)
        }
    }

    fun createNewFile(projectId: String, name: String, path: String, content: String = "", language: String = "plaintext"): ProjectFile {
        val file = ProjectFile(
            id = IdGenerator.generate(),
            projectId = projectId,
            name = name,
            path = path,
            content = content,
            language = language,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        viewModelScope.launch {
            fileRepository.saveFile(file)
        }
        return file
    }

    fun deleteFile(file: ProjectFile) {
        viewModelScope.launch {
            fileRepository.deleteFile(file.id)
            _projectFiles.value = _projectFiles.value.filter { it.id != file.id }
            if (_currentFile.value?.id == file.id) {
                _currentFile.value = _projectFiles.value.firstOrNull()
            }
        }
    }

    fun saveVersion(name: String, description: String) {
        val project = _currentProject.value ?: return
        val filesSnapshot = Json { prettyPrint = true }.encodeToString(_projectFiles.value)
        val version = ProjectVersion(
            id = IdGenerator.generate(),
            projectId = project.id,
            name = name,
            description = description,
            filesSnapshot = filesSnapshot,
            createdAt = LocalDateTime.now()
        )
        viewModelScope.launch {
            versionRepository.saveVersion(version)
        }
    }

    fun restoreVersion(version: ProjectVersion) {
        val files = Json { }.decodeFromString<List<ProjectFile>>(version.filesSnapshot)
        viewModelScope.launch {
            fileRepository.deleteByProjectId(version.projectId)
            fileRepository.saveFiles(files)
        }
    }

    fun deleteProject(project: Project) {
        viewModelScope.launch {
            fileRepository.deleteByProjectId(project.id)
            versionRepository.getProjectVersions(project.id).first().forEach { versionRepository.deleteVersion(it.id) }
            projectRepository.deleteProject(project.id)
            _projects.value = _projects.value.filter { it.id != project.id }
            if (_currentProject.value?.id == project.id) {
                _currentProject.value = null
                _projectFiles.value = emptyList()
            }
        }
    }

    fun exportProjectAsZip(project: Project): String {
        // Placeholder for ZIP export
        val files = _projectFiles.value
        return Json { prettyPrint = true }.encodeToString(files)
    }

    // Templates
    private fun getHtmlTemplate(type: Project.ProjectType): String = when (type) {
        Project.ProjectType.HTML -> """<!DOCTYPE html>
<html lang="ar" dir="rtl">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>صفحة HTML جديدة</title>
    <link rel="stylesheet" href="style.css">
</head>
<body>
    <main>
        <h1>مرحباً بك في مساعدون عمرو</h1>
        <p>ابدأ بإنشاء صفحتك هنا...</p>
    </main>
    <script src="app.js"></script>
</body>
</html>"""
        Project.ProjectType.LANDING_PAGE -> """<!DOCTYPE html>
<html lang="ar" dir="rtl">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Landing Page</title>
    <link rel="stylesheet" href="style.css">
</head>
<body>
    <header class="hero">
        <h1>منتج رائع</h1>
        <p>وصف مختصر وجذاب لمنتجك</p>
        <button class="cta">ابدأ الآن</button>
    </header>
    <section class="features">
        <div class="feature">ميزة 1</div>
        <div class="feature">ميزة 2</div>
        <div class="feature">ميزة 3</div>
    </section>
    <footer>
        <p>&copy; 2024 مساعدون عمرو</p>
    </footer>
    <script src="app.js"></script>
</body>
</html>"""
        else -> """<!DOCTYPE html>
<html lang="ar" dir="rtl">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>مشروع جديد</title>
    <link rel="stylesheet" href="style.css">
</head>
<body>
    <div id="app">جاري التحميل...</div>
    <script src="app.js"></script>
</body>
</html>"""
    }

    private fun getCssTemplate(): String = """:root {
    --primary: #006D77;
    --primary-dark: #00525A;
    --secondary: #4C6569;
    --background: #F7FDFF;
    --surface: #FFFFFF;
    --text: #191C1D;
    --text-muted: #70797B;
    --border: #C0C8CB;
    --radius: 12px;
    --shadow: 0 2px 8px rgba(0,0,0,0.08);
}

* { box-sizing: border-box; margin: 0; padding: 0; }

body {
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
    background: var(--background);
    color: var(--text);
    line-height: 1.6;
    direction: rtl;
}

.hero {
    padding: 4rem 2rem;
    text-align: center;
    background: linear-gradient(135deg, var(--primary), var(--secondary));
    color: white;
}

.hero h1 { font-size: 3rem; margin-bottom: 1rem; }
.hero p { font-size: 1.25rem; opacity: 0.9; margin-bottom: 2rem; }

.cta {
    background: white;
    color: var(--primary);
    border: none;
    padding: 1rem 2rem;
    font-size: 1.1rem;
    border-radius: var(--radius);
    cursor: pointer;
    font-weight: 600;
    transition: transform 0.2s;
}

.cta:hover { transform: translateY(-2px); }

.features {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
    gap: 1.5rem;
    padding: 3rem 2rem;
    max-width: 1200px;
    margin: 0 auto;
}

.feature {
    background: var(--surface);
    padding: 2rem;
    border-radius: var(--radius);
    box-shadow: var(--shadow);
    text-align: center;
}

footer {
    text-align: center;
    padding: 2rem;
    color: var(--text-muted);
    border-top: 1px solid var(--border);
}

@media (max-width: 640px) {
    .hero h1 { font-size: 2rem; }
    .features { grid-template-columns: 1fr; }
}"""

    private fun getJsTemplate(): String = """// تطبيق مساعدون عمرو
console.log('تم تحميل التطبيق بنجاح');

document.addEventListener('DOMContentLoaded', () => {
    // تهيئة التطبيق هنا
    console.log('DOM جاهز');
});"""

    private fun getWebAppHtml(): String = """<!DOCTYPE html>
<html lang="ar" dir="rtl">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>تطبيق ويب</title>
    <link rel="manifest" href="manifest.json">
    <link rel="stylesheet" href="style.css">
</head>
<body>
    <div id="app">
        <header><h1>تطبيق الويب</h1></header>
        <main>المحتوى الرئيسي</main>
    </div>
    <script src="app.js"></script>
</body>
</html>"""

    private fun getWebAppJs(): String = """// Web App
if ('serviceWorker' in navigator) {
    navigator.serviceWorker.register('/sw.js');
}

console.log('Web App loaded');"""

    private fun getManifestJson(name: String): String = """{
    "name": "$name",
    "short_name": "$name",
    "start_url": "/",
    "display": "standalone",
    "background_color": "#F7FDFF",
    "theme_color": "#006D77",
    "icons": []
}"""

    private fun getDashboardHtml(): String = """<!DOCTYPE html>
<html lang="ar" dir="rtl">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>لوحة تحكم</title>
    <link rel="stylesheet" href="style.css">
</head>
<body>
    <div class="dashboard">
        <aside class="sidebar">القائمة الجانبية</aside>
        <main class="content">
            <header><h1>لوحة التحكم</h1></header>
            <div class="stats">إحصائيات</div>
        </main>
    </div>
    <script src="app.js"></script>
</body>
</html>"""

    private fun getDashboardCss(): String = """
:root {
    --primary: #006D77;
    --sidebar-width: 260px;
}

.dashboard { display: flex; min-height: 100vh; }
.sidebar {
    width: var(--sidebar-width);
    background: var(--surface);
    border-left: 1px solid var(--border);
    padding: 1.5rem;
}
.content { flex: 1; padding: 2rem; }
.stats { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 1rem; }
"""

    private fun getDashboardJs(): String = """// Dashboard
console.log('Dashboard loaded');"""

    private fun getStoreHtml(): String = """<!DOCTYPE html>
<html lang="ar" dir="rtl">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>متجر إلكتروني</title>
    <link rel="stylesheet" href="style.css">
</head>
<body>
    <header><h1>متجرنا</h1></header>
    <main class="products">المنتجات تظهر هنا</main>
    <script src="app.js"></script>
</body>
</html>"""

    private fun getStoreCss(): String = """
:root { --primary: #006D77; }
.products { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 1.5rem; padding: 2rem; }
"""

    private fun getStoreJs(): String = """// Store
console.log('Store loaded');"""

    private fun getApiTemplate(): String = """const express = require('express');
const app = express();
const PORT = process.env.PORT || 3000;

app.use(express.json());

app.get('/api/health', (req, res) => {
    res.json({ status: 'ok', timestamp: new Date().toISOString() });
});

app.listen(PORT, () => {
    console.log(\`Server running on port \${PORT}\`);
});"""

    private fun getPackageJson(name: String): String = """{
    "name": "$name",
    "version": "1.0.0",
    "description": "API Project",
    "main": "server.js",
    "scripts": {
        "start": "node server.js",
        "dev": "nodemon server.js"
    },
    "dependencies": {
        "express": "^4.18.0"
    }
}"""
}