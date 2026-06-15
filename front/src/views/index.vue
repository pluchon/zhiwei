<template>
  <div class="dashboard-page" :class="dashboardPageClass">
    <template v-if="isAdmin">
      <div class="dashboard-toolbar">
        <el-radio-group v-model="rangeDays" @change="onRangeChange">
          <el-radio-button
            v-for="item in rangeOptions"
            :key="item.value"
            :value="item.value"
          >{{ item.label }}</el-radio-button>
        </el-radio-group>
      </div>

      <el-row :gutter="12" v-loading="loading" class="kpi-row">
        <el-col
          v-for="item in dashboardCards"
          :key="item.title"
          :xs="24"
          :sm="12"
          :md="8"
          :xl="6"
        >
          <article class="kpi-card" @click="openCard(item)">
            <div class="kpi-card__label">{{ item.title }}</div>
            <div class="kpi-card__value">{{ item.value }}</div>
          </article>
        </el-col>
      </el-row>

      <el-row :gutter="12" v-loading="loading" class="chart-row">
        <el-col :xs="24" :lg="12">
          <el-card shadow="never" class="chart-card">
            <template #header>
              <div class="chart-card__head">
                <div class="chart-card__title">
                  <b>当前工单状态分布</b>
                  <el-tooltip content="统计当前进行中与各状态工单数量，图例对应各状态占比" placement="top">
                    <span class="chart-tag">实时快照</span>
                  </el-tooltip>
                </div>
              </div>
            </template>
            <CampusChart :option="statusChartOption" height="340px" />
          </el-card>
        </el-col>
        <el-col :xs="24" :lg="12">
          <el-card shadow="never" class="chart-card">
            <template #header>
              <div class="chart-card__head">
                <div class="chart-card__title">
                  <b>故障类型分布</b>
                  <el-tooltip :content="`统计最近 ${rangeDays} 天内各故障类型的报修占比`" placement="top">
                    <span class="chart-tag">最近 {{ rangeDays }} 天</span>
                  </el-tooltip>
                </div>
              </div>
            </template>
            <CampusChart :option="faultTypeChartOption" height="340px" />
          </el-card>
        </el-col>
        <el-col :xs="24" :lg="12">
          <el-card shadow="never" class="chart-card">
            <template #header>
              <div class="chart-card__head">
                <div class="chart-card__title">
                  <b>校区分布</b>
                  <el-tooltip :content="`统计最近 ${rangeDays} 天内各校区工单数量，柱色区分不同校区`" placement="top">
                    <span class="chart-tag">最近 {{ rangeDays }} 天</span>
                  </el-tooltip>
                </div>
              </div>
            </template>
            <CampusChart :option="campusChartOption" height="340px" />
          </el-card>
        </el-col>
        <el-col :xs="24" :lg="12">
          <el-card shadow="never" class="chart-card">
            <template #header>
              <div class="chart-card__head">
                <div class="chart-card__title">
                  <b>楼栋分布</b>
                  <el-tooltip :content="`统计最近 ${rangeDays} 天内报修较多的楼栋，图例对应各楼栋工单数`" placement="top">
                    <span class="chart-tag">最近 {{ rangeDays }} 天</span>
                  </el-tooltip>
                </div>
              </div>
            </template>
            <CampusChart :option="buildingChartOption" height="340px" />
          </el-card>
        </el-col>
      </el-row>
    </template>

    <template v-else-if="isReporter">
      <div class="reporter-toolbar">
        <h2 class="reporter-greeting">{{ greetingText }}</h2>
        <el-radio-group v-model="rangeDays" @change="onRangeChange">
          <el-radio-button
            v-for="item in rangeOptions"
            :key="item.value"
            :value="item.value"
          >{{ item.label }}</el-radio-button>
        </el-radio-group>
      </div>

      <div v-loading="loading" class="reporter-kpi-grid">
        <article
          v-for="item in reporterCards"
          :key="item.title"
          class="kpi-card reporter-kpi-card"
          :class="item.tone ? `reporter-kpi-card--${item.tone}` : ''"
          @click="item.action?.()"
        >
          <div class="kpi-card__label">{{ item.title }}</div>
          <div class="kpi-card__value">{{ item.value }}</div>
          <p class="reporter-kpi-card__hint">{{ item.hint }}</p>
        </article>
      </div>

      <el-row :gutter="12" v-loading="loading" class="chart-row reporter-chart-row">
        <el-col :xs="24" :lg="8">
          <el-card shadow="never" class="chart-card reporter-chart-card">
            <template #header>
              <div class="chart-card__head">
                <div class="chart-card__title">
                  <b>报修状态</b>
                  <span class="chart-tag chart-tag--reporter">我的工单</span>
                </div>
              </div>
            </template>
            <CampusChart :option="reporterStatusChartOption" height="300px" />
          </el-card>
        </el-col>
        <el-col :xs="24" :lg="8">
          <el-card shadow="never" class="chart-card reporter-chart-card">
            <template #header>
              <div class="chart-card__head">
                <div class="chart-card__title">
                  <b>报修类型</b>
                  <span class="chart-tag chart-tag--reporter">最近 {{ rangeDays }} 天</span>
                </div>
              </div>
            </template>
            <CampusChart :option="reporterFaultChartOption" height="300px" />
          </el-card>
        </el-col>
        <el-col :xs="24" :lg="8">
          <el-card shadow="never" class="chart-card reporter-chart-card">
            <template #header>
              <div class="chart-card__head">
                <div class="chart-card__title">
                  <b>提交趋势</b>
                  <span class="chart-tag chart-tag--reporter">最近 {{ rangeDays }} 天</span>
                </div>
              </div>
            </template>
            <CampusChart :option="reporterTrendChartOption" height="300px" />
          </el-card>
        </el-col>
      </el-row>

      <el-card shadow="never" class="chart-card reporter-recent-card">
        <template #header>
          <div class="chart-card__head">
            <b>最近报修</b>
            <el-button link type="primary" @click="openReporterList()">查看全部</el-button>
          </div>
        </template>
        <div v-if="reporterDashboard?.recentOrders?.length" class="recent-order-list">
          <button
            v-for="item in reporterDashboard.recentOrders"
            :key="item.orderId"
            type="button"
            class="recent-order-item"
            @click="openOrderDetail(item.orderId)"
          >
            <div class="recent-order-item__main">
              <strong>{{ item.title || "未命名报修" }}</strong>
              <span>{{ formatRecentTime(item.createTime) }}</span>
            </div>
            <span class="recent-order-item__status">{{ item.statusLabel }}</span>
          </button>
        </div>
        <div v-else class="recent-order-empty">
          <p>还没有报修记录</p>
          <el-button type="primary" @click="openRepairCreate">立即提交报修</el-button>
        </div>
      </el-card>
    </template>

    <template v-else-if="isRepairer">
      <div class="reporter-toolbar repairer-toolbar">
        <div class="repairer-toolbar__main">
          <h2 class="reporter-greeting">{{ repairerGreetingText }}</h2>
          <span
            class="repairer-state-tag"
            :class="repairerStateTagClass"
          >
            {{ repairerAcceptingLabel }}
          </span>
        </div>
        <el-radio-group v-model="rangeDays" @change="onRangeChange">
          <el-radio-button
            v-for="item in rangeOptions"
            :key="item.value"
            :value="item.value"
          >{{ item.label }}</el-radio-button>
        </el-radio-group>
      </div>

      <div v-loading="loading" class="reporter-kpi-grid">
        <article
          v-for="item in repairerCards"
          :key="item.title"
          class="kpi-card reporter-kpi-card"
          :class="item.tone ? `reporter-kpi-card--${item.tone}` : ''"
          @click="item.action?.()"
        >
          <div class="kpi-card__label">{{ item.title }}</div>
          <div class="kpi-card__value">{{ item.value }}</div>
        </article>
      </div>

      <el-row :gutter="12" v-loading="loading" class="chart-row reporter-chart-row">
        <el-col :xs="24" :lg="8">
          <el-card shadow="never" class="chart-card reporter-chart-card">
            <template #header>
              <div class="chart-card__head">
                <div class="chart-card__title">
                  <b>工单状态</b>
                  <span class="chart-tag chart-tag--repairer">我的工单</span>
                </div>
              </div>
            </template>
            <CampusChart :option="repairerStatusChartOption" height="300px" />
          </el-card>
        </el-col>
        <el-col :xs="24" :lg="8">
          <el-card shadow="never" class="chart-card reporter-chart-card">
            <template #header>
              <div class="chart-card__head">
                <div class="chart-card__title">
                  <b>维修类型</b>
                  <span class="chart-tag chart-tag--repairer">最近 {{ rangeDays }} 天</span>
                </div>
              </div>
            </template>
            <CampusChart :option="repairerFaultChartOption" height="300px" />
          </el-card>
        </el-col>
        <el-col :xs="24" :lg="8">
          <el-card shadow="never" class="chart-card reporter-chart-card">
            <template #header>
              <div class="chart-card__head">
                <div class="chart-card__title">
                  <b>完成趋势</b>
                  <span class="chart-tag chart-tag--repairer">最近 {{ rangeDays }} 天</span>
                </div>
              </div>
            </template>
            <CampusChart :option="repairerTrendChartOption" height="300px" />
          </el-card>
        </el-col>
      </el-row>

      <el-card shadow="never" class="chart-card reporter-recent-card">
        <template #header>
          <div class="chart-card__head">
            <b>最近工单</b>
            <el-button link type="primary" @click="openRepairerList()">查看全部</el-button>
          </div>
        </template>
        <div v-if="repairerDashboard?.recentOrders?.length" class="recent-order-list">
          <button
            v-for="item in repairerDashboard.recentOrders"
            :key="item.orderId"
            type="button"
            class="recent-order-item recent-order-item--repairer"
            @click="openOrderDetail(item.orderId)"
          >
            <div class="recent-order-item__main">
              <strong>{{ item.title || "未命名工单" }}</strong>
              <span>{{ formatRecentTime(item.createTime) }}</span>
            </div>
            <span class="recent-order-item__status">{{ item.statusLabel }}</span>
          </button>
        </div>
        <div v-else class="recent-order-empty">
          <p>还没有接手的维修工单</p>
          <el-button type="primary" @click="openAvailableOrders">前往待接工单</el-button>
        </div>
      </el-card>
    </template>
  </div>
</template>

<script setup src="./index.js"></script>
<style scoped lang="scss" src="./index.scss"></style>
