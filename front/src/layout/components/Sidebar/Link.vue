<template>
  <component :is="type" v-bind="linkProps()">
    <slot />
  </component>
</template>

<script setup src="./Link.js"></script>
