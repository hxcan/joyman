# JoyMan 项目目录结构

## 📁 完整目录树

```
joyman/
├── app/
│   ├── build.gradle                    # 应用级构建配置
│   ├── src/
│   │   └── main/
│   │       ├── AndroidManifest.xml     # 应用清单
│   │       ├── java/com/stupidbeauty/joyman/
│   │       │   ├── MainActivity.java                 # 主界面
│   │       │   ├── VoiceTaskActivity.java            # 语音创建任务
│   │       │   ├── data/                           # 数据层
│   │       │   │   ├── database/                   # Room 数据库
│   │       │   │   │   ├── AppDatabase.java          # 数据库定义
│   │       │   │   │   ├── dao/                    # 数据访问对象
│   │       │   │   │   │   ├── TaskDao.java          # 任务 DAO
│   │       │   │   │   │   └── ProjectDao.java       # 项目 DAO
│   │       │   │   │   └── entity/                 # 实体类
│   │       │   │   │       ├── Task.java             # 任务实体
│   │       │   │   │       └── Project.java          # 项目实体
│   │       │   │   ├── model/                      # 业务模型
│   │       │   │   │   ├── TaskPackage.java          # 任务包（导入导出）
│   │       │   │   │   └── SyncResult.java           # 同步结果
│   │       │   │   └── repository/                 # 数据仓库
│   │       │   │       └── TaskRepository.java
│   │       │   ├── util/                           # 工具类
│   │       │   │   ├── IdGenerator.java              # ID 生成器（核心）
│   │       │   │   ├── ConflictResolver.java         # 冲突解决器
│   │       │   │   └── JsonUtil.java                 # JSON 工具
│   │       │   ├── service/                        # 服务层
│   │       │   │   ├── P2PService.java               # P2P 同步服务
│   │       │   │   └── RedmineApiService.java        # Redmine API 兼容层
│   │       │   └── ui/                             # UI 组件
│   │       │       ├── adapter/                    # RecyclerView 适配器
│   │       │       │   └── TaskAdapter.java
│   │       │       └── viewmodel/                  # ViewModel
│   │       │           └── TaskViewModel.java
│   │       └── res/                                # 资源文件
│   │           ├── values/
│   │           │   ├── strings.xml
│   │           │   ├── colors.xml
│   │           │   └── themes.xml
│   │           ├── layout/                         # 布局文件
│   │           │   ├── activity_main.xml
│   │           │   └── item_task.xml
│   │           └── mipmap/                         # 图标
│   └── proguard-rules.pro                          # 混淆规则
├── docs/                                           # 文档
│   ├── project-structure.md                        # 本文档
│   ├── database-schema.md                          # 数据库设计
│   ├── id-generation.md                            # ID 生成算法
│   ├── sync-protocol.md                            # P2P 同步协议
│   └── redmine-compatibility.md                    # Redmine 兼容性说明
├── build.gradle                                    # 根构建配置
├── settings.gradle                                 # 项目设置
├── gradle.properties                               # Gradle 属性
├── .gitignore                                      # Git 忽略规则（Android）
├── LICENSE                                         # MIT License
└── README.md                                       # 项目说明
```

## 🎯 核心模块说明

### 1. **数据层 (data/)**
- **Room 数据库**: 本地 SQLite 封装，支持异步查询
- **DAO**: 标准化的数据访问接口
- **Entity**: 任务、项目等核心数据模型
- **Repository**: 统一数据源管理（本地 DB + 导入导出）

### 2. **工具层 (util/)**
- **IdGenerator**: 时间戳 + 随机数生成 14 位数字 ID
- **ConflictResolver**: 检测标题 + 创建时间冲突，自动重新分配 ID
- **JsonUtil**: 任务包序列化/反序列化

### 3. **服务层 (service/)**
- **P2PService**: WiFi Direct/蓝牙设备发现与数据传输
- **RedmineApiService**: 最小化实现 Redmine API 兼容接口

### 4. **UI 层 (ui/)**
- **Adapter**: 任务列表展示
- **ViewModel**: MVVM 架构，生命周期感知

## 📋 下一步开发优先级

1. ✅ 项目结构搭建（已完成）
2. ⏳ 数据库设计 (`docs/database-schema.md`)
3. ⏳ ID 生成器实现 (`util/IdGenerator.java`)
4. ⏳ 基础 CRUD 操作 (`dao/TaskDao.java`)
5. ⏳ 简单 UI 界面 (`activity_main.xml`)

---
*更新时间：2026-03-31 | 版本：v1.0.0-alpha*

> **重要说明**: 本项目使用 **纯 Java** 开发，所有源代码文件均为 `.java` 格式。文档中所有 `.kt` 引用已更正为 `.java`。