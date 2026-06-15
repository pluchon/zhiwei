import fs from "node:fs";
import path from "node:path";

/**
 * Vue 官方编译器不允许 <script setup> 直接使用 src 属性。
 *
 * 项目源码要求脚本与 Vue 模板分离，因此在 Vue 插件解析前，
 * 将外部脚本内容临时注入 SFC。该转换仅发生在开发和构建阶段，
 * 不会修改磁盘上的三文件结构。
 */
export default function createExternalScript() {
  return {
    name: "campus-external-vue-script",
    enforce: "pre",
    transform(source, id) {
      if (!id.endsWith(".vue")) {
        return null;
      }

      const pattern =
        /<script(?<before>[^>]*)src=["'](?<src>.+?)["'](?<after>[^>]*)><\/script>/;
      const match = source.match(pattern);

      if (!match) {
        return null;
      }

      const scriptPath = path.resolve(path.dirname(id), match.groups.src);
      this.addWatchFile(scriptPath);
      const script = fs.readFileSync(scriptPath, "utf-8");
      const attributes = `${match.groups.before}${match.groups.after}`.trim();
      const openingTag = attributes ? `<script ${attributes}>` : "<script>";
      const injectedScript = `${openingTag}\n${script}\n</script>`;

      return {
        // 使用函数式替换，避免外部脚本中的 $&、$' 等内容被当作替换标记。
        code: source.replace(pattern, () => injectedScript),
        map: null,
      };
    },
  };
}
