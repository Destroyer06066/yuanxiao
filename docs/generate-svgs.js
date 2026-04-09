const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const mdPath = path.join(__dirname, 'supplement-flow.md');
const outputDir = path.join(__dirname, 'svg');

if (!fs.existsSync(outputDir)) {
  fs.mkdirSync(outputDir, { recursive: true });
}

const mdContent = fs.readFileSync(mdPath, 'utf-8');

// 提取所有 mermaid 代码块
const regex = /```mermaid\n([\s\S]*?)```/g;
let match;
let index = 0;

const diagrams = [];

while ((match = regex.exec(mdContent)) !== null) {
  const mermaidCode = match[1].trim();

  // 生成图表名称
  let name = `diagram-${index + 1}`;

  // 尝试从上下文提取标题
  const prevLines = mdContent.substring(0, match.index).split('\n').reverse().slice(0, 5).reverse().join('\n');
  const titleMatch = prevLines.match(/#+\s*(.+)/);
  if (titleMatch) {
    name = titleMatch[1].trim().replace(/[^a-zA-Z0-9\u4e00-\u9fa5]/g, '-').replace(/-+/g, '-');
  }

  diagrams.push({ name, code: mermaidCode, index: index + 1 });
  index++;
}

console.log(`Found ${diagrams.length} diagrams\n`);

// 生成每个图表的 SVG
diagrams.forEach((diag, i) => {
  const mmdPath = path.join(outputDir, `temp-${i + 1}.mmd`);
  const svgPath = path.join(outputDir, `${diag.index}-${diag.name}.svg`);

  try {
    // 写入临时 mermaid 文件
    fs.writeFileSync(mmdPath, diag.code);

    // 执行 mmdc 生成 SVG
    execSync(`mmdc -i "${mmdPath}" -o "${svgPath}" -b white -w 1200`, { stdio: 'inherit' });

    console.log(`✓ Generated: ${path.basename(svgPath)}`);
  } catch (err) {
    console.error(`✗ Failed to generate ${diag.name}:`, err.message);
  } finally {
    // 清理临时文件
    if (fs.existsSync(mmdPath)) {
      fs.unlinkSync(mmdPath);
    }
  }
});

console.log(`\nSVG files saved to: ${outputDir}`);
