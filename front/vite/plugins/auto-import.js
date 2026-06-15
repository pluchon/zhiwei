import autoImport from "unplugin-auto-import/vite";

export default function createAutoImport() {
  return autoImport({
    imports: [
      "vue",
      "vue-router",
      "pinia",
      {
        "element-plus": ["ElMessage", "ElMessageBox", "ElNotification"],
        "@/utils/dict": ["useDict"],
        "@/utils/ruoyi": ["selectDictLabel"],
      },
    ],
    dts: false,
  });
}
