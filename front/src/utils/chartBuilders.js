import {
  CHART_GRID,
  CHART_MUTED,
  CHART_PALETTE,
  CHART_SURFACE,
  CHART_TEXT,
  baseChartOption,
  baseTooltip,
  paletteColor,
  pieTooltip,
} from "@/utils/campusChart";

function emptyOption(title) {
  return baseChartOption({
    title: {
      text: title,
      left: "center",
      top: "middle",
      textStyle: { color: CHART_MUTED, fontSize: 13, fontWeight: 400 },
    },
  });
}

function mapDistribution(items = []) {
  return items.map((item) => ({
    name: item.name,
    value: Number(item.count) || 0,
  }));
}

function legendConfig(names, position = "bottom", options = {}) {
  if (position === "right") {
    const largeLegend = options.legendNameOnly === true;
    return {
      type: "scroll",
      orient: "vertical",
      right: largeLegend ? 8 : 4,
      top: "middle",
      icon: "roundRect",
      itemWidth: largeLegend ? 14 : 10,
      itemHeight: largeLegend ? 14 : 10,
      itemGap: largeLegend ? 14 : 10,
      textStyle: {
        color: CHART_TEXT,
        fontSize: largeLegend ? 13 : 11,
        lineHeight: largeLegend ? 20 : 16,
      },
      data: names,
    };
  }
  return categoryLegend(names);
}

function categoryLegend(names) {
  return {
    type: names.length > 6 ? "scroll" : "plain",
    bottom: 0,
    left: "center",
    selectedMode: false,
    icon: "roundRect",
    itemWidth: 10,
    itemHeight: 10,
    itemGap: 12,
    textStyle: { color: CHART_TEXT, fontSize: 11 },
    data: names.map((name, index) => ({
      name,
      itemStyle: { color: paletteColor(index) },
    })),
  };
}

function barLayout(count, orientation = "vertical") {
  if (orientation === "horizontal") {
    return {
      barMaxWidth: count <= 3 ? 22 : 16,
      barCategoryGap: count <= 3 ? "28%" : "18%",
    };
  }
  return {
    barMaxWidth: count <= 2 ? 80 : count <= 4 ? 52 : 36,
    barCategoryGap: count <= 2 ? "32%" : count <= 4 ? "24%" : "18%",
  };
}

function buildColoredBarSeries(title, names, values, orientation = "vertical") {
  const layout = barLayout(names.length, orientation);
  const series = {
    name: title,
    type: "bar",
    ...layout,
    data: values.map((value, index) => ({
      name: names[index],
      value,
      itemStyle: {
        color: paletteColor(index),
        borderRadius: orientation === "vertical" ? [4, 4, 0, 0] : [0, 4, 4, 0],
      },
    })),
    emphasis: { focus: "self" },
  };
  if (orientation === "horizontal") {
    series.label = {
      show: true,
      position: "right",
      color: CHART_MUTED,
      fontSize: 11,
      distance: 6,
    };
  }
  return [series];
}

function axisCountTooltip(unit = "工单数") {
  return {
    ...baseTooltip(),
    formatter: (params) => {
      const row = Array.isArray(params) ? params[0] : params;
      return `${row.name}<br/>${unit}：${row.value}`;
    },
  };
}

export function buildHorizontalBarOption(title, items = [], { maxItems = 10 } = {}) {
  const data = [...items]
    .sort((a, b) => (b.count || 0) - (a.count || 0))
    .slice(0, maxItems);
  if (!data.length) {
    return emptyOption("暂无数据");
  }
  const names = data.map((item) => item.name).reverse();
  const values = data.map((item) => item.count || 0).reverse();
  const legendBottom = names.length > 4 ? 40 : 32;
  return baseChartOption({
    color: CHART_PALETTE,
    tooltip: axisCountTooltip(),
    legend: categoryLegend(names),
    grid: { left: 12, right: 20, top: 12, bottom: legendBottom, containLabel: true },
    xAxis: {
      type: "value",
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: CHART_GRID, type: "dashed" } },
      axisLabel: { color: CHART_MUTED, fontSize: 11 },
    },
    yAxis: {
      type: "category",
      data: names,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: CHART_TEXT, width: 108, overflow: "truncate", fontSize: 11 },
    },
    series: buildColoredBarSeries(title, names, values, "horizontal"),
  });
}

