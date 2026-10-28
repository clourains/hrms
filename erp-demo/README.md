# ERP 实验工程（模拟出库 → 自动推送运单到 HRMS）

本目录是一个独立的 Spring Boot 工程（端口 8090，H2 内存库），用于在本机模拟 ERP 出库流程，并把运单自动推送到 HRMS，验证「出库确认 → 运单入系统 → 17TRACK 轨迹 → 驾驶舱航线」全链路。

## 目录结构

```
erp-demo/
├── pom.xml                      # Spring Boot 3.5.x + Java 17
├── run-erp.ps1                  # 启动脚本（-Background 后台运行）
├── fetch-key.ps1                # 从 HRMS 拉取开放接口 Key 并注入配置
├── inbox/                       # 监控目录：放入 CSV 即自动导入（处理完移入 archive/）
├── templates/
│   ├── shipment-import-template.csv    # 精简模板
│   └── 17track-export-template.csv     # 17TRACK 导出风格模板
├── docs/erp-integration/        # 正式 ERP 接入文档与 Java 示例代码
└── src/main/                    # 后端 + 页面（http://localhost:8090/）
```

## 环境要求

- JDK 17
- Maven（HRMS 仓库内自带 `.mvn/apache-maven-3.9.16`；脱离仓库使用时需系统已安装 `mvn`）
- HRMS 后端运行在 `http://localhost:8080/api`

## 启动

```powershell
cd erp-demo
powershell -ExecutionPolicy Bypass -File .\run-erp.ps1 -Background
```

页面：<http://localhost:8090/>

## 功能

1. **手工出库**：页面上新增出库单 → 确认出库 → 20 秒内自动推送。
2. **监控目录导入**：把 17TRACK 导出的 CSV 放进 `inbox/`，10 秒内自动识别、建单、确认并推送；重复运单号跳过；文件处理完移入 `inbox/archive/`。
3. **推送状态**：页面显示 `PENDING / DONE / FAILED`、重试次数和失败原因，失败可手动重试。
4. **outbox 重试**：网络异常按 2/4/8/16 分钟退避，最多 5 次；HRMS 校验失败直接标 `FAILED` 转人工。

## 对接 HRMS

- 接口：`POST http://localhost:8080/api/open-api/import/shipment`
- 鉴权：请求头 `X-API-Key`
- 完整字段、ERP 侧接入步骤、重试与幂等策略见 [docs/erp-integration/README.md](docs/erp-integration/README.md)

## 注意

- H2 是内存库，**重启后出库单与推送记录清空**。
- 演示运单号在 17TRACK 查不到，HRMS 里只会有本地「已交运」节点；真实运单号由 HRMS 每 6 小时自动同步轨迹。
- 开放接口 Key 写在 `src/main/resources/application.yml`，可用 `fetch-key.ps1` 从本机 HRMS 重新注入。
- 本工程不会写入 HRMS 代码，只调用其开放接口。
