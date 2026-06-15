defineProps({
  isActive: {
    type: Boolean,
    default: false,
  },
});

const emit = defineEmits();
const toggleClick = () => {
  emit("toggleClick");
};