export function buildVerticalBarOption(title, items = []) {
  const data = mapDistribution(items);
  if (!data.length) {
    return emptyOption("暂无数据");
  }
  const names = data.map((item) => item.name);
  const values = data.map((item) => item.value);
  const legendBottom = names.length > 4 ? 44 : 32;
  return baseChartOption({
    color: CHART_PALETTE,
    tooltip: axisCountTooltip(),
    legend: categoryLegend(names),
    grid: { left: 16, right: 16, top: 16, bottom: legendBottom, containLabel: true },
    xAxis: {
      type: "category",
      data: names,
      axisLine: { lineStyle: { color: CHART_GRID } },
      axisTick: { show: false },
      axisLabel: {
        color: CHART_TEXT,
        interval: 0,
        rotate: names.length > 4 ? 24 : 0,
        fontSize: 11,
      },
    },
    yAxis: {
      type: "value",
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: CHART_GRID, type: "dashed" } },
      axisLabel: { color: CHART_MUTED, fontSize: 11 },
    },
    series: buildColoredBarSeries(title, names, values, "vertical"),
  });
}

function buildDonutCenterGraphic(centerX, centerY, total) {
  return [
    {
      type: "group",
      left: centerX,
      top: centerY,
      bounding: "raw",
      z: 10,
      silent: true,
      children: [
        {
          type: "text",
          style: {
            text: String(total),
            fontSize: 22,
            fontWeight: 600,
            fill: CHART_TEXT,
            textAlign: "center",
            textVerticalAlign: "middle",
          },
          x: 0,
          y: -10,
        },
        {
          type: "text",
          style: {
            text: "合计",
            fontSize: 11,
            fill: CHART_MUTED,
            textAlign: "center",
            textVerticalAlign: "middle",
          },
          x: 0,
          y: 14,
        },
      ],
    },
  ];
}

export function buildDonutOption(title, items = [], options = {}) {
  const data = mapDistribution(items);
  if (!data.length) {
    return emptyOption("暂无数据");
  }
  const total = data.reduce((sum, item) => sum + item.value, 0);
  const names = data.map((item) => item.name);
  const legendPosition = options.legendPosition || "right";
  const compact = options.compact === true;
  const useRightLegend = legendPosition === "right" && !compact;
  const legendNameOnly = options.legendNameOnly === true;
  const tooltipPercentOnly = options.tooltipPercentOnly === true;
  const center = useRightLegend ? ["34%", "50%"] : compact ? ["50%", "44%"] : ["50%", "42%"];
  const radius = useRightLegend ? ["46%", "68%"] : compact ? ["42%", "62%"] : ["48%", "70%"];
  const legendBottom = legendPosition === "bottom" || compact;
  return baseChartOption({
    color: CHART_PALETTE,
    tooltip: {
      ...pieTooltip(),
      formatter: (params) => {
        if (tooltipPercentOnly) {
          return `${params.name} ${params.percent}%`;
        }
        return `${params.name}<br/>${params.value} 单（${params.percent}%）`;
      },
    },
    legend: {
      ...legendConfig(names, legendBottom ? "bottom" : legendPosition, options),
      ...(legendNameOnly
        ? {}
        : {
            formatter: (name) => {
              const item = data.find((row) => row.name === name);
              if (!item || !total) {
                return name;
              }
              const percent = Math.round((item.value / total) * 100);
              return `${name}  ${item.value}单 ${percent}%`;
            },
          }),
    },
    graphic: buildDonutCenterGraphic(center[0], center[1], total),
    series: [
      {
        name: title,
        type: "pie",
        radius,
        center,
        avoidLabelOverlap: true,
        itemStyle: {
          borderRadius: 4,
          borderColor: CHART_SURFACE,
          borderWidth: 2,
        },
        label: { show: false },
        emphasis: {
          label: { show: false },
          scaleSize: legendNameOnly ? 4 : 6,
        },
        labelLine: { show: false },
        data,
      },
    ],
  });
}

