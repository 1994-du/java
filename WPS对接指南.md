# WPS Office SDK 对接指南

## 问题说明

目前前端虽然可以上传文件，但无法在 WPS 中打开，因为：

1. **WPS SDK 需要的是 WPS 服务器上的 fileId**，不是我们自己后端的文件 ID
2. 需要后端完整对接 WPS 开放平台 API

## 完整对接流程

### 第一步：WPS 开放平台配置

1. 登录 [WPS 开放平台](https://open.wps.cn/)
2. 进入应用管理 → 你的应用
3. 配置以下信息：

```
回调地址（获取文件）: https://your-domain.com/wps/callback/file
回调地址（保存文件）: https://your-domain.com/wps/callback/save
回调地址（文件状态）: https://your-domain.com/wps/callback/status
```

### 第二步：后端需要实现的接口

#### 1. 获取文件接口 (GET /wps/callback/file)

```javascript
// 当 WPS 需要获取文件内容时调用
router.get('/wps/callback/file', async (req, res) => {
  const { fileId } = req.query;
  
  // 从你的数据库/存储中读取文件
  const file = await db.files.findOne({ where: { wpsFileId: fileId } });
  
  // 返回文件内容
  res.setHeader('Content-Type', file.mimeType);
  res.sendFile(file.path);
});
```

#### 2. 保存文件接口 (POST /wps/callback/save)

```javascript
// 当用户在 WPS 中点击保存时调用
router.post('/wps/callback/save', async (req, res) => {
  const { fileId } = req.query;
  const fileContent = req.body; // 文件二进制内容
  
  // 保存到你的存储
  await saveFile(fileId, fileContent);
  
  res.json({
    code: 0,
    msg: "success",
    data: {
      fileId: fileId
    }
  });
});
```

#### 3. 上传文件到 WPS 并获取 fileId

```javascript
// 后端需要实现：上传文件到 WPS 并获取 wpsFileId
const uploadToWPS = async (file) => {
  // 1. 先上传文件到 WPS
  const formData = new FormData();
  formData.append('file', file);
  
  const response = await axios.post(
    'https://open.wps.cn/office/api/file/upload',
    formData,
    {
      headers: {
        'Content-Type': 'multipart/form-data',
        'Authorization': `Bearer ${WPS_ACCESS_TOKEN}`
      }
    }
  );
  
  return response.data.fileId; // 这个才是给前端 SDK 用的 fileId
};
```

### 第三步：前端修改

目前的前端代码大部分是正确的，只需要确保：

1. **fileId** 来自后端调用 WPS API 后返回的 wpsFileId
2. 其他配置保持不变

## 当前演示状态

现在的代码可以作为：
- ✅ 完整的前端 UI 演示
- ✅ SDK 集成示例
- ✅ 上传功能演示

要真正使用，需要后端完成上述对接。

## 测试建议

1. 先确认你的 WPS 应用状态是**已发布**
2. 检查 AppID 和 AppSecret 是否正确
3. 确保回调地址公网可访问（WPS 需要能调用你的后端）
4. 可以先用 WPS 提供的测试文档测试
