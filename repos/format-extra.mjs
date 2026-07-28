// 用独立库格式化 Prettier 不支持的文件：pom.xml(XML) 与 *.sql
// 运行：node format-extra.mjs
import { readFileSync, writeFileSync, readdirSync, statSync } from "node:fs";
import { join, dirname } from "node:path";
import { fileURLToPath } from "node:url";
import { format as formatSql } from "sql-formatter";

const root = join(dirname(fileURLToPath(import.meta.url)), "backend");

const sqlFiles = [];
const xmlFiles = [];

function walk(dir) {
  for (const name of readdirSync(dir)) {
    const p = join(dir, name);
    const st = statSync(p);
    if (st.isDirectory()) {
      if (name === "target" || name === "node_modules") continue;
      walk(p);
    } else if (name.endsWith(".sql")) {
      sqlFiles.push(p);
    } else if (name === "pom.xml") {
      xmlFiles.push(p);
    }
  }
}
walk(root);

for (const f of sqlFiles) {
  const src = readFileSync(f, "utf8");
  const out = formatSql(src, { language: "postgresql", tabWidth: 2 });
  writeFileSync(f, out);
  console.log("SQL formatted:", f);
}

// Maven 风格 XML 格式化：纯文本节点内联，含子元素的节点按层级缩进。
// 纯文本分词实现，不依赖第三方 XML 解析器。
function formatMavenXml(xml) {
  const indentUnit = "  ";
  const lines = [];
  let level = 0;
  // 开放元素栈：{ line: 开始标签所在行号, hasChild: 是否包含子元素 }
  const stack = [];

  const text = xml.replace(/\r\n/g, "\n");
  const tokenRe =
    /(<!--[\s\S]*?-->)|(<\?[\s\S]*?\?>)|(<\/?[a-zA-Z][^>]*?\/>)|(<\/?[a-zA-Z][^>]*?>)|([^<]+)/g;
  let m;
  while ((m = tokenRe.exec(text)) !== null) {
    const [full, comment, declaration, selfClose, openClose, textNode] = m;
    if (comment) {
      lines.push(indentUnit.repeat(level) + comment.trim());
      if (stack.length) stack[stack.length - 1].hasChild = true;
    } else if (declaration) {
      lines.push(declaration.trim());
    } else if (selfClose) {
      lines.push(indentUnit.repeat(level) + selfClose.trim());
      if (stack.length) stack[stack.length - 1].hasChild = true;
    } else if (openClose) {
      const isClose = openClose.startsWith("</");
      if (isClose) {
        level = Math.max(0, level - 1);
        const top = stack.pop();
        if (top && !top.hasChild) {
          // 无子元素：把结束标签拼回开始行，形成 <tag>text</tag>
          lines[top.line] = lines[top.line] + openClose.trim();
        } else {
          lines.push(indentUnit.repeat(level) + openClose.trim());
        }
      } else {
        const line = indentUnit.repeat(level) + openClose.trim();
        lines.push(line);
        stack.push({ line: lines.length - 1, hasChild: false });
        if (stack.length >= 2) stack[stack.length - 2].hasChild = true;
        level += 1;
      }
    } else if (textNode) {
      const trimmed = textNode.trim();
      if (trimmed && stack.length) {
        const top = stack[stack.length - 1];
        if (!top.hasChild) {
          lines[top.line] = lines[top.line] + trimmed;
        }
      }
    }
  }

  return lines.join("\n").replace(/\n+$/, "\n");
}

for (const f of xmlFiles) {
  const src = readFileSync(f, "utf8");
  const out = formatMavenXml(src);
  writeFileSync(f, out);
  console.log("XML formatted:", f);
}

console.log(
  `Done. SQL: ${sqlFiles.length} file(s), XML: ${xmlFiles.length} file(s).`,
);
