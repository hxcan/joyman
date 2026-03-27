# JoyMan 快速开始指南

## 🚀 编译运行（5 分钟上手）

### 前置条件
- Android Studio Arctic Fox (2020.3.1) 或更高版本
- JDK 8 或更高版本
- Android SDK API 34

### 步骤 1：克隆项目
```bash
git clone https://github.com/hxcan/joyman.git
cd joyman
```

### 步骤 2：打开项目
1. 启动 Android Studio
2. 选择 `File` → `Open`
3. 选择 `joyman` 文件夹
4. 等待 Gradle 同步完成（首次需要下载依赖，约 2-5 分钟）

### 步骤 3：运行应用
**方式 A：模拟器**
1. 点击 `Tools` → `Device Manager`
2. 创建一个新的虚拟设备（推荐 Pixel 6, API 34）
3. 点击运行按钮（绿色三角形）或按 `Shift + F10`

**方式 B：真机调试**
1. 手机开启开发者模式和 USB 调试
2. 通过 USB 连接电脑
3. 点击运行按钮

### 预期效果
- ✅ 应用成功安装并启动
- ✅ 主界面显示 3 个示例任务
- ✅ 点击右上角菜单可看到：同步、导出、导入、设置选项
- ✅ 点击悬浮按钮（FAB）可扩展添加快速创建功能（待实现）

## 📁 当前项目结构

```
app/src/main/java/com/stupidbeauty/joyman/
├── MainActivity.java              # 主界面
├── data/
│   └── database/entity/
│       └── Task.java              # 任务实体
├── util/
│   └── IdGenerator.java           # ID 生成器
└── ui/adapter/
    └── TaskAdapter.java           # 列表适配器
```

## 🔧 下一步开发建议

### 阶段一：数据库集成（优先级：高）
1. 创建 `AppDatabase.java` - Room 数据库定义
2. 创建 `TaskDao.java` - 数据访问接口
3. 在 `MainActivity` 中替换示例数据为真实数据库查询

### 阶段二：任务创建功能（优先级：高）
1. 创建 `NewTaskActivity.java`
2. 添加表单布局 `activity_new_task.xml`
3. 实现保存逻辑

### 阶段三：完整 CRUD（优先级：中）
- 编辑任务
- 删除任务
- 任务详情查看

## 🐛 已知问题
- [ ] 悬浮按钮（FAB）点击事件未实现
- [ ] 空状态提示视图未绑定逻辑
- [ ] 菜单项功能均为 Toast 提示（待实现真实功能）

## 📞 帮助与支持
如遇到编译问题，请检查：
1. Gradle 版本是否匹配（项目使用 AGP 8.2.0）
2. 网络连接是否正常（需下载 Maven 依赖）
3. SDK 是否安装了 API 34

---
*更新时间：2026-03-27 | 版本：v1.0.0-alpha*
