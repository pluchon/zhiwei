<template>
  <div
    id="tags-view-container"
    class="tags-view-container"
    :class="{ 'tags-view-container--chrome': tagsViewStyle === 'chrome' }"
  >
    <!-- 左切换箭头 -->
    <span
      class="tags-nav-btn tags-nav-btn--left"
      :class="{ disabled: !canScrollLeft }"
      @click="scrollLeft"
    >
      <el-icon><arrow-left /></el-icon>
    </span>

    <!-- 标签滚动区 -->
    <scroll-pane
      ref="scrollPaneRef"
      class="tags-view-wrapper"
      @scroll="handleScroll"
      @update-arrows="updateArrowState"
    >
      <router-link
        v-for="tag in visitedViews"
        :key="tag.path"
        :data-path="tag.path"
        :class="{ active: isActive(tag), 'has-icon': tagsIcon }"
        :to="{ path: tag.path, query: tag.query, fullPath: tag.fullPath }"
        class="tags-view-item"
        :style="tagActiveStyle(tag)"
        @click.middle="!isAffix(tag) ? closeSelectedTag(tag) : ''"
        @contextmenu.prevent="openMenu(tag, $event)"
      >
        <svg-icon
          v-if="tagsIcon && tag.meta && tag.meta.icon && tag.meta.icon !== '#'"
          :icon-class="tag.meta.icon"
          style="margin-right: 3px"
        />
        {{ tag.title }}
        <span
          v-if="!isAffix(tag)"
          @click.prevent.stop="closeSelectedTag(tag)"
          class="tags-close-btn"
        >
          <close class="el-icon-close" />
        </span>
      </router-link>
    </scroll-pane>

    <!-- 右切换箭头 -->
    <span
      class="tags-nav-btn tags-nav-btn--right"
      :class="{ disabled: !canScrollRight }"
      @click="scrollRight"
    >
      <el-icon><arrow-right /></el-icon>
    </span>

    <!-- 下拉操作菜单 -->
    <el-dropdown
      class="tags-action-dropdown"
      trigger="click"
      placement="bottom-end"
      @command="handleDropdownCommand"
    >
      <span class="tags-action-btn">
        <el-icon><arrow-down /></el-icon>
      </span>
      <template #dropdown>
        <el-dropdown-menu class="tags-dropdown-menu">
          <el-dropdown-item v-if="!isAffix(selectedDropdownTag)" command="close"
            ><close style="width: 1em; height: 1em" />关闭当前</el-dropdown-item
          >
          <el-dropdown-item command="closeOthers"
            ><circle-close
              style="width: 1em; height: 1em"
            />关闭其他</el-dropdown-item
          >
          <el-dropdown-item command="closeLeft" :disabled="isFirstView()"
            ><back style="width: 1em; height: 1em" />关闭左侧</el-dropdown-item
          >
          <el-dropdown-item command="closeRight" :disabled="isLastView()"
            ><right style="width: 1em; height: 1em" />关闭右侧</el-dropdown-item
          >
          <el-dropdown-item command="closeAll"
            ><circle-close
              style="width: 1em; height: 1em"
            />全部关闭</el-dropdown-item
          >
          <el-dropdown-item command="fullscreen" divided>
            <template v-if="!isFullscreen"
              ><full-screen style="width: 1em; height: 1em" />全屏显示</template
            >
            <template v-else
              ><close style="width: 1em; height: 1em" />退出全屏</template
            >
          </el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>

    <!-- 刷新按钮 -->
    <span
      class="tags-action-btn tags-refresh-btn"
      title="刷新页面"
      @click="refreshSelectedTag(selectedDropdownTag)"
    >
      <el-icon><refresh-right /></el-icon> 刷新
    </span>

    <!-- 右键上下文菜单 -->
    <ul
      v-show="visible"
      :style="{ left: left + 'px', top: top + 'px' }"
      class="contextmenu"
    >
      <li @click="refreshSelectedTag(selectedTag)">
        <refresh-right style="width: 1em; height: 1em" />刷新页面
      </li>
      <li v-if="!isAffix(selectedTag)" @click="closeSelectedTag(selectedTag)">
        <close style="width: 1em; height: 1em" />关闭当前
      </li>
      <li @click="closeOthersTags">
        <circle-close style="width: 1em; height: 1em" />关闭其他
      </li>
      <li v-if="!isFirstView()" @click="closeLeftTags">
        <back style="width: 1em; height: 1em" />关闭左侧
      </li>
      <li v-if="!isLastView()" @click="closeRightTags">
        <right style="width: 1em; height: 1em" />关闭右侧
      </li>
      <li @click="closeAllTags(selectedTag)">
        <circle-close style="width: 1em; height: 1em" />全部关闭
      </li>
    </ul>
  </div>
</template>

<script setup src="./index.js"></script>

<style lang="scss" scoped src="./index.scss"></style>
