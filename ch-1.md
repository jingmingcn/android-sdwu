---
marp: true
size: 16:9
theme: am_purple
paginate: true
# headingDivider: [2,3,4]
footer: \ *井明（数据科学与计算机学院）* *移动应用开发（Android+Kotlin）* *2025年秋*
---

<!-- _class: cover_e -->
<!-- _paginate: "" -->
<!-- _footer: ![](./images/logo_transparent.png) -->
<!-- _header: ![](./images/logo-1_transparent.png) -->

# 移动应用开发 之 Android 开发环境

###### “Any sufficiently advanced app is indistinguishable from magic.”

井明
数据科学与计算机学院
jingming@sdu.edu.cn
2025年秋

---

<!-- _header: 目录<br>CONTENTS<br>![](images/logo_1_transparent.png)-->
<!-- _class: toc_b -->
<!-- _footer: "" -->
<!-- _paginate: "" -->

---
## 所需软件及国内下载地址

1. JDK (Java Development Kit)
  - 推荐版本：JDK 17+（Android Studio Giraffe 及以上版本推荐）
  - 国内下载地址：华为云 OpenJDK 镜像 [https://mirrors.huaweicloud.com/openjdk/](https://mirrors.huaweicloud.com/openjdk/)
  - 选择对应系统的.zip 或 .tar.gz 或.exe 文件（Windows 建议 x64 版本）
2. Android Studio
  - 推荐版本：最新稳定版
  - 国内下载地址：Android Studio 中文社区 [https://developer.android.google.cn/studio](https://developer.android.google.cn/studio) 
  - 选择 "Android Studio" 完整版下载

---

## JDK 安装与配置

1. 下载并安装 JDK
  - Windows：运行下载的 .exe 文件，按照提示完成安装
  - 或者解压 .zip 文件到指定目录（如 C:\Program Files\Java\jdk-17）
2. 配置环境变量
  - Windows：
    - 右键“此电脑” -> “属性” -> “高级系统设置” -> “环境变量”
    - 在“系统变量”中找到“Path”，点击“编辑”，添加 JDK 的 bin 目录路径（如 C:\Program Files\Java\jdk-17\bin）
    - 新建变量 JAVA_HOME，值为 JDK 安装路径（如 C:\Program Files\Java\jdk-17）
3. 验证安装
  - 打开终端或命令提示符，输入 `java -version` 和 `javac -version`，应显示安装的 JDK 版本信息

---

## Android Studio 安装与配置

1. 运行下载的安装文件
1. 选择 "Do not import settings"，点击 OK
1. 同意许可协议
1. 选择安装路径（建议：D:\Android\Android Studio，避免系统盘）
1. 选择组件：保持默认即可（Android Studio、Android Virtual Device）
1. 点击 Install 完成安装

---
## Android Studio 全局代理设置

1. 打开 Android Studio
1. 进入 File > Settings > Appearance & Behavior > System Settings > HTTP Proxy
1. 选择 "Auto-detect proxy settings" ，
2. 输入代理服务器地址： `https://mirrors.aliyun.com/android.googlesource.com/`

---
## Gradle 代理配置

1. 在用户目录下找到 `.gradle` 文件夹（如 C:\Users\YourUsername\\.gradle）
2. 编辑或创建 `init.gradle` 文件，添加以下内容：
---
```groovy
allprojects {
    repositories {
        maven { url 'https://maven.aliyun.com/repository/google' }
        maven { url 'https://maven.aliyun.com/repository/jcenter' }
        maven { url 'https://maven.aliyun.com/repository/central' }
        maven { url 'https://maven.aliyun.com/repository/gradle-plugin' }
        maven { url 'https://mirrors.huaweicloud.com/repository/maven' }
        maven { url 'https://maven.aliyun.com/repository/public' }
        // 保留Google和MavenCentral仓库，但国内镜像会优先使用
        google()
        mavenCentral()
    }
    
    buildscript {
        repositories {
            maven { url 'https://maven.aliyun.com/repository/google' }
            maven { url 'https://maven.aliyun.com/repository/jcenter' }
            maven { url 'https://maven.aliyun.com/repository/central' }
            maven { url 'https://maven.aliyun.com/repository/gradle-plugin' }
            maven { url 'https://mirrors.huaweicloud.com/repository/maven' }
            google()
            mavenCentral()
        }
    }
}
```

---
<!-- _footer: '' -->
## 测试开发环境

创建第一个 Kotlin Android 项目

1. 打开 Android Studio，选择 "New Project"
2. 选择 "Empty Compose Activity"，点击 "Next"
3. 配置项目名称、包名、保存位置等，点击 "Finish"
     - Name: FirstApp
     - Package name: com.example.firstapp
      - Language: Kotlin
     - Minimum SDK: 选择API 24: Android 7.0 (Nougat)
4. 修改`settings.gradle.kts`文件，确保包含以下内容：
```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS) // 默认
    // 改成
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT) // 或 DISABLED
}
```

---

## 运行测试应用

1. 点击 Android Studio 工具栏中的 "AVD Manager" 图标（手机加小屏幕的图标）
1. 点击 "Create Virtual Device"
1. 选择一个设备（如 "Pixel 6"），点击 "Next"
1. 选择一个系统镜像（建议选择带有 "Google APIs" 的版本）
   1. 如果没有下载，点击 "Download"，通过之前配置的国内源会加速下载
2. 点击 "Finish" 创建模拟器
3. 点击工具栏中的 "Run" 按钮（绿色三角形）
4. 等待应用编译并在模拟器中启动
5. 点击应用中的 "Click Me" 按钮，文本应从 "Hello, Android!" 变为 "Hello, Kotlin!"