export function buildLineTrendOption(title, items = [], { unit = "工单数" } = {}) {
  const data = mapDistribution(items);
  if (!data.length) {
    return emptyOption("暂无数据");
  }
  const names = data.map((item) => item.name);
  const values = data.map((item) => item.value);
  return baseChartOption({
    color: [CHART_PALETTE[0]],
    tooltip: axisCountTooltip(unit),
    grid: { left: 16, right: 20, top: 24, bottom: 32, containLabel: true },
    xAxis: {
      type: "category",
      data: names,
      boundaryGap: false,
      axisLine: { lineStyle: { color: CHART_GRID } },
      axisTick: { show: false },
      axisLabel: { color: CHART_MUTED, fontSize: 11 },
    },
    yAxis: {
      type: "value",
      minInterval: 1,
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: CHART_GRID, type: "dashed" } },
      axisLabel: { color: CHART_MUTED, fontSize: 11 },
    },
    series: [
      {
        name: title,
        type: "line",
        smooth: true,
        symbol: "circle",
        symbolSize: 6,
        lineStyle: { width: 2, color: CHART_PALETTE[0] },
        itemStyle: { color: CHART_PALETTE[0], borderWidth: 2, borderColor: CHART_SURFACE },
        areaStyle: {
          color: {
            type: "linear",
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: "rgba(79, 70, 229, 0.18)" },
              { offset: 1, color: "rgba(79, 70, 229, 0.02)" },
            ],
          },
        },
        data: values,
      },
    ],
  });
}

export function buildEfficiencyBarOption(efficiency = {}, options = {}) {
  const compact = options.compact === true;
  const labels = ["首次接单", "处理时长", "完成时长"];
  const values = [
    efficiency.avgFirstAcceptMinutes || 0,
    efficiency.avgProcessMinutes || 0,
    efficiency.avgCompletionMinutes || 0,
  ];
  return baseChartOption({
    color: CHART_PALETTE,
    tooltip: axisCountTooltip("时长（分钟）"),
    legend: compact ? { show: false } : categoryLegend(labels),
    grid: {
      left: 12,
      right: 12,
      top: compact ? 32 : 16,
      bottom: compact ? 52 : 36,
      containLabel: true,
    },
    xAxis: {
      type: "category",
      data: labels,
      axisLine: { lineStyle: { color: CHART_GRID } },
      axisTick: { show: false },
      axisLabel: {
        color: CHART_TEXT,
        fontSize: compact ? 10 : 11,
        interval: 0,
        margin: 10,
      },
    },
    yAxis: {
      type: "value",
      axisLine: { show: false },
      splitLine: { lineStyle: { color: CHART_GRID, type: "dashed" } },
      axisLabel: { color: CHART_MUTED, fontSize: 11 },
    },
    series: buildColoredBarSeries("维修效率", labels, values, "vertical").map((series) => ({
      ...series,
      barMaxWidth: compact ? 44 : series.barMaxWidth,
      barCategoryGap: compact ? "36%" : series.barCategoryGap,
      label: {
        show: true,
        position: "top",
        formatter: ({ value }) => `${Number(value).toFixed(1)}`,
        color: CHART_MUTED,
        fontSize: 11,
      },
    })),
  });
}

export function buildCategoryRepairOption(items = [], options = {}) {
  return buildDonutOption(
    "分类报修",
    items.map((item) => ({
      name: item.categoryName,
      count: item.repairCount,
    })),
    {
      legendPosition: "right",
      legendNameOnly: true,
      tooltipPercentOnly: true,
      ...options,
    },
  );
}

