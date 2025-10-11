import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const projectRoot = path.resolve(__dirname, '..');
const distDir = path.join(projectRoot, 'dist');
const androidAssetsDir = path.join(projectRoot, 'android-shell', 'app', 'src', 'main', 'assets');

console.log('📦 开始同步构建文件到 Android...');
console.log('源目录:', distDir);
console.log('目标目录:', androidAssetsDir);

// 检查 dist 目录是否存在
if (!fs.existsSync(distDir)) {
  console.error('❌ 错误: dist 目录不存在，请先运行 npm run build');
  process.exit(1);
}

// 确保 Android assets 目录存在
if (!fs.existsSync(androidAssetsDir)) {
  console.log('创建 Android assets 目录...');
  fs.mkdirSync(androidAssetsDir, { recursive: true });
}

// 递归复制目录
function copyDir(src, dest) {
  if (!fs.existsSync(dest)) {
    fs.mkdirSync(dest, { recursive: true });
  }

  const entries = fs.readdirSync(src, { withFileTypes: true });

  for (const entry of entries) {
    const srcPath = path.join(src, entry.name);
    const destPath = path.join(dest, entry.name);

    if (entry.isDirectory()) {
      copyDir(srcPath, destPath);
    } else {
      fs.copyFileSync(srcPath, destPath);
      console.log(`  ✓ 复制: ${entry.name}`);
    }
  }
}

// 清理旧文件（保留 file-test.html 等测试文件）
console.log('\n🧹 清理旧的构建文件...');
const filesToKeep = ['file-test.html', 'link-test.html', 'main.html'];
const entries = fs.readdirSync(androidAssetsDir);

for (const entry of entries) {
  if (!filesToKeep.includes(entry)) {
    const fullPath = path.join(androidAssetsDir, entry);
    if (fs.statSync(fullPath).isDirectory()) {
      fs.rmSync(fullPath, { recursive: true, force: true });
      console.log(`  🗑️  删除目录: ${entry}`);
    } else if (entry.endsWith('.html') || entry.endsWith('.js') || entry.endsWith('.css')) {
      fs.unlinkSync(fullPath);
      console.log(`  🗑️  删除文件: ${entry}`);
    }
  }
}

// 复制 dist 目录内容
console.log('\n📋 复制新的构建文件...');
copyDir(distDir, androidAssetsDir);

// 读取 index.html 内容并创建 gpx-viewer.html
const indexHtmlPath = path.join(androidAssetsDir, 'index.html');
const gpxViewerHtmlPath = path.join(androidAssetsDir, 'gpx-viewer.html');

if (fs.existsSync(indexHtmlPath)) {
  let htmlContent = fs.readFileSync(indexHtmlPath, 'utf-8');
  
  // 修改 HTML，添加 Android 适配提示
  htmlContent = htmlContent.replace(
    '<head>',
    `<head>
    <!-- GPX Viewer - Android WebView Compatible -->
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">`
  );
  
  fs.writeFileSync(gpxViewerHtmlPath, htmlContent);
  console.log('  ✓ 创建: gpx-viewer.html');
}

 
console.log('\n✅ 同步完成！');
console.log(`\n💡 提示: 在 Android 中加载 gpx-viewer.html 即可使用`);
console.log(`   路径: file:///android_asset/gpx-viewer.html\n`);

