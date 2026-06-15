import * as echarts from "echarts/core";
import {
  BarChart,
  GaugeChart,
  LineChart,
  PieChart,
  RadarChart,
} from "echarts/charts";
import {
  GraphicComponent,
  GridComponent,
  LegendComponent,
  TitleComponent,
  TooltipComponent,
} from "echarts/components";
import { LegacyGridContainLabel } from "echarts/features";
import { CanvasRenderer } from "echarts/renderers";

echarts.use([
  BarChart,
  GaugeChart,
  LineChart,
  PieChart,
  RadarChart,
  GraphicComponent,
  GridComponent,
  LegendComponent,
  TitleComponent,
  TooltipComponent,
  LegacyGridContainLabel,
  CanvasRenderer,
]);

export const CHART_PALETTE = [
  "#4f46e5",
  "#0ea5e9",
  "#10b981",
  "#f59e0b",
  "#ef4444",
  "#8b5cf6",
  "#ec4899",
  "#14b8a6",
  "#f97316",
  "#6366f1",
  "#84cc16",
  "#06b6d4",
];

export function paletteColor(index) {
  return CHART_PALETTE[index % CHART_PALETTE.length];
}

export const CHART_TEXT = "#1a2332";
export const CHART_MUTED = "#6b7280";
export const CHART_GRID = "#e2e8f0";
export const CHART_SURFACE = "#ffffff";

const BASE_ANIMATION = {
  animation: true,
  animationDuration: 480,
  animationEasing: "cubicOut",
  animationDurationUpdate: 320,
};

export function initCampusChart(el) {
  if (!el) {
    return null;
  }
  return echarts.init(el, null, { renderer: "canvas" });
}

export function disposeCampusChart(chart) {
  chart?.dispose();
}

export function baseChartOption(partial = {}) {
  return {
    ...BASE_ANIMATION,
    textStyle: {
      fontFamily:
        '"Helvetica Neue", Helvetica, "PingFang SC", "Microsoft YaHei", Arial, sans-serif',
      color: CHART_TEXT,
    },
    ...partial,
  };
}

export function baseTooltip() {
  return {
    trigger: "axis",
    backgroundColor: "rgba(17, 24, 39, 0.94)",
    borderWidth: 0,
    padding: [8, 10],
    textStyle: { color: "#f8fafc", fontSize: 12 },
    axisPointer: {
      type: "shadow",
      shadowStyle: { color: "rgba(29, 78, 216, 0.08)" },
    },
  };
}

export function pieTooltip() {
  return {
    trigger: "item",
    backgroundColor: "rgba(17, 24, 39, 0.94)",
    borderWidth: 0,
    padding: [8, 10],
    textStyle: { color: "#f8fafc", fontSize: 12 },
  };
}

export function barGradient(colorStart, colorEnd) {
  return {
    type: "linear",
    x: 0,
    y: 0,
    x2: 1,
    y2: 0,
    colorStops: [
      { offset: 0, color: colorStart },
      { offset: 1, color: colorEnd },
    ],
  };
}

export function verticalBarGradient() {
  return {
    type: "linear",
    x: 0,
    y: 0,
    x2: 0,
    y2: 1,
    colorStops: [
      { offset: 0, color: "#3b82f6" },
      { offset: 1, color: "#1d4ed8" },
    ],
  };
}