export function buildRepairerWorkloadOption(items = []) {
  if (!items.length) {
    return emptyOption("暂无师傅数据");
  }
  const names = items.map((item) => item.realName || item.userNo);
  const seriesNames = ["接单", "完成", "处理中"];
  return baseChartOption({
    color: CHART_PALETTE,
    tooltip: {
      ...baseTooltip(),
      trigger: "axis",
      axisPointer: { type: "shadow" },
    },
    legend: categoryLegend(seriesNames),
    grid: { left: 16, right: 16, top: 16, bottom: 36, containLabel: true },
    xAxis: {
      type: "category",
      data: names,
      axisLabel: {
        color: CHART_TEXT,
        interval: 0,
        rotate: names.length > 4 ? 18 : 0,
        fontSize: 11,
      },
      axisLine: { lineStyle: { color: CHART_GRID } },
      axisTick: { show: false },
    },
    yAxis: {
      type: "value",
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: CHART_GRID, type: "dashed" } },
      axisLabel: { color: CHART_MUTED, fontSize: 11 },
    },
    series: [
      {
        name: "接单",
        type: "bar",
        stack: "work",
        barMaxWidth: 24,
        data: items.map((item) => item.acceptCount || 0),
        itemStyle: { borderRadius: [0, 0, 0, 0] },
      },
      {
        name: "完成",
        type: "bar",
        stack: "work",
        barMaxWidth: 24,
        data: items.map((item) => item.completedCount || 0),
        itemStyle: { borderRadius: [0, 0, 0, 0] },
      },
      {
        name: "处理中",
        type: "bar",
        barMaxWidth: 24,
        data: items.map((item) => item.processingCount || 0),
        itemStyle: { borderRadius: [4, 4, 0, 0] },
      },
    ],
  });
}

export function buildPersonalRadarOption(stats = {}) {
  const accept = Number(stats.acceptCount) || 0;
  const completed = Number(stats.completedCount) || 0;
  const processing = Number(stats.processingCount) || 0;
  const max = Math.max(accept, completed, processing, 1);
  return baseChartOption({
    color: ["#1d4ed8"],
    tooltip: { trigger: "item" },
    radar: {
      radius: "58%",
      center: ["50%", "54%"],
      splitLine: { lineStyle: { color: CHART_GRID } },
      splitArea: { areaStyle: { color: ["#f8fafc", "#f1f5f9"] } },
      axisName: { color: CHART_TEXT, fontSize: 11 },
      axisLine: { lineStyle: { color: CHART_GRID } },
      indicator: [
        { name: "接单数", max },
        { name: "完成数", max },
        { name: "处理中", max },
        { name: "首次处理", max: Math.max(stats.avgFirstProcessMinutes || 0, 60) },
        { name: "完成时长", max: Math.max(stats.avgCompletionMinutes || 0, 60) },
      ],
    },
    series: [
      {
        type: "radar",
        data: [
          {
            value: [
              accept,
              completed,
              processing,
              stats.avgFirstProcessMinutes || 0,
              stats.avgCompletionMinutes || 0,
            ],
            areaStyle: { color: "rgba(29, 78, 216, 0.14)" },
            lineStyle: { width: 2, color: "#1d4ed8" },
            symbol: "circle",
            symbolSize: 5,
          },
        ],
      },
    ],
  });
}

export function buildCompletionGaugeOption(stats = {}) {
  const accept = Number(stats.acceptCount) || 0;
  const completed = Number(stats.completedCount) || 0;
  const rate = accept > 0 ? Math.round((completed / accept) * 100) : 0;
  return baseChartOption({
    series: [
      {
        type: "gauge",
        startAngle: 210,
        endAngle: -30,
        min: 0,
        max: 100,
        radius: "86%",
        center: ["50%", "56%"],
        progress: {
          show: true,
          width: 12,
          roundCap: true,
          itemStyle: { color: "#1d4ed8" },
        },
        axisLine: {
          lineStyle: { width: 12, color: [[1, CHART_GRID]] },
          roundCap: true,
        },
        axisTick: { show: false },
        splitLine: { show: false },
        axisLabel: { show: false },
        pointer: { show: false },
        detail: {
          valueAnimation: true,
          formatter: "{value}%",
          color: CHART_TEXT,
          fontSize: 26,
          fontWeight: 600,
          offsetCenter: [0, "8%"],
        },
        title: {
          show: true,
          offsetCenter: [0, "34%"],
          color: CHART_MUTED,
          fontSize: 12,
        },
        data: [{ value: rate, name: "完成率" }],
      },
    ],
  });
}

export function buildTopAssetBarOption(items = []) {
  return buildHorizontalBarOption(
    "维修次数",
    items.map((item) => ({ name: item.assetName || item.assetNo, count: item.repairCount })),
    { maxItems: 8 },
  );
}

export function buildAiDynamicChartOption(chart = {}) {
  const items = (chart.items || []).map((row) => ({
    name: row.name,
    count: Number(row.count) || 0,
  }));
  const type = String(chart.chartType || "BAR").toUpperCase();
  const unit = chart.unit || "工单数";
  const title = chart.title || "统计图表";
  if (!items.length) {
    return emptyOption("暂无数据");
  }
  if (type === "PIE") {
    return buildDonutOption(title, items, { legendPosition: "right" });
  }
  if (type === "LINE") {
    return buildLineTrendOption(title, items, { unit });
  }
  if (chart.horizontal) {
    return buildHorizontalBarOption(title, items);
  }
  return buildVerticalBarOption(title, items);
}

export function buildAiStatisticsPreview(result = {}) {
  if (!result) {
    return null;
  }
  if (result.charts?.length) {
    const miniCharts = result.charts.map((chart, index) => ({
      key: chart.dimension || `chart-${index}`,
      label: chart.title,
      rangeLabel: chart.rangeLabel,
      option: buildAiDynamicChartOption(chart),
    }));
    return {
      type: "dynamic",
      summary: result.summary || "",
      kpi: [],
      miniCharts: miniCharts.slice(0, 1),
    };
  }
  if (result.overview) {
    const overview = result.overview;
    const efficiency = overview.repairEfficiency || {};
    return {
      type: "admin",
      summary: result.summary || "",
      kpi: [
        { label: "已完成", value: efficiency.completedCount ?? 0 },
        { label: "超7天完成", value: efficiency.overSevenDaysCount ?? 0 },
        { label: "未完成", value: efficiency.unfinishedCount ?? 0 },
      ],
      miniCharts: [
        {
          key: "efficiency",
          label: "维修效率",
          option: buildMiniEfficiencyPreview(efficiency),
        },
        {
          key: "category",
          label: "分类占比",
          option: buildMiniCategoryPreview(overview.assetCategoryRepairs || []),
        },
      ],
    };
  }
  if (result.personal) {
    const personal = result.personal;
    const total = Number(personal.acceptCount || 0);
    const completed = Number(personal.completedCount || 0);
    const rate = total > 0 ? Math.round((completed / total) * 100) : 0;
    return {
      type: "personal",
      summary: result.summary || "",
      kpi: [
        { label: "接单", value: personal.acceptCount ?? 0 },
        { label: "完成", value: personal.completedCount ?? 0 },
        { label: "处理中", value: personal.processingCount ?? 0 },
      ],
      miniCharts: [
        {
          key: "radar",
          label: "工作概览",
          option: buildMiniRadarPreview(personal),
        },
        {
          key: "gauge",
          label: "完成率",
          option: buildMiniGaugePreview(rate),
        },
      ],
    };
  }
  return null;
}

export function buildAiStatisticsDetailCharts(result = {}) {
  if (!result) {
    return [];
  }
  if (result.charts?.length) {
    return result.charts.map((chart) => ({
      title: chart.title,
      height: "340px",
      span: 24,
      option: buildAiDynamicChartOption(chart),
    }));
  }
  if (result.overview) {
    const overview = result.overview;
    return [
      {
        title: "维修效率（分钟）",
        height: "340px",
        span: 12,
        option: buildEfficiencyBarOption(overview.repairEfficiency || {}),
      },
      {
        title: "资产分类报修占比",
        height: "340px",
        span: 12,
        option: buildCategoryRepairOption(overview.assetCategoryRepairs || [], {
          legendPosition: "right",
        }),
      },
      {
        title: "未完成工单趋势",
        height: "340px",
        span: 24,
        option: buildLineTrendOption("未完成工单", overview.unfinishedOrderTrend || []),
      },
      {
        title: "维修次数较多资产 TOP",
        height: "340px",
        span: 24,
        option: buildTopAssetBarOption(overview.topRepairedAssets || []),
      },
    ];
  }
  if (result.personal) {
    return [
      {
        title: "工作概览",
        height: "340px",
        span: 12,
        option: buildPersonalRadarOption(result.personal),
      },
      {
        title: "完成率",
        height: "340px",
        span: 12,
        option: buildCompletionGaugeOption(result.personal),
      },
    ];
  }
  return [];
}

function buildMiniEfficiencyPreview(efficiency = {}) {
  const values = [
    efficiency.avgFirstAcceptMinutes || 0,
    efficiency.avgProcessMinutes || 0,
    efficiency.avgCompletionMinutes || 0,
  ];
  return baseChartOption({
    animation: false,
    grid: { left: 6, right: 6, top: 6, bottom: 6, containLabel: false },
    xAxis: { type: "category", show: false, data: ["A", "B", "C"] },
    yAxis: { type: "value", show: false },
    series: [
      {
        type: "bar",
        barWidth: 14,
        data: values.map((value, index) => ({
          value,
          itemStyle: {
            color: paletteColor(index),
            borderRadius: [3, 3, 0, 0],
          },
        })),
      },
    ],
  });
}

function buildMiniCategoryPreview(items = []) {
  const data = mapDistribution(items);
  if (!data.length) {
    return emptyOption("");
  }
  return baseChartOption({
    animation: false,
    tooltip: { show: false },
    legend: { show: false },
    series: [
      {
        type: "pie",
        radius: ["50%", "78%"],
        center: ["50%", "50%"],
        label: { show: false },
        labelLine: { show: false },
        data,
      },
    ],
  });
}

function buildMiniRadarPreview(personal = {}) {
  const indicators = [
    { name: "接单", max: Math.max(Number(personal.acceptCount || 0), 1) * 1.2 },
    { name: "完成", max: Math.max(Number(personal.completedCount || 0), 1) * 1.2 },
    { name: "处理中", max: Math.max(Number(personal.processingCount || 0), 1) * 1.2 },
  ];
  const values = [
    Number(personal.acceptCount || 0),
    Number(personal.completedCount || 0),
    Number(personal.processingCount || 0),
  ];
  return baseChartOption({
    animation: false,
    radar: {
      center: ["50%", "52%"],
      radius: "62%",
      indicator: indicators,
      axisName: { show: false },
      splitLine: { lineStyle: { color: CHART_GRID } },
      splitArea: { show: false },
      axisLine: { lineStyle: { color: CHART_GRID } },
    },
    series: [
      {
        type: "radar",
        symbol: "none",
        lineStyle: { width: 1, color: CHART_PALETTE[0] },
        areaStyle: { color: "rgba(37, 99, 235, 0.18)" },
        data: [{ value: values }],
      },
    ],
  });
}

function buildMiniGaugePreview(rate = 0) {
  return baseChartOption({
    animation: false,
    series: [
      {
        type: "gauge",
        startAngle: 90,
        endAngle: -270,
        min: 0,
        max: 100,
        radius: "86%",
        center: ["50%", "50%"],
        progress: {
          show: true,
          width: 8,
          itemStyle: { color: CHART_PALETTE[0] },
        },
        axisLine: { lineStyle: { width: 8, color: [[1, CHART_GRID]] } },
        axisTick: { show: false },
        splitLine: { show: false },
        axisLabel: { show: false },
        pointer: { show: false },
        detail: {
          valueAnimation: false,
          formatter: "{value}%",
          color: CHART_TEXT,
          fontSize: 14,
          offsetCenter: [0, 0],
        },
        data: [{ value: rate }],
      },
    ],
  });
}

/** @deprecated 使用 buildAiStatisticsPreview + buildAiStatisticsDetailCharts */
export function buildAiStatisticsCharts(result = {}) {
  return buildAiStatisticsDetailCharts(result);
}